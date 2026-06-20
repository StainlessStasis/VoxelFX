package io.github.stainlessstasis.voxelfx.demo;

import com.mojang.math.Transformation;
import io.github.stainlessstasis.voxelfx.animation.VfxAnimation;
import io.github.stainlessstasis.voxelfx.animation.VfxAnimationBuilder;
import io.github.stainlessstasis.voxelfx.easing.Easing;
import io.github.stainlessstasis.voxelfx.easing.Easings;
import io.github.stainlessstasis.voxelfx.entity.VfxEntity;
import io.github.stainlessstasis.voxelfx.task.CancellableRunnable;
import io.github.stainlessstasis.voxelfx.task.ClientTaskScheduler;
import io.github.stainlessstasis.voxelfx.util.VfxMathUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class VfxDemos {
    private static final Map<String, BiConsumer<ClientLevel, LocalPlayer>> DEMOS = new HashMap<>();
    static {
        DEMOS.put("keyframes_and_easings", VfxDemos::demoKeyframesAndEasings);
        DEMOS.put("blocks_and_items", VfxDemos::demoBlocksAndItems);
        DEMOS.put("playback_controls", VfxDemos::demoPlaybackControls);
        DEMOS.put("overlay", VfxDemos::demoOverlay);
        DEMOS.put("queue_and_inheritance", VfxDemos::demoQueueAndInheritance);
        DEMOS.put("loops_and_callbacks", VfxDemos::demoLoopsAndCallbacks);
        DEMOS.put("frame_modifiers", VfxDemos::demoFrameModifiers);
        DEMOS.put("entity_binding", VfxDemos::demoEntityBinding);
        DEMOS.put("shockwave", VfxDemos::demoShockwave);
        DEMOS.put("nova_bomb", NovaBombDemo::demoNovaBomb);
        DEMOS.put("performance_test", VfxDemos::demoPerformanceTest);
        DEMOS.put("vanilla_performance_test", VfxDemos::demoVanillaPerformanceTest);
    }

    public static Set<String> getDemoNames() {
        return DEMOS.keySet();
    }

    public static boolean playDemo(String name, ClientLevel level, LocalPlayer player) {
        BiConsumer<ClientLevel, LocalPlayer> demoMethod = DEMOS.get(name.toLowerCase());
        if (demoMethod != null) {
            demoMethod.accept(level, player);
            return true;
        }
        return false;
    }

    private static Vec3 getFrontPosition(Player player) {
        Vec3 look = player.getLookAngle();
        return player.position().add(look.scale(4)).add(0, player.getEyeHeight() - 1, 0);
    }

    public static void demoKeyframesAndEasings(ClientLevel level, LocalPlayer player) {
        Vec3 pos = getFrontPosition(player);
        float spacing = 3f;
        List<Easing> easings = List.of(Easings.LINEAR, Easings.EASE_OUT_BOUNCE, Easings.EASE_IN_OUT_ELASTIC, Easings.EASE_OUT_EXPO);
        BlockState[] blocks = { Blocks.RED_CONCRETE.defaultBlockState(), Blocks.YELLOW_CONCRETE.defaultBlockState(), Blocks.LIME_CONCRETE.defaultBlockState(), Blocks.CYAN_CONCRETE.defaultBlockState() };

        for (int i = 0; i < easings.size(); i++) {
            VfxEntity entity = VfxEntity.create(level, pos.add(i * spacing, 0, 0));

            var easing = easings.get(i);
            entity.playAnimation(VfxAnimationBuilder.create()
                    .blockState(blocks[i], builder -> {})
                    .scale(0.5f, builder -> {})
                    .translation(builder -> builder
                            .addKeyframe(0.5f, 0, 4, 0, easing)
                            .addKeyframe(1f, 0, 0, 0, easing))
                    .loopInfinite()
                    .build(80));
        }
    }

    public static void demoBlocksAndItems(ClientLevel level, LocalPlayer player) {
        Vec3 pos = getFrontPosition(player);
        VfxEntity entity = VfxEntity.create(level, pos);

        entity.playAnimation(VfxAnimationBuilder.create()
                .blockState(Blocks.GLASS.defaultBlockState(), builder -> {})
                .itemStack(new ItemStack(Items.NETHER_STAR), i -> {})
                .scale(1.5f, builder -> {})
                .rotation(0, 0, 0, builder -> builder
                        .addKeyframe(1f, 0, 360, 0, Easings.LINEAR))
                .loopInfinite()
                .build(60));
    }

    // STOP / PAUSE / RESUME / PLAYBACK SPEED
    public static void demoPlaybackControls(ClientLevel level, LocalPlayer player) {
        Vec3 pos = getFrontPosition(player);
        VfxEntity entity = VfxEntity.create(level, pos);
        entity.setInfinitePersist(true);


        entity.playAnimation(VfxAnimationBuilder.create()
                .blockState(Blocks.EMERALD_BLOCK.defaultBlockState(), builder -> {})
                .scale(1f, builder -> {})
                .translation(builder -> builder
                        .addKeyframe(1f, 0, 5, 0, Easings.LINEAR))
                .onKeyframeReached(0.35f, vfxEntity -> {
                    vfxEntity.setPlaySpeed(0.5f);
                    player.sendSystemMessage(Component.literal("Slowed to 0.5x"));
                })
                .onKeyframeReached(0.5f, vfxEntity -> {
                    vfxEntity.pauseAnimation();
                    player.sendSystemMessage(Component.literal("Paused"));
                    ClientTaskScheduler.INSTANCE.runTaskLater(20, new CancellableRunnable() {
                        @Override protected void execute() {
                            vfxEntity.resumeAnimation();
                            vfxEntity.setPlaySpeed(1.5f);
                            player.sendSystemMessage(Component.literal("Resumed at 1.5x"));
                        }
                    });
                })
                .onKeyframeReached(0.9f, vfxEntity -> {
                    vfxEntity.setPlaySpeed(-3f);
                    player.sendSystemMessage(Component.literal("Reversing!"));
                })
                .build(100));
    }

    public static void demoOverlay(ClientLevel level, LocalPlayer player) {
        Vec3 pos = getFrontPosition(player);
        VfxEntity entity = VfxEntity.create(level, pos);


        entity.playAnimation(VfxAnimationBuilder.create()
                .blockState(Blocks.WHITE_CONCRETE.defaultBlockState(), builder -> {})
                .scale(1f, builder -> {})
                .overlay(1f, 0f, 0f, 0f, builder -> builder
                        .addColorKeyframe(0.25f, new Vector3f(1f, 0f, 0f))
                        .addIntensityKeyframe(0.1f, 0f)
                        .addIntensityKeyframe(0.25f, 0.8f, Easings.EASE_OUT_QUAD)
                        .addColorKeyframe(0.5f, new Vector3f(0f, 1f, 0f))
                        .addColorKeyframe(0.75f, new Vector3f(0f, 0f, 1f))
                        .addColorKeyframe(0.9f, new Vector3f(1f, 0f, 0f))
                        .addIntensityKeyframe(0.9f, 0.8f)
                        .addIntensityKeyframe(1f, 0f, Easings.EASE_IN_QUAD))
                .loopInfinite()
                .build(100));
    }

    public static void demoQueueAndInheritance(ClientLevel level, LocalPlayer player) {
        Vec3 pos = getFrontPosition(player);
        VfxEntity entity = VfxEntity.create(level, pos);

        // stage 1: slam down and squish
        VfxAnimation drop = VfxAnimationBuilder.create()
                .blockState(Blocks.ANVIL.defaultBlockState(), builder -> {})
                .scale(1f, builder -> {})
                .translation(0, 5, 0, builder -> builder
                        .addKeyframe(1f, 0, 0, 0, Easings.EASE_IN_QUART))
                .onStart(vfxEntity -> level.playLocalSound(pos.x, pos.y, pos.z,
                        SoundEvents.ANVIL_FALL, SoundSource.AMBIENT, 1f, 0.8f, false))
                .build(20);

        // stage 2: squish on impact, inheriting where it landed
        VfxAnimation squish = VfxAnimationBuilder.create()
                .inheritBlockState()
                .inheritTranslation()
                .scale(1f, builder -> builder
                        .addKeyframe(0.3f, 2f, 0.2f, 2f, Easings.EASE_OUT_EXPO)
                        .addKeyframe(1f, 1f, 1f, 1f, Easings.EASE_OUT_BOUNCE))
                .onStart(vfxEntity -> level.playLocalSound(pos.x, pos.y, pos.z,
                        SoundEvents.ANVIL_LAND, SoundSource.AMBIENT, 1f, 1.2f, false))
                .build(30);

        // stage 3: explode outward, inheriting the scale
        VfxAnimation explode = VfxAnimationBuilder.create()
                .inheritBlockState()
                .inheritTranslation()
                .inheritScale()
                .scale(builder -> builder
                        .addKeyframe(1f, 4f, Easings.EASE_OUT_EXPO))
                .overlay(1f, 0.5f, 0f, 0f, builder -> builder
                        .addIntensityKeyframe(0.2f, 0.9f, Easings.EASE_OUT_QUAD)
                        .addIntensityKeyframe(1f, 0f, Easings.EASE_IN_QUAD))
                .onStart(vfxEntity -> level.playLocalSound(pos.x, pos.y, pos.z,
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.AMBIENT, 0.6f, 1.5f, false))
                .build(25);

        entity.playOrQueueAnimation(drop);
        entity.playOrQueueAnimation(squish);
        entity.playOrQueueAnimation(explode);
    }

    public static void demoLoopsAndCallbacks(ClientLevel level, LocalPlayer player) {
        Vec3 pos = getFrontPosition(player);
        VfxEntity entity = VfxEntity.create(level, pos);


        entity.playAnimation(VfxAnimationBuilder.create()
                .blockState(Blocks.BEACON.defaultBlockState(), builder -> {})
                .scale(1f, builder -> {})
                .onLoop(vfxEntity -> level.playLocalSound(
                        pos.x, pos.y, pos.z,
                        SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 0.4f, 1.2f, false))
                .onKeyframeReached(0.5f, vfxEntity -> level.playLocalSound(
                        pos.x, pos.y, pos.z,
                        SoundEvents.BEACON_POWER_SELECT, SoundSource.AMBIENT, 0.3f, 1.5f, false))
                .overlay(0.4f, 0.8f, 1f, 0f, builder -> builder
                        .addIntensityKeyframe(0.3f, 0.6f, Easings.EASE_OUT_QUAD)
                        .addIntensityKeyframe(0.5f, 0f, Easings.EASE_IN_QUAD)
                        .addIntensityKeyframe(1f, 0f))
                .scale(1f, builder -> builder
                        .addKeyframe(0.3f, 1.3f, Easings.EASE_OUT_QUAD)
                        .addKeyframe(0.5f, 1f, Easings.EASE_IN_QUAD))
                .loopInfinite()
                .build(40));
    }

    public static void demoFrameModifiers(ClientLevel level, LocalPlayer player) {
        Vec3 pos = getFrontPosition(player);
        VfxEntity anchor = VfxEntity.create(level, pos);
        anchor.setInfinitePersist(true);

        VfxEntity orbiter = VfxEntity.create(level, pos);
        orbiter.setInfinitePersist(true);

        float orbitRadius = 3f;
        orbiter.playAnimation(VfxAnimationBuilder.create()
                .blockState(Blocks.PURPUR_BLOCK.defaultBlockState(), builder -> {})
                .onFrameTranslation((translation, ctx) -> {
                    float angle = ctx.interpolatedTicks() * 0.1f;
                    translation.x += (float)(Math.cos(angle) * orbitRadius);
                    translation.z += (float)(Math.sin(angle) * orbitRadius);
                })
                .scale(0.5f, builder -> builder
                        .addKeyframe(0.5f, 1f, Easings.EASE_OUT_ELASTIC)
                        .addKeyframe(1f, 0.5f, Easings.EASE_IN_QUAD))
                .loopInfinite()
                .build(60));
    }

    public static void demoEntityBinding(ClientLevel level, LocalPlayer player) {
        Vec3 pos = getFrontPosition(player);
        Snowball snowball = new Snowball(EntityType.SNOWBALL, level);
        snowball.setPos(pos);
        Vec3 look = player.getLookAngle();
        snowball.setDeltaMovement(look.scale(1.5f));
        level.addEntity(snowball);

        VfxEntity trail = VfxEntity.createBoundTo(level, snowball);
        trail.setOnTick(vfxEntity -> {
            if (!level.getBlockState(snowball.blockPosition()).isAir()) {
                snowball.discard();
            }
        });
        trail.setOnBoundEntityRemoved(vfxEntity -> {
            vfxEntity.stopAnimations();
        });

        trail.playAnimation(VfxAnimationBuilder.create()
                .blockState(Blocks.PACKED_ICE.defaultBlockState(), builder -> {})
                .scale(0.75f, builder -> {})
                .rotation(0, 0, 0, builder -> builder
                        .addKeyframe(1f, 360, 360, 0, Easings.LINEAR))
                .onEnd(vfxEntity -> {
                    Vec3 impactPos = vfxEntity.position();
                    level.playLocalSound(impactPos.x, impactPos.y, impactPos.z,
                            SoundEvents.GLASS_BREAK, SoundSource.AMBIENT, 1.0f, 0.6f, false);

                    VfxEntity impact = VfxEntity.create(level, impactPos);
                    impact.setBrightnessOverride(Brightness.FULL_BRIGHT.pack());

                    var snapshot = vfxEntity.captureCurrentSnapshot();
                    impact.playAnimation(VfxAnimationBuilder.create()
                            .blockState(snapshot.blockState(), builder -> {})
                            .rotation(snapshot.rotation(), builder -> {})
                            .overlay(o -> o
                                    .addIntensityKeyframe(1.0f, 0.0f, Easings.EASE_OUT_QUAD))
                            .scale(s -> s
                                    .addKeyframe(1.0f, 2f, Easings.EASE_OUT_EXPO))
                            .build(12));
                })
                .loopInfinite()
                .build(40));
    }

    public static void demoShockwave(ClientLevel level, LocalPlayer player) {
        Vec3 center = player.getEyePosition().add(player.getLookAngle().normalize().scale(6f));

        int ringCount = 48;
        int baseDuration = 20;

        BlockState[][] colorSequences = {
                { Blocks.SHROOMLIGHT.defaultBlockState(), Blocks.ORANGE_CONCRETE.defaultBlockState(), Blocks.GRAY_STAINED_GLASS.defaultBlockState() },
                { Blocks.SHROOMLIGHT.defaultBlockState(), Blocks.ORANGE_STAINED_GLASS.defaultBlockState(), Blocks.WHITE_STAINED_GLASS.defaultBlockState() },
                { Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.RED_STAINED_GLASS.defaultBlockState(), Blocks.BLACK_STAINED_GLASS.defaultBlockState() },
        };

        float radius = 10f;
        VfxMathUtils.forEachPointOnRing(ringCount, 0f, dir -> {
            BlockState[] sequence = VfxMathUtils.randomOf(colorSequences);

            float startScale = VfxMathUtils.randomBetween(2f, 2.75f);
            float endScale = VfxMathUtils.randomBetween(0.5f, 0.75f);
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
                            .addKeyframe(1f, dir.x * radius, 0, dir.z * radius, Easings.EASE_OUT_EXPO))
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
                    .build(baseDuration + (int) VfxMathUtils.randomBetween(0f, 10f));

            entity.playAnimation(anim);
        });
    }

    public static void demoPerformanceTest(ClientLevel level, LocalPlayer player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        float radius = 20f;

        BlockState[] blocks = {
                Blocks.CYAN_STAINED_GLASS.defaultBlockState(),
                Blocks.DIAMOND_BLOCK.defaultBlockState(),
                Blocks.FURNACE.defaultBlockState(),
                Blocks.OAK_LOG.defaultBlockState(),
                Blocks.STONE.defaultBlockState(),
        };

        int count = 5000;
        for (int i = 0; i < count; i++) {
            double theta = Math.random() * 2 * Math.PI;
            double phi = Math.acos(2 * Math.random() - 1);
            double r = radius * Math.cbrt(Math.random());
            double x = px + r * Math.sin(phi) * Math.cos(theta);
            double y = py + r * Math.cos(phi);
            double z = pz + r * Math.sin(phi) * Math.sin(theta);

            VfxEntity entity = VfxEntity.create(level, new Vec3(x, y, z));


            int duration = 1800 + (int) VfxMathUtils.randomBetween(0f, 900f);
            VfxAnimation anim = VfxAnimationBuilder.create()
                    .blockState(blocks[i % blocks.length], b -> {})
                    .translation(0, 0, 0, t -> t
                            .addRandomKeyframe(1f, -2f, 2f, 0f, 4f, -2f, 2f, Easings.random(player.getRandom())))
                    .scale(0.5f, s -> s
                            .addRandomKeyframe(1f, 0.5f, 2f, 0.5f, 2f, 0.5f, 2f, Easings.random(player.getRandom())))
                    .rotation(0, 0, 0, rot -> rot
                            .addRandomKeyframe(1f, 0f, 720f, 0f, 720f, 0f, 720f, Easings.random(player.getRandom())))
                    .build(duration);
            entity.playAnimation(anim);
        }

        player.sendSystemMessage(Component.literal("Spawned " + count + " VFX entities"));
    }

    public static void demoVanillaPerformanceTest(ClientLevel level, LocalPlayer player) {
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        float radius = 20f;

        BlockState[] blocks = {
                Blocks.CYAN_STAINED_GLASS.defaultBlockState(),
                Blocks.DIAMOND_BLOCK.defaultBlockState(),
                Blocks.FURNACE.defaultBlockState(),
                Blocks.OAK_LOG.defaultBlockState(),
                Blocks.STONE.defaultBlockState(),
        };

        int count = 5000;
        for (int i = 0; i < count; i++) {
            double theta = Math.random() * 2 * Math.PI;
            double phi = Math.acos(2 * Math.random() - 1);
            double r = radius * Math.cbrt(Math.random());
            double x = px + r * Math.sin(phi) * Math.cos(theta);
            double y = py + r * Math.cos(phi);
            double z = pz + r * Math.sin(phi) * Math.sin(theta);

            Display.BlockDisplay entity = new Display.BlockDisplay(EntityType.BLOCK_DISPLAY, level);
            entity.setPos(x, y, z);
            entity.setBlockState(blocks[i % blocks.length]);

            int duration = 1800 + (int) VfxMathUtils.randomBetween(0f, 900f);

            Vector3f endTranslation = new Vector3f(
                    VfxMathUtils.randomBetween(-2f, 2f),
                    VfxMathUtils.randomBetween(0f, 4f),
                    VfxMathUtils.randomBetween(-2f, 2f)
            );
            Vector3f endScale = new Vector3f(
                    VfxMathUtils.randomBetween(0.5f, 2f),
                    VfxMathUtils.randomBetween(0.5f, 2f),
                    VfxMathUtils.randomBetween(0.5f, 2f)
            );
            Quaternionf endRotation = new Quaternionf().rotationXYZ(
                    (float) Math.toRadians(VfxMathUtils.randomBetween(0f, 720f)),
                    (float) Math.toRadians(VfxMathUtils.randomBetween(0f, 720f)),
                    (float) Math.toRadians(VfxMathUtils.randomBetween(0f, 720f))
            );

            entity.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new Quaternionf(),
                    new Vector3f(0.5f, 0.5f, 0.5f),
                    null
            ));

            entity.setTransformationInterpolationDuration(duration);
            entity.setTransformationInterpolationDelay(0);

            entity.setTransformation(new Transformation(
                    endTranslation,
                    endRotation,
                    endScale,
                    null
            ));

        }

        player.sendSystemMessage(Component.literal("Spawned " + count + " vanilla Block Display entities"));
    }
}