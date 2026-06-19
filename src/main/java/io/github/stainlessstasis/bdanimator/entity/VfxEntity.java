package io.github.stainlessstasis.bdanimator.entity;

import io.github.stainlessstasis.bdanimator.animation.BillboardMode;
import io.github.stainlessstasis.bdanimator.animation.VfxAnimation;
import io.github.stainlessstasis.bdanimator.animation.VfxSnapshot;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

@SuppressWarnings("NullableProblems")
public class VfxEntity extends Entity {
    private VfxSnapshot lastSnapshot = VfxSnapshot.DEFAULT;
    private final Vector3f lastRenderedTranslation = new Vector3f();
    private final Vector3f lastRenderedScale = new Vector3f(1f);
    private final Vector3f lastRenderedOverlayColor = new Vector3f(1f);
    private final float[] lastRenderedOverlayIntensity = new float[]{0f};
    private BlockState lastRenderedBlockState = Blocks.AIR.defaultBlockState();
    private final BlockModelRenderState blockModel = new BlockModelRenderState();
    private ItemStack lastRenderedItemStack = ItemStack.EMPTY;
    private final ItemStackRenderState itemModel = new ItemStackRenderState();

    private final Deque<VfxAnimation> animationQueue = new ArrayDeque<>();
    private @Nullable VfxAnimation currentAnimation;
    private long animationStartTick;
    private int animationDurationTicks;
    private float lastProgress = 0f;
    private boolean isPaused = false;
    private long pausedAtTick = 0;
    private float pausedProgress = 0f;
    private float playSpeed = 1f;
    private boolean reverseStopsAtStart = true;
    private @Nullable Consumer<VfxEntity> onTick;
    private @Nullable Consumer<VfxEntity> onRemoval;
    private int nextKeyframeCallbackIndex = 0;

    private int brightnessOverride = -1;
    private int ticksToPersist = 0;
    private int despawnTimer = 0;
    private boolean isPersistInfinite = false;
    private int loopsCompleted = 0;

    private float cullingRadius = 32f;
    private boolean isAffectedByCulling = false;

    private @Nullable Entity boundEntity;
    private Vector3f bindOffset = new Vector3f();
    private boolean bindLocalSpace = false;

    private BillboardMode billboardMode = BillboardMode.FIXED;

