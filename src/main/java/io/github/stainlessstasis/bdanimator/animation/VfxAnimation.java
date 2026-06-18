package io.github.stainlessstasis.bdanimator.animation;

import io.github.stainlessstasis.bdanimator.channel.BlockStateChannel;
import io.github.stainlessstasis.bdanimator.channel.KeyframedChannel;
import io.github.stainlessstasis.bdanimator.entity.VfxEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

public record VfxAnimation(
        KeyframedChannel<Vector3f, Vector3f> translationChannel,
        KeyframedChannel<Vector3f, Vector3f> scaleChannel,
        KeyframedChannel<Vector3f, Quaternionf> rotationChannel,
        KeyframedChannel<Vector3f, Vector3f> overlayColorChannel,
        KeyframedChannel<Float, float[]> overlayIntensityChannel,
        BlockStateChannel blockStateChannel,
        boolean inheritTranslation,
        boolean inheritScale,
        boolean inheritRotation,
        boolean inheritOverlayColor,
        boolean inheritOverlayIntensity,
        boolean inheritBlockState,
        @Nullable Vector3fTickModifier translationModifier,
        @Nullable Vector3fTickModifier scaleModifier,
        @Nullable QuaternionfTickModifier rotationModifier,
        @Nullable Vector3fTickModifier overlayColorModifier,
        @Nullable FloatTickModifier overlayIntensityModifier,
        Vector3f rotationPivot,
        int durationTicks,
        int loopCount,
        @Nullable Consumer<VfxEntity> onStart,
        @Nullable Consumer<VfxEntity> onEnd,
        @Nullable Consumer<VfxEntity> onLoop,
        List<KeyframeCallbackEntry> keyframeCallbacks
) {
    @FunctionalInterface
    public interface QuaternionfTickModifier {
        void apply(Quaternionf value, AnimationContext context);
    }

    @FunctionalInterface
    public interface Vector3fTickModifier {
        void apply(Vector3f value, AnimationContext context);
    }

    @FunctionalInterface
    public interface FloatTickModifier {
        void apply(float[] value, AnimationContext context);
    }

    public record AnimationContext(VfxEntity entity, float interpolatedTicks, float partialTick) {
        public float getNormalizedAnimationProgress() {
            return entity.getAnimationProgress(partialTick);
        }
    }


}
