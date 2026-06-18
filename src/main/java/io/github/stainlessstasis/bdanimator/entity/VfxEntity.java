package io.github.stainlessstasis.bdanimator.entity;

import io.github.stainlessstasis.bdanimator.animation.BillboardMode;
import io.github.stainlessstasis.bdanimator.animation.VfxAnimation;
import io.github.stainlessstasis.bdanimator.animation.VfxSnapshot;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

@SuppressWarnings("NullableProblems")
public class VfxEntity extends Entity {
    private VfxSnapshot lastSnapshot = VfxSnapshot.DEFAULT;
    private final Deque<VfxAnimation> animationQueue = new ArrayDeque<>();
    private @Nullable VfxAnimation currentAnimation;
    private long animationStartTick;
    private int animationDurationTicks;
    private float lastProgress = 0f;
    private @Nullable Consumer<VfxEntity> onTick;
    private @Nullable Consumer<VfxEntity> onRemoval;
    private int nextKeyframeCallbackIndex = 0;

    private int brightnessOverride = -1;
    private int ticksToPersist = 0;
    private int despawnTimer = 0;
    private boolean isPersistInfinite = false;
    private int loopsCompleted = 0;

    private float cullingRadius = 32f;
    private boolean isAffectedByCulling = true;

    private BillboardMode billboardMode = BillboardMode.FIXED;

    public VfxEntity(EntityType<? extends Entity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static VfxEntity createDefault(EntityType<? extends Entity> type, Level level) {
        return new VfxEntity(type, level);
    }

    /**
     * Immediately plays an animation, overriding whichever one is currently playing.
     * See {@link #playOrQueueAnimation} for queueing animations.
     */
    public void playAnimation(VfxAnimation animation) {
        playAnimation(animation, 0f);
    }

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

    public void playAnimationWithOffset(VfxAnimation animation, float seconds) {
        int durationTicks = animation.durationTicks();
        if (durationTicks <= 0) {
            playAnimation(animation, 0f);
            return;
        }
        float ticksOffset = seconds * 20f;
        playAnimation(animation, ticksOffset / (float) durationTicks);
    }

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

    public float getAnimationProgress(float partialTick) {
        if (animationDurationTicks <= 0) return 1f;
        float ticksSince = (float)(this.tickCount - this.animationStartTick);
        float t = Math.clamp(
                Mth.inverseLerp(ticksSince + partialTick, 0f, animationDurationTicks),
                0f, 1f
        );
        this.lastProgress = t;
        return t;
    }

    private VfxSnapshot captureEndSnapshot(VfxAnimation animation) {
        return new VfxSnapshot(
                animation.translationChannel().getLastKeyframeValue(),
                animation.scaleChannel().getLastKeyframeValue(),
                animation.rotationChannel().getLastKeyframeValue(),
                animation.overlayColorChannel().getLastKeyframeValue(),
                animation.overlayIntensityChannel().getLastKeyframeValue(),
                animation.blockStateChannel().getLastKeyframeValue()
        );
    }

    private VfxAnimation applySnapshot(VfxAnimation animation) {
        return new VfxAnimation(
                animation.inheritTranslation()
                        ? animation.translationChannel().withStartValue(lastSnapshot.translation())
                        : animation.translationChannel(),
                animation.inheritScale()
                        ? animation.scaleChannel().withStartValue(lastSnapshot.scale())
                        : animation.scaleChannel(),
                animation.inheritRotation()
                        ? animation.rotationChannel().withStartValue(lastSnapshot.rotation())
                        : animation.rotationChannel(),
                animation.inheritOverlayColor()
                        ? animation.overlayColorChannel().withStartValue(lastSnapshot.overlayColor())
                        : animation.overlayColorChannel(),
                animation.inheritOverlayIntensity()
                        ? animation.overlayIntensityChannel().withStartValue(lastSnapshot.overlayIntensity())
                        : animation.overlayIntensityChannel(),
                animation.inheritBlockState()
                        ? animation.blockStateChannel().withStartValue(lastSnapshot.blockState())
                        : animation.blockStateChannel(),
                animation.inheritTranslation(),
                animation.inheritScale(),
                animation.inheritRotation(),
                animation.inheritOverlayColor(),
                animation.inheritOverlayIntensity(),
                animation.inheritBlockState(),
                animation.translationModifier(),
                animation.scaleModifier(),
                animation.rotationModifier(),
                animation.overlayColorModifier(),
                animation.overlayIntensityModifier(),
                animation.rotationPivot(),
                animation.durationTicks(),
                animation.loopCount(),
                animation.onStart(),
                animation.onEnd(),
                animation.onLoop(),
                animation.keyframeCallbacks()
        );
    }

    private boolean hasAnyInheritance(VfxAnimation animation) {
        return animation.inheritTranslation() || animation.inheritScale() || animation.inheritRotation()
                || animation.inheritOverlayColor() || animation.inheritOverlayIntensity() || animation.inheritBlockState();
    }

    @Override
    public void tick() {
        super.tick();

        if (onTick != null) {
            onTick.accept(this);
        }

        if (currentAnimation != null) {
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

            if (tickCount - animationStartTick >= animationDurationTicks) {
                int loopCount = currentAnimation.loopCount();
                boolean isLastLoop = loopCount >= 0 && loopsCompleted >= loopCount;

                if (!isLastLoop) {
                    loopsCompleted++;
                    lastProgress = 0f;
                    nextKeyframeCallbackIndex = 0;
                    animationStartTick = tickCount;
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
                        if (hasAnyInheritance(next)) {
                            playAnimation(applySnapshot(next));
                        } else {
                            playAnimation(next);
                        }
                    } else {
                        tickDespawn();
                    }
                }
            }
        } else {
            tickDespawn();
        }
    }

    protected void tickDespawn() {
        if (isPersistInfinite) return;
        if (despawnTimer >= ticksToPersist) {
            discard();
        }
        despawnTimer++;
    }

    @Override
    public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);
        if (onRemoval != null) {
            onRemoval.accept(this);
        }
    }

    public @Nullable VfxAnimation getCurrentAnimation() { return currentAnimation; }
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

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float v) { return false; }
    @Override protected void readAdditionalSaveData(ValueInput input) { discard(); }
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
}
