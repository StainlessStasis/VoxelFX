package io.github.stainlessstasis.bdanimator.entity;

import io.github.stainlessstasis.bdanimator.animation.VfxAnimation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@SuppressWarnings("NullableProblems")
public class VfxEntity extends Entity {
    private @Nullable VfxAnimation currentAnimation;
    private long animationStartTick;
    private int animationDurationTicks;
    private float lastProgress = 0f;
    private @Nullable Consumer<VfxEntity> onTick;
    private @Nullable Consumer<VfxEntity> onRemoval;

    private int brightnessOverride = -1;
    private int ticksToPersist = 0;
    private int despawnTimer = 0;
    private boolean isPersistInfinite = false;
    private int loopsCompleted = 0;

    public VfxEntity(EntityType<? extends Entity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static VfxEntity createDefault(EntityType<? extends Entity> type, Level level) {
        return new VfxEntity(type, level);
    }

    public void playAnimation(VfxAnimation animation) {
        this.currentAnimation = animation;
        this.animationStartTick = this.tickCount;
        this.animationDurationTicks = animation.durationTicks();
        this.loopsCompleted = 0;
        this.lastProgress = 0f;
        if (animation.onStart() != null) {
            animation.onStart().accept(this);
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

    @Override
    public void tick() {
        super.tick();

        if (onTick != null) {
            onTick.accept(this);
        }

        if (currentAnimation != null) {
            if (currentAnimation.keyframeCallbacks() != null) {
                float previousProgress = this.lastProgress;
                float progress = getAnimationProgress(0f);
                currentAnimation.keyframeCallbacks().forEach((time, callback) -> {
                    if (previousProgress < time && progress >= time) {
                        callback.accept(this);
                    }
                });
            }

            if (tickCount - animationStartTick >= animationDurationTicks) {
                int loopCount = currentAnimation.loopCount();
                boolean isLastLoop = loopCount >= 0 && loopsCompleted >= loopCount;

                if (!isLastLoop) {
                    loopsCompleted++;
                    lastProgress = 0f;
                    animationStartTick = tickCount;
                    if (currentAnimation.onLoop() != null) {
                        currentAnimation.onLoop().accept(this);
                    }
                } else {
                    if (currentAnimation.onEnd() != null) {
                        currentAnimation.onEnd().accept(this);
                    }
                    currentAnimation = null;
                    loopsCompleted = 0;
                    tickDespawn();
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

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float v) { return false; }
    @Override protected void readAdditionalSaveData(ValueInput input) { discard(); }
    @Override protected void addAdditionalSaveData(ValueOutput output) {}
}
