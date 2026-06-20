package io.github.stainlessstasis.voxelfx.demo;

import io.github.stainlessstasis.voxelfx.animation.VfxAnimation;
import io.github.stainlessstasis.voxelfx.animation.VfxAnimationBuilder;
import io.github.stainlessstasis.voxelfx.animation.VfxSnapshot;
import io.github.stainlessstasis.voxelfx.easing.Easings;
import io.github.stainlessstasis.voxelfx.entity.VfxEntity;
import io.github.stainlessstasis.voxelfx.task.CancellableRunnable;
import io.github.stainlessstasis.voxelfx.task.ClientTaskScheduler;
import io.github.stainlessstasis.voxelfx.util.VfxMathUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.stainlessstasis.voxelfx.util.VfxMathUtils.randomBetween;
import static io.github.stainlessstasis.voxelfx.util.VfxMathUtils.randomOf;

public class NovaBombDemo {
    private static final long[] SONIC_CHARGE_SEEDS = new long[]{758150237086332386L, 1026614418597737318L};

    public static void demoNovaBomb(ClientLevel level, LocalPlayer player) {
        Vec3 spawnPos = player.getEyePosition().add(player.getLookAngle().scale(1.5f));
        Vec3 look = player.getLookAngle().normalize();

        Snowball projectile = new Snowball(EntityType.SNOWBALL, level);
        projectile.setPos(spawnPos);
        projectile.setDeltaMovement(look.scale(2f));

        AtomicBoolean hasExploded = new AtomicBoolean(false);
        Quaternionf coreRotation = new Quaternionf();

        VfxEntity core = VfxEntity.createBoundTo(level, projectile);
        // manually tick the snowball since clientside entities dont really work due to 26.2 changes. dont add it to the level
        core.setOnTick(vfxEntity -> {
            if (projectile.isAlive()) {
                projectile.tick();
            }
        });

        core.playAnimation(VfxAnimationBuilder.create()
                .blockState(Blocks.CRYING_OBSIDIAN.defaultBlockState(), b -> {})
                .onFrameRotation((r, ctx) -> {
                    r.set(coreRotation);
                    if (hasExploded.get()) {
                        return;
                    }
                    r.identity();
                    r.rotateY(ctx.interpolatedTicks() * 0.25f);
                    r.rotateX(ctx.interpolatedTicks() * 0.18f);
                    coreRotation.set(r);
                })
                .loopInfinite()
                .build(60));

        int orbitCount = 10;
        VfxEntity[] orbiters = new VfxEntity[orbitCount];
        BlockState[] orbitBlocks = {
                Blocks.AMETHYST_BLOCK.defaultBlockState(),
                Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                Blocks.PURPLE_STAINED_GLASS.defaultBlockState(),
                Blocks.BUDDING_AMETHYST.defaultBlockState(),
        };

        for (int i = 0; i < orbitCount; i++) {
            float angleOffset = (float)(i * Math.PI * 2f / orbitCount) + randomBetween(0f, 0.3f);
            float orbitRadius = randomBetween(1.5f, 2.8f);
            float orbitSpeed = randomBetween(0.12f, 0.25f);
            float orbitTilt = randomBetween(-0.6f, 0.6f);
            float scale = randomBetween(0.15f, 0.4f);
            float rotSpeed = randomBetween(0.2f, 0.6f);
            BlockState orbitBlock = randomOf(orbitBlocks);

            VfxEntity orbiter = VfxEntity.createBoundTo(level, projectile);
            orbiters[i] = orbiter;

            orbiter.playAnimation(VfxAnimationBuilder.create()
                    .blockState(orbitBlock, b -> {})
                    .scale(0f, s -> s
                            .addKeyframe(1f, scale, Easings.EASE_OUT_EXPO))
                    .onFrameTranslation((t, ctx) -> {
                        float angle = angleOffset + ctx.interpolatedTicks() * orbitSpeed;
                        t.x += (float)(Math.cos(angle) * orbitRadius);
                        t.z += (float)(Math.sin(angle) * orbitRadius);
                        t.y += (float)(Math.sin(angle + orbitTilt) * orbitRadius * 0.4f);
                    })
                    .onFrameRotation((r, ctx) -> r.rotateY(ctx.interpolatedTicks() * rotSpeed))
                    .overlay(0.3f, 0.0f, 1f, randomBetween(0.2f, 0.5f), o -> {})
                    .loopInfinite()
                    .build(20));
        }

        level.playLocalSound(spawnPos.x, spawnPos.y, spawnPos.z,
                SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.AMBIENT, 2f, 2f, false);

        VfxEntity poller = VfxEntity.create(level, spawnPos);
        poller.setInfinitePersist(true);
        poller.setOnTick(e -> {
            if (projectile.isRemoved() || !level.getBlockState(projectile.blockPosition()).isAir()) {
                hasExploded.set(true);

                detonateNovaBomb(level, projectile.position(), core, orbiters, player);
                if (!projectile.isRemoved()) {
                    projectile.discard();
                }
                e.discard();
            }
        });

        ClientTaskScheduler.INSTANCE.runTaskLater(60, new CancellableRunnable() {
            @Override protected void execute() {
                if (!projectile.isRemoved()) {
                    projectile.discard();
                }
            }
        });
    }

