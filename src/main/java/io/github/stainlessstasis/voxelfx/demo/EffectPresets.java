package io.github.stainlessstasis.voxelfx.demo;

import io.github.stainlessstasis.voxelfx.animation.VfxAnimation;
import io.github.stainlessstasis.voxelfx.animation.VfxAnimationBuilder;
import io.github.stainlessstasis.voxelfx.easing.Easings;
import io.github.stainlessstasis.voxelfx.entity.VfxEntity;
import io.github.stainlessstasis.voxelfx.util.VfxMathUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class EffectPresets {
    public static void shockwave(ClientLevel level, Vec3 center, ShockwaveConfig config) {
        VfxMathUtils.forEachPointOnRing(config.ringCount(), 0f, dir -> {
            BlockState[] sequence = VfxMathUtils.randomOf(config.palettes());
            if (sequence == null || sequence.length < 3) return;

            float startScale = VfxMathUtils.randomBetween(config.minScale(), config.maxScale());
            float endScale = VfxMathUtils.randomBetween(config.minScale() * 0.25f, config.maxScale() * 0.27f);

            float fireTransition = VfxMathUtils.randomBetween(0.25f, 0.4f);
            float smokeTransition = VfxMathUtils.randomBetween(0.5f, 0.7f);

            Vector3f startRotation = new Vector3f(
                    VfxMathUtils.randomBetween(0f, 360f),
                    VfxMathUtils.randomBetween(0f, 360f),
                    VfxMathUtils.randomBetween(0f, 360f)
            );

            VfxEntity entity = VfxEntity.create(level, center);

            VfxAnimation anim = VfxAnimationBuilder.create()
                    .blockState(sequence[0], b -> b
                            .addKeyframe(fireTransition, sequence[1])
                            .addKeyframe(smokeTransition, sequence[2]))
                    .translation(0, 0, 0, t -> t
                            .addKeyframe(1f, dir.x * config.maxRadius(), 0, dir.z * config.maxRadius(), Easings.EASE_OUT_EXPO))
                    .scale(startScale, s -> s
                            .addKeyframe(0.7f, startScale * 0.75f, Easings.EASE_IN_QUAD)
                            .addKeyframe(1f, endScale, Easings.EASE_IN_QUAD))
                    .rotation(startRotation, r -> r
                            .addRandomDeltaKeyframe(1f, -30f, 30f, Easings.EASE_OUT_QUAD))
                    .overlay(new Vector3f(1f, 0.5f, 0f), 0.8f, o -> o
                            .addColorKeyframe(fireTransition, new Vector3f(0.8f, 0.0f, 0f), Easings.EASE_IN_QUAD)
                            .addColorKeyframe(smokeTransition, new Vector3f(0.1f, 0.0f, 0.0f), Easings.EASE_IN_QUAD)
                            .addIntensityKeyframe(smokeTransition, 0.2f, Easings.EASE_IN_QUAD)
                            .addColorKeyframe(1f, new Vector3f(0.05f, 0.05f, 0.05f))
                            .addIntensityKeyframe(1f, 0f, Easings.EASE_IN_QUAD))
                    .build(config.baseDuration() + (int) VfxMathUtils.randomBetween(0f, 10f));

            entity.playAnimation(anim);
        });
    }
}