    protected VfxEntity(EntityType<? extends Entity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static VfxEntity create(Level level, Vec3 pos) {
        VfxEntity entity = new VfxEntity(BDAnimatorEntities.VFX_ENTITY.get(), level);
        entity.setPos(pos);
        return entity;
    }

    public static VfxEntity createBoundTo(Level level, Entity target) {
        return createBoundTo(level, target, new Vector3f(), false);
    }

    public static VfxEntity createBoundTo(Level level, Entity target, Vector3f offset, boolean localSpace) {
        VfxEntity entity = create(level, target.position());
        entity.bindTo(target, offset, localSpace);
        entity.setPos(target.position());
        return entity;
    }

    /**
     * Immediately plays an animation, overriding whichever one is currently playing.
     * See {@link #playOrQueueAnimation} for queueing animations.
     */
    public void playAnimation(VfxAnimation animation) {
        playAnimation(animation, 0f);
    }

    /**
     * Immediately plays an animation with an offset in its progress (0-1).
     * See {@link #playOrQueueAnimation} for queueing animations.
     */
    public void playAnimation(VfxAnimation animation, float progressOffset) {
        this.currentAnimation = animation;
        this.animationDurationTicks = animation.durationTicks();
        this.loopsCompleted = 0;
        this.nextKeyframeCallbackIndex = 0;

        float progress = Math.clamp(progressOffset, 0f, 1f);
        int tickOffset = (int) (this.animationDurationTicks * progress);
        this.animationStartTick = this.tickCount - tickOffset;
        this.lastProgress = progress;

        if (animation.onStart() != null) {
            animation.onStart().accept(this);
        }
    }

    /**
     * See {@link #playAnimation(VfxAnimation, float)}.
     */
    public void playAnimationWithOffset(VfxAnimation animation, float seconds) {
        int durationTicks = animation.durationTicks();
        if (durationTicks <= 0) {
            playAnimation(animation, 0f);
            return;
        }
        float ticksOffset = seconds * 20f;
        playAnimation(animation, ticksOffset / (float) durationTicks);
    }

    /**
     * See {@link #playAnimation(VfxAnimation, float)}.
     */
    public void playAnimationWithOffset(VfxAnimation animation, int ticks) {
        int durationTicks = animation.durationTicks();
        if (durationTicks <= 0) {
            playAnimation(animation, 0f);
            return;
        }
        playAnimation(animation, (float) ticks / (float) durationTicks);
    }

    /**
     * If no animation is currently playing, the animation will be played immediately.
     * Otherwise, it will be added to the animation queue.
     */
    public void playOrQueueAnimation(VfxAnimation animation) {
        if (currentAnimation == null) {
            playAnimation(animation);
        } else {
            animationQueue.add(animation);
        }
    }

    /**
     * Stops this entity's animations and clears the animation queue.
     */
    public void stopAnimations() {
        if (currentAnimation == null) return;
        if (currentAnimation.onEnd() != null) {
            currentAnimation.onEnd().accept(this);
        }
        lastSnapshot = captureEndSnapshot(currentAnimation);
        currentAnimation = null;
        loopsCompleted = 0;
        isPaused = false;
        animationQueue.clear();
        tickDespawn();
    }

    /**
     * Pauses this entity's animations, if not already paused. For resuming, see {@link #resumeAnimation}
     */
    public void pauseAnimation() {
        if (isPaused || currentAnimation == null) return;
        isPaused = true;
        pausedAtTick = tickCount;
        pausedProgress = lastProgress;
    }

    /**
     * Resumes this entity's animations. For pausing, see {@link #pauseAnimation}.
     */
    public void resumeAnimation() {
        if (!isPaused || currentAnimation == null) return;
        long pausedDuration = tickCount - pausedAtTick;
        animationStartTick += pausedDuration;
        isPaused = false;
    }
    public boolean isPaused() { return isPaused; }

    /**
     * Sets the play speed of this entity's animations. Negative values will play in reverse.
     */
    public void setPlaySpeed(float speed) {
        if (speed == this.playSpeed) return;
        float currentProgress = getAnimationProgress(0f);
        this.playSpeed = speed;
        if (speed != 0f) {
            float scaledTicks = currentProgress * animationDurationTicks;
            float ticksSince = scaledTicks / speed;
            this.animationStartTick = (long)(this.tickCount - ticksSince);
        }

        this.lastProgress = currentProgress;
    }
    public float getPlaySpeed() { return playSpeed; }
    public void setReverseStopsAtStart(boolean value) { this.reverseStopsAtStart = value; }

    /**
     * Gets the normalized animation progress, based on the play speed of the animation. Goes from 1 to 0 when reversed. Always returns the same value when paused.
     */
    public float getAnimationProgress(float partialTick) {
        if (isPaused) return pausedProgress;
        if (animationDurationTicks <= 0) return 1f;
        float ticksSince = (float)(this.tickCount - this.animationStartTick);
        float scaled = ticksSince * playSpeed;
        float t = Math.clamp(
                Mth.inverseLerp(scaled + partialTick * playSpeed, 0f, animationDurationTicks),
                0f, 1f
        );
        this.lastProgress = t;
        return t;
    }

    public void clearAnimationQueue() {
        animationQueue.clear();
    }

    private VfxSnapshot captureEndSnapshot(VfxAnimation animation) {
        var prev = this.lastSnapshot;
//        System.out.println("CAPTURE END SNAPSHOT");
//        System.out.println("PREVIOUS: "+prev);
//        System.out.println("Translation: "+ lastRenderedTranslation);
//        System.out.println("Scale: "+ lastRenderedScale);
//        System.out.println("Rotation: "+ animation.rotationChannel().resolveValueAt(
//                1f, animation.inheritRotation() ? prev.rotation() : VfxSnapshot.DEFAULT.rotation()
//        ));
//        System.out.println("Color: "+ lastRenderedOverlayColor);
//        System.out.println("Intensity: "+ lastRenderedOverlayIntensity[0]);
//        System.out.println(lastRenderedBlockState);
//        System.out.println(lastRenderedItemStack);
        return new VfxSnapshot(
                lastRenderedTranslation,
                lastRenderedScale,
                animation.rotationChannel().resolveValueAt(
                        1f, animation.inheritRotation() ? prev.rotation() : VfxSnapshot.DEFAULT.rotation()
                ),
                lastRenderedOverlayColor,
                lastRenderedOverlayIntensity[0],
                lastRenderedBlockState,
                lastRenderedItemStack
        );
    }

    protected void updateRenderedTranslation(Vector3f value) { this.lastRenderedTranslation.set(value); }
    protected void updateRenderedScale(Vector3f value) { this.lastRenderedScale.set(value); }
    protected void updateRenderedOverlayColor(Vector3f value) { this.lastRenderedOverlayColor.set(value); }
    protected void updateRenderedOverlayIntensity(float value) { this.lastRenderedOverlayIntensity[0] = value; }
    protected void updateBlockModel(BlockState currentState, BlockModelResolver resolver) {
        if (this.lastRenderedBlockState != currentState) {
            this.lastRenderedBlockState = currentState;
            resolver.update(this.blockModel, currentState, DisplayRenderer.BLOCK_DISPLAY_CONTEXT);
        }
    }
    public BlockModelRenderState getBlockModel() {
        return this.blockModel;
    }
    protected void updateItemModel(ItemStack currentStack, ItemModelResolver resolver) {
        if (this.lastRenderedItemStack == null || !ItemStack.isSameItemSameComponents(this.lastRenderedItemStack, currentStack)) {
            this.lastRenderedItemStack = currentStack.copy();
            resolver.updateForNonLiving(this.itemModel, currentStack, ItemDisplayContext.GROUND, this);
        }
    }
    public ItemStackRenderState getItemModel() {
        return this.itemModel;
    }

    @Override
    public void tick() {
        super.tick();
        updateBoundPosition();

        if (onTick != null) {
            onTick.accept(this);
        }

        if (currentAnimation == null) {
            tickDespawn();
        } else {
            if (!isPaused) {
                tickAnimations();
            }
        }
    }

    protected void tickAnimations() {
        if (currentAnimation == null) return;
        var keyframeCallbacks = currentAnimation.keyframeCallbacks();
        if (keyframeCallbacks != null && !keyframeCallbacks.isEmpty()) {
            float progress = getAnimationProgress(0f);

            while (nextKeyframeCallbackIndex < keyframeCallbacks.size()) {
                var entry = keyframeCallbacks.get(nextKeyframeCallbackIndex);
                if (progress >= entry.time()) {
                    entry.callback().accept(this);
                    nextKeyframeCallbackIndex++;
                } else {
                    break;
                }
            }
        }

        boolean forwardComplete = playSpeed >= 0 && lastProgress >= 1f;
        boolean reverseComplete = playSpeed < 0 && lastProgress <= 0f;
        if (reverseComplete && reverseStopsAtStart) {
            if (currentAnimation.onEnd() != null) currentAnimation.onEnd().accept(this);
            lastSnapshot = captureEndSnapshot(currentAnimation);
            currentAnimation = null;
            loopsCompleted = 0;
            tickDespawn();
            return;
        }
        if (forwardComplete || reverseComplete) {
            int loopCount = currentAnimation.loopCount();
            boolean isLastLoop = loopCount >= 0 && loopsCompleted >= loopCount;

            if (!isLastLoop) {
                loopsCompleted++;
                resetLoop(playSpeed >= 0 ? 0f : 1f);
                if (currentAnimation.onLoop() != null) {
                    currentAnimation.onLoop().accept(this);
                }
            } else {
                if (currentAnimation.onEnd() != null) {
                    currentAnimation.onEnd().accept(this);
                }
                lastSnapshot = captureEndSnapshot(currentAnimation);
                currentAnimation = null;
                loopsCompleted = 0;

                VfxAnimation next = animationQueue.poll();
                if (next != null) {
                    if (next.hasAnyInheritance()) {
                        playAnimation(next);
                    } else {
                        playAnimation(next);
                    }
                } else {
                    tickDespawn();
                }
            }
        }
    }

    protected void tickDespawn() {
        if (isPersistInfinite) return;
        if (despawnTimer >= ticksToPersist) {
            discard();
        }
        despawnTimer++;
    }

    private void resetLoop(float startProgress) {
        this.lastProgress = startProgress;
        if (currentAnimation != null) {
            this.nextKeyframeCallbackIndex = playSpeed >= 0 ? 0 : currentAnimation.keyframeCallbacks().size();
        }
        float scaledTicks = startProgress * animationDurationTicks;
        if (playSpeed != 0f) {
            float ticksSince = scaledTicks / playSpeed;
            this.animationStartTick = (long)(this.tickCount - ticksSince) - 1;
        }
    }

    public void bindTo(Entity entity) {
        bindTo(entity, new Vector3f(), false);
    }

    public void bindTo(Entity entity, Vector3f offset) {
        bindTo(entity, offset, false);
    }

    public void bindTo(Entity entity, Vector3f offset, boolean localSpace) {
        this.boundEntity = entity;
        this.bindOffset = offset;
        this.bindLocalSpace = localSpace;
    }

    public void unbind() {
        this.boundEntity = null;
    }

    public @Nullable Entity getBoundEntity() {
        return boundEntity;
    }

    private void updateBoundPosition() {
        if (boundEntity == null || !boundEntity.isAlive()) {
            if (boundEntity != null) boundEntity = null;
            return;
        }

        Vec3 pos = boundEntity.position();
        if (bindLocalSpace) {
            Vec3 rotatedOffset = new Vec3(bindOffset.x, bindOffset.y, bindOffset.z)
                    .xRot((float) Math.toRadians(-boundEntity.getXRot()))
                    .yRot((float) Math.toRadians(-boundEntity.getYRot()));
            setPos(pos.add(rotatedOffset));
        } else {
            setPos(pos.x + bindOffset.x, pos.y + bindOffset.y, pos.z + bindOffset.z);
        }
    }

    @Override
    public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);
        if (onRemoval != null) {
            onRemoval.accept(this);
        }
    }

    public @Nullable VfxAnimation getCurrentAnimation() { return currentAnimation; }
    public int getLoopsCompleted() { return this.loopsCompleted; }
    public float getLastProgress() { return this.lastProgress; }
    public float getPausedProgress() { return this.pausedProgress; }

    public int getBrightnessOverride() { return brightnessOverride; }
    public void setBrightnessOverride(int brightness) { this.brightnessOverride = brightness; }

    public void setOnTick(Consumer<VfxEntity> onTick) { this.onTick = onTick; }
    public void setOnRemoval(Consumer<VfxEntity> onRemoval) { this.onRemoval = onRemoval; }

    public int getTicksToPersist() { return this.ticksToPersist; }
    public void setTicksToPersist(int ticks) { this.ticksToPersist = ticks; }
    public boolean isPersistInfinite() { return this.isPersistInfinite; }
    public void setInfinitePersist(boolean value) { this.isPersistInfinite = value; }

    public float getCullingRadius() { return cullingRadius; }
    public void setCullingRadius(float radius) { this.cullingRadius = radius; }
    public boolean isAffectedByCulling() { return isAffectedByCulling; }
    public void setAffectedByCulling(boolean affectedByCulling) { this.isAffectedByCulling = affectedByCulling; }

    public BillboardMode getBillboardMode() { return billboardMode; }
    public void setBillboardMode(BillboardMode mode) { this.billboardMode = mode; }

    public VfxSnapshot getLastSnapshot() { return this.lastSnapshot; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float v) { return false; }
    @Override protected void readAdditionalSaveData(ValueInput input) { discard(); }
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
}