    private static void detonateNovaBomb(ClientLevel level, Vec3 impact, VfxEntity core, VfxEntity[] orbiters, LocalPlayer player) {
        level.playSeededSound(player, impact.x, impact.y, impact.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.AMBIENT, 4f, 2f, randomOf(SONIC_CHARGE_SEEDS[0], SONIC_CHARGE_SEEDS[1]));

        for (VfxEntity orbiter : orbiters) {
            VfxSnapshot snap = orbiter.captureCurrentSnapshot();
            orbiter.unbind();

            orbiter.playAnimation(VfxAnimationBuilder.create()
                    .inheritBlockState()
                    .inheritScale()
                    .inheritTranslation()
                    .translation(t -> t
                            .addKeyframe(1f, 0f, 0f, 0f, Easings.EASE_IN_EXPO))
                    .scale(s -> s
                            .addKeyframe(0.6f, snap.scale().x * 1.3f, Easings.EASE_OUT_QUAD)
                            .addKeyframe(1f, 0f, Easings.EASE_IN_EXPO))
                    .onFrameRotation((r, ctx) ->
                            r.rotateY(ctx.interpolatedTicks() * 0.6f * (1f - ctx.getAnimationProgress())))
                    .overlay(0.5f, 0f, 1f, 0.5f, o -> o
                            .addColorKeyframe(0.7f, new Vector3f(1f, 0.8f, 1f), Easings.EASE_IN_EXPO)
                            .addIntensityKeyframe(0.8f, 1f, Easings.EASE_IN_EXPO)
                            .addIntensityKeyframe(1f, 0f))
                    .build(15));
        }

        int suctionRingCount = 56;
        VfxMathUtils.forEachPointOnRing(suctionRingCount, 0.2f, dir -> {
            float distance = randomBetween(8f, 16f);
            float startX = dir.x * distance;
            float startZ = dir.z * distance;

            VfxEntity suction = VfxEntity.create(level,
                    impact.add(startX, randomBetween(-1f, 1f), startZ));

            float scale = randomBetween(0.2f, 0.9f);
            suction.playAnimation(VfxAnimationBuilder.create()
                    .blockState(randomOf(
                            Blocks.PURPLE_STAINED_GLASS.defaultBlockState(),
                            Blocks.AMETHYST_BLOCK.defaultBlockState(),
                            Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                            Blocks.BUDDING_AMETHYST.defaultBlockState(),
                            Blocks.BLACK_STAINED_GLASS.defaultBlockState()), b -> {})
                    .scale(0f, s -> s
                            .addKeyframe(0.15f, scale, Easings.EASE_OUT_EXPO)
                            .addKeyframe(0.85f, scale * 0.6f, Easings.EASE_IN_QUAD)
                            .addKeyframe(1f, 0f, Easings.EASE_IN_EXPO))
                    .translation(0, 0, 0, t -> t
                            .addKeyframe(1f, -startX, randomBetween(-0.5f, 0.5f), -startZ, Easings.EASE_IN_EXPO))
                    .onFrameRotation((r, ctx) ->
                            r.rotateY(ctx.interpolatedTicks() * randomBetween(0.2f, 0.5f)))
                    .overlay(0.4f, 0f, 1f, 0.6f, o -> o
                            .addIntensityKeyframe(0.8f, 0.6f)
                            .addIntensityKeyframe(1f, 0f, Easings.EASE_IN_QUAD))
                    .build((randomBetween(12, 18))));
        });

        core.unbind();
        core.setPos(impact);

        VfxAnimation coreSuck = VfxAnimationBuilder.create()
                .inheritBlockState()
                .scale(1.4f, s -> s
                        .addKeyframe(0.5f, 0.4f, Easings.EASE_IN_QUAD)
                        .addKeyframe(1f, 3.5f, Easings.EASE_OUT_EXPO))
                .overlay(0.1f, 0f, 0.5f, 0.7f, o -> o
                        .addColorKeyframe(0.5f, new Vector3f(0.8f, 0.4f, 1f))
                        .addIntensityKeyframe(0.5f, 0.3f, Easings.EASE_IN_QUAD)
                        .addIntensityKeyframe(1f, 1f, Easings.EASE_OUT_EXPO))
                .build(15);

        core.playOrQueueAnimation(coreSuck);

        ClientTaskScheduler.INSTANCE.runTaskLater(15, new CancellableRunnable() {
            @Override protected void execute() {
                level.playLocalSound(impact.x, impact.y, impact.z,
                        SoundEvents.WARDEN_SONIC_BOOM, SoundSource.AMBIENT, 6f, 0.7f, false);
                level.playLocalSound(impact.x, impact.y, impact.z,
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.AMBIENT, 6f, 1.5f, false);

                VfxAnimation coreBurst = VfxAnimationBuilder.create()
                        .inheritBlockState()
                        .scale(3.5f, s -> s
                                .addKeyframe(0.08f, 14f, Easings.EASE_OUT_EXPO)
                                .addKeyframe(1f, 0f, Easings.EASE_IN_QUART))
                        .overlay(1f, 0.5f, 1f, 0.5f, o -> o
                                .addColorKeyframe(0.05f, new Vector3f(1f, 1f, 1f))
                                .addIntensityKeyframe(0.08f, 1f, Easings.EASE_OUT_EXPO)
                                .addIntensityKeyframe(1f, 0f, Easings.EASE_IN_QUART))
                        .build(18);
                core.playAnimation(coreBurst);

                int burstCount = 110;
                VfxMathUtils.forEachPointOnSphere(burstCount, true, dir -> {
                    dir.add(
                            VfxMathUtils.randomBetween(-0.25f, 0.25f),
                            VfxMathUtils.randomBetween(-0.15f, 0.45f),
                            VfxMathUtils.randomBetween(-0.25f, 0.25f)
                    );

                    float speed = randomBetween(6f, 18f);
                    float startScale = randomBetween(0.25f, 1.4f);
                    int duration = (int) randomBetween(40f, 85f);

                    BlockState block = randomOf(
                            Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                            Blocks.AMETHYST_BLOCK.defaultBlockState(),
                            Blocks.PURPLE_STAINED_GLASS.defaultBlockState(),
                            Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                            Blocks.OBSIDIAN.defaultBlockState(),
                            Blocks.BUDDING_AMETHYST.defaultBlockState()
                    );

                    VfxEntity burst = VfxEntity.create(level, impact);

                    burst.playAnimation(VfxAnimationBuilder.create()
                            .blockState(block, b -> b
                                    .addKeyframe(randomBetween(0.3f, 0.6f),
                                            Blocks.BLACK_STAINED_GLASS.defaultBlockState()))
                            .translation(0, 0, 0, t -> t
                                    .addKeyframe(0.3f,
                                            dir.x * speed, dir.y * speed, dir.z * speed,
                                            Easings.EASE_OUT_EXPO)
                                    .addKeyframe(1f,
                                            dir.x * speed * 1.25f,
                                            dir.y * speed * 1.25f - randomBetween(1.5f, 5f),
                                            dir.z * speed * 1.25f,
                                            Easings.EASE_OUT_SINE))
                            .scale(startScale, s -> s
                                    .addKeyframe(0.15f, startScale * 1.4f, Easings.EASE_OUT_QUAD)
                                    .addKeyframe(0.55f, startScale * 0.8f)
                                    .addKeyframe(1f, 0f, Easings.EASE_IN_QUART))
                            .rotation(
                                    randomBetween(0f, 360f),
                                    randomBetween(0f, 360f),
                                    randomBetween(0f, 360f),
                                    r -> r.addRandomDeltaKeyframe(1f, -240f, 240f, Easings.EASE_OUT_QUAD))
                            .overlay(0.5f, 0.0f, 1f, 0.9f, o -> o
                                    .addColorKeyframe(0.25f, new Vector3f(0.4f, 0f, 0.6f))
                                    .addColorKeyframe(0.6f, new Vector3f(0.05f, 0f, 0.1f))
                                    .addIntensityKeyframe(0.35f, 0.6f, Easings.EASE_OUT_QUAD)
                                    .addIntensityKeyframe(1f, 0f, Easings.EASE_IN_SINE))
                            .build(duration));
                });
            }
        });

        int ringCount = 75;
        ClientTaskScheduler.INSTANCE.runTaskLater(18, new CancellableRunnable() {
            @Override protected void execute() {
                VfxMathUtils.forEachPointOnRing(ringCount, 0.05f, dir -> {
                    float startScale = randomBetween(1.2f, 2.8f);
                    float radius = randomBetween(16f, 24f);

                    BlockState block = randomOf(
                            Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                            Blocks.AMETHYST_BLOCK.defaultBlockState(),
                            Blocks.PURPLE_STAINED_GLASS.defaultBlockState(),
                            Blocks.BLACK_STAINED_GLASS.defaultBlockState()
                    );

                    VfxEntity fragment = VfxEntity.create(level, impact);

                    fragment.playAnimation(VfxAnimationBuilder.create()
                            .blockState(block, b -> b
                                    .addKeyframe(randomBetween(0.4f, 0.6f),
                                            Blocks.BLACK_STAINED_GLASS.defaultBlockState()))
                            .translation(0, 0, 0, t -> t
                                    .addKeyframe(1f, dir.x * radius, randomBetween(-0.5f, 1.5f), dir.z * radius, Easings.EASE_OUT_EXPO))
                            .scale(startScale, s -> s
                                    .addKeyframe(0.5f, startScale * 0.7f, Easings.EASE_IN_QUAD)
                                    .addKeyframe(1f, 0f, Easings.EASE_IN_QUART))
                            .rotation(
                                    randomBetween(0f, 360f),
                                    randomBetween(0f, 360f),
                                    randomBetween(0f, 360f),
                                    r -> r.addRandomDeltaKeyframe(1f, -90f, 90f, Easings.EASE_OUT_QUAD))
                            .overlay(0.5f, 0f, 1f, 0.8f, o -> o
                                    .addColorKeyframe(0.3f, new Vector3f(0.1f, 0f, 0.2f))
                                    .addIntensityKeyframe(0.5f, 0.4f, Easings.EASE_IN_QUAD)
                                    .addIntensityKeyframe(1f, 0f, Easings.EASE_IN_QUAD))
                            .build((int) randomBetween(22f, 34f)));
                });
            }
        });

        ClientTaskScheduler.INSTANCE.runTaskLater(20, new CancellableRunnable() {
            @Override protected void execute() {
                int seekerCount = 7;
                for (int s = 0; s < seekerCount; s++) {
                    final float seekerAngle = (float)(s * Math.PI * 2f / seekerCount);
                    final float seekerOrbitSpeed = 0.2f;
                    final float seekerOrbitRadius = 5f;

                    VfxEntity seeker = VfxEntity.create(level, impact);
                    seeker.setInfinitePersist(true);

                    VfxAnimation seekerFly = VfxAnimationBuilder.create()
                            .blockState(Blocks.AMETHYST_BLOCK.defaultBlockState(), b -> {})
                            .scale(0f, sc -> sc.addKeyframe(1f, 0.3f, Easings.EASE_OUT_EXPO))
                            .onFrameTranslation((t, ctx) -> {
                                float angle = seekerAngle + ctx.interpolatedTicks() * seekerOrbitSpeed;
                                float r = ctx.getAnimationProgress() * seekerOrbitRadius;
                                t.x += (float)(Math.cos(angle) * r);
                                t.z += (float)(Math.sin(angle) * r);
                                t.y += 0.5f + (float)(Math.sin(ctx.interpolatedTicks() * 0.2f) * 0.5f);
                            })
                            .onFrameRotation((r, ctx) -> {
                                r.rotateY(ctx.interpolatedTicks() * 0.5f);
                                r.rotateX(ctx.interpolatedTicks() * 0.3f);
                            })
                            .overlay(0.4f, 0f, 1f, 0f, o -> o
                                    .addIntensityKeyframe(1f, 0.7f, Easings.EASE_OUT_QUAD))
                            .build(20);

                    VfxAnimation seekerReturn = VfxAnimationBuilder.create()
                            .inheritBlockState()
                            .inheritScale()
                            .inheritTranslation()
                            .translation(t -> t
                                    .addKeyframe(1f, 0f, 0f, 0f, Easings.EASE_IN_EXPO))
                            .scale(sc -> sc
                                    .addKeyframe(0.7f, 0.4f, Easings.EASE_IN_QUAD)
                                    .addKeyframe(1f, 0f, Easings.EASE_IN_QUART))
                            .overlay(0.4f, 0f, 1f, 0.7f, o -> o
                                    .addColorKeyframe(0.7f, new Vector3f(1f, 0.5f, 1f), Easings.EASE_IN_QUAD)
                                    .addIntensityKeyframe(0.9f, 1f, Easings.EASE_IN_EXPO)
                                    .addIntensityKeyframe(1f, 0f))
                            .onEnd(e -> {
                                for (int m = 0; m < 8; m++) {
                                    double theta = Math.random() * 2 * Math.PI;
                                    float mX = (float) Math.cos(theta);
                                    float mZ = (float) Math.sin(theta);
                                    VfxEntity mini = VfxEntity.create(level, impact);
                                    mini.playAnimation(VfxAnimationBuilder.create()
                                            .blockState(Blocks.PURPLE_STAINED_GLASS.defaultBlockState(), b -> {})
                                            .scale(0.2f, sc -> sc
                                                    .addKeyframe(1f, 0f, Easings.EASE_OUT_EXPO))
                                            .translation(0, 0, 0, t -> t
                                                    .addKeyframe(1f, mX * 3f, randomBetween(0f, 2f), mZ * 3f,
                                                            Easings.EASE_OUT_EXPO))
                                            .overlay(0.8f, 0.2f, 1f, 0.8f, o -> o
                                                    .addIntensityKeyframe(1f, 0f, Easings.EASE_IN_QUAD))
                                            .build(12));
                                }
                            })
                            .build(20);

                    seeker.playOrQueueAnimation(seekerFly);
                    seeker.playOrQueueAnimation(seekerReturn);
                }
            }
        });
    }
}