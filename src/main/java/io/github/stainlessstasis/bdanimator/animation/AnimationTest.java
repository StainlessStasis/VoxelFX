package io.github.stainlessstasis.bdanimator.animation;

import com.mojang.math.Transformation;
import io.github.stainlessstasis.bdanimator.easing.Easing;
import io.github.stainlessstasis.bdanimator.easing.Easings;
import io.github.stainlessstasis.bdanimator.entity.BDAnimatorEntities;
import io.github.stainlessstasis.bdanimator.entity.VfxEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class AnimationTest {
    public static void runShockwaveTest() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Vec3 center = player.getEyePosition().add(player.getLookAngle().normalize().scale(6f));

        int ringCount = 48;
        int duration = 20;

        BlockState[][] colorSequences = {
                { Blocks.SHROOMLIGHT.defaultBlockState(), Blocks.ORANGE_CONCRETE.defaultBlockState(), Blocks.GRAY_STAINED_GLASS.defaultBlockState() },
                { Blocks.SHROOMLIGHT.defaultBlockState(), Blocks.ORANGE_STAINED_GLASS.defaultBlockState(), Blocks.WHITE_STAINED_GLASS.defaultBlockState() },
                { Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.RED_STAINED_GLASS.defaultBlockState(), Blocks.BLACK_STAINED_GLASS.defaultBlockState() },
        };

        for (int i = 0; i < ringCount; i++) {
            double angle = (2 * Math.PI / ringCount) * i;
            float dirX = (float) Math.cos(angle);
            float dirZ = (float) Math.sin(angle);

            float randomYaw = (float)(Math.random() * 360);
            float randomPitch = (float)(Math.random() * 360);
            float randomRoll = (float)(Math.random() * 360);
            float randomEndYaw = randomYaw + (float)(Math.random() * 60 - 30);
            float randomEndPitch = randomPitch + (float)(Math.random() * 60 - 30);
            float randomEndRoll = randomRoll + (float)(Math.random() * 60 - 30);

            BlockState[] sequence = colorSequences[(int)(Math.random() * colorSequences.length)];

            float radius = 10f;
            float startScale = 2f + (float)(Math.random() * 0.75f);
            float endScale = 0.5f + (float)(Math.random() * 0.25f);
            float fireTransition = 0.25f + (float)(Math.random() * 0.15f);
            float smokeTransition = 0.5f + (float)(Math.random() * 0.2f);

            VfxEntity entity = VfxEntity.create(level, center);
            level.addEntity(entity);

            VfxAnimation anim = VfxAnimationBuilder.create()
                    .blockState(sequence[0], b -> b
                            .addKeyframe(fireTransition, sequence[1])
                            .addKeyframe(smokeTransition, sequence[2]))
                    .translation(0, 0, 0, t -> t
                            .addKeyframe(1f, dirX * radius, 0, dirZ * radius, Easings.EASE_OUT_EXPO))
                    .scale(startScale, s -> s
                            .addKeyframe(0.7f, startScale * 0.75f, Easings.EASE_IN_QUAD)
                            .addKeyframe(1f, endScale, Easings.EASE_IN_QUAD))
                    .rotation(randomPitch, randomYaw, randomRoll, r -> r
                            .addKeyframe(1f, randomEndPitch, randomEndYaw, randomEndRoll, Easings.EASE_OUT_QUAD))
                    .overlay(new Vector3f(1f, 0.5f, 0f), 0.8f, o -> o
                            .addColorKeyframe(fireTransition, new Vector3f(0.8f, 0.0f, 0f), Easings.EASE_IN_QUAD)
                            .addColorKeyframe(smokeTransition, new Vector3f(0.1f, 0.0f, 0.0f), Easings.EASE_IN_QUAD)
                            .addIntensityKeyframe(smokeTransition, 0.2f, Easings.EASE_IN_QUAD)
                            .addColorKeyframe(1f, new Vector3f(0.05f, 0.05f, 0.05f))
                            .addIntensityKeyframe(1f, 0f, Easings.EASE_IN_QUAD))
                    .build((int)(duration + Math.random() * 10));

            entity.playAnimation(anim);
        }
    }

    public static void runQueueTest() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Vec3 pos = player.getEyePosition().add(player.getLookAngle().normalize().scale(4f));
        VfxEntity entity = VfxEntity.create(level, pos);
        entity.setAffectedByCulling(false);
        level.addEntity(entity);

        VfxAnimation anim1 = VfxAnimationBuilder.create()
                .onStart(e -> player.sendSystemMessage(Component.literal("Anim 1")))
                .blockState(Blocks.ANVIL.defaultBlockState(), b -> {})
                .scale(1f, s -> s
                        .addKeyframe(1f, 1.5f, 0.4f, 1.5f, Easings.EASE_OUT_QUAD))
                .overlay(1f, 0f, 0f, 0f, o -> o
                        .addIntensityKeyframe(1f, 0.7f, Easings.EASE_IN_QUAD))
                .build(20);

        VfxAnimation anim2 = VfxAnimationBuilder.create()
                .onStart(e -> player.sendSystemMessage(Component.literal("Anim 2")))
                .inheritScale()
                .blockState(Blocks.LECTERN.defaultBlockState(), b -> {})
                .translation(t -> t
                        .addKeyframe(1f, 0f, 6f, 0f, Easings.EASE_OUT_EXPO))
                .scale(s -> s
                        .addKeyframe(0.2f, 0.4f, 2.5f, 0.4f, Easings.EASE_OUT_ELASTIC)
                        .addKeyframe(1f, 1f, 1f, 1f, Easings.EASE_IN_QUAD))
                .rotation(0, 0, 0, r -> r
                        .addKeyframe(1f, 0f, 720f, 0f, Easings.EASE_OUT_CUBIC))
                .overlay(1f, 0.6f, 0f, 0.7f, o -> o
                        .addColorKeyframe(1f, new Vector3f(1f, 1f, 1f))
                        .addIntensityKeyframe(1f, 0.2f, Easings.EASE_OUT_QUAD))
                .build(30);

        VfxAnimation anim3 = VfxAnimationBuilder.create()
                .onStart(e -> player.sendSystemMessage(Component.literal("Anim 3")))
                .inheritTranslation()
                .inheritBlockState()
                .blockState(Blocks.LECTERN.defaultBlockState(), b -> b
                        .addKeyframe(0.25f, Blocks.BELL.defaultBlockState())
                        .addKeyframe(0.35f, Blocks.MANGROVE_PRESSURE_PLATE.defaultBlockState())
                        .addKeyframe(0.5f, Blocks.DARK_OAK_SAPLING.defaultBlockState())
                        .addKeyframe(0.6f, Blocks.QUARTZ_STAIRS.defaultBlockState())
                        .addKeyframe(0.8f, Blocks.BAMBOO.defaultBlockState())
                        .addKeyframe(0.95f, Blocks.SMALL_AMETHYST_BUD.defaultBlockState()))
                .inheritRotation()
                .onTickRotation((rotation, context) -> {
                    rotation.rotationXYZ(0, (float) Math.toRadians(context.interpolatedTicks() * 30f), 0);
                })
                .onTickTranslation((translation, context) -> {
                    translation.y += (float) (Math.sin(context.interpolatedTicks() * 0.3f) * 4f);
                })
                .onTickScale((scale, context) -> {
                    scale.mul(Math.max(1.5f, (float) (Math.sin(context.interpolatedTicks() * 0.3f) * 4f)));
                })
                .inheritScale()
                .scale(s -> s
                        .addKeyframe(1f, 3f, Easings.EASE_OUT_EXPO))
                .inheritOverlayColor()
                .inheritOverlayIntensity()
                .overlay(o -> o
                        .addColorKeyframe(1f, new Vector3f(0f, 0f, 0f))
                        .addIntensityKeyframe(1f, 0f, Easings.EASE_IN_QUAD))
                .build(200);

        entity.playOrQueueAnimation(anim1);
        entity.playOrQueueAnimation(anim2);
        entity.playOrQueueAnimation(anim3);
    }

    public static void runItemEncasementTest() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Vec3 pos = player.getEyePosition().add(player.getLookAngle().normalize().scale(4f));
        VfxEntity entity = VfxEntity.create(level, pos);
        entity.setAffectedByCulling(false);
        level.addEntity(entity);

        VfxAnimation stage1 = VfxAnimationBuilder.create()
                .onStart(e -> player.sendSystemMessage(Component.literal("Stage 1: item only")))
                .itemStack(new ItemStack(Items.DIAMOND), i -> {})
                .scale(5f, s -> {})
                .rotation(0, 0, 0, r -> r
                        .addKeyframe(1f, 0, 360, 0, Easings.LINEAR))
                .onTickTranslation((translation, context) ->
                        translation.y += (float) (Math.sin(context.interpolatedTicks() * 0.15f) * 0.15f))
                .build(60);

        VfxAnimation stage2 = VfxAnimationBuilder.create()
                .onStart(e -> player.sendSystemMessage(Component.literal("Stage 2: encasing in glass")))
                .inheritItemStack()
                .inheritScale()
                .rotation(0, 0, 0, r -> r
                        .addKeyframe(1f, 0, 360, 0, Easings.LINEAR))
                .blockState(Blocks.CYAN_STAINED_GLASS.defaultBlockState(), b -> {})
                .onTickTranslation((translation, context) ->
                        translation.y += (float) (Math.sin(context.interpolatedTicks() * 0.15f) * 0.15f))
                .build(60);

        VfxAnimation stage3 = VfxAnimationBuilder.create()
                .onStart(e -> player.sendSystemMessage(Component.literal("Stage 3: item removed, glass remains")))
                .loopInfinite()
                .inheritScale()
                .inheritBlockState()
                .rotation(0, 0, 0, r -> r
                        .addKeyframe(1f, 0, 360, 0, Easings.LINEAR))
                .onTickTranslation((translation, context) ->
                        translation.y += (float) (Math.sin(context.interpolatedTicks() * 0.15f) * 0.15f))
                .build(60);

        entity.playOrQueueAnimation(stage1);
        entity.playOrQueueAnimation(stage2);
        entity.playOrQueueAnimation(stage3);
    }


    public static void runEverythingTest() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Vec3 pos = player.getEyePosition().add(player.getLookAngle().normalize().scale(4f));
        VfxEntity entity = VfxEntity.create(level, pos);
        level.addEntity(entity);

        VfxAnimation anim = VfxAnimationBuilder.create()
                .onStart(e -> {
                    System.out.println("STARTED");
                })
                .onEnd(e -> {
                    System.out.println("END");
                })
                .onLoop(e -> {
                    System.out.println("LOOPED");
                })
                .onKeyframeReached(0.5f, e -> {
                    System.out.println("HALF");
                })
                .loop(2)
                .blockState(Blocks.MAGMA_BLOCK.defaultBlockState(), b -> {})
                .translation(0, 0, 0, t -> t
                        .addKeyframe(0.25f, 0, 3, 0, Easings.EASE_OUT_QUAD)
                        .addKeyframe(0.5f, 2, -3, 0, Easings.EASE_IN_OUT_QUAD)
                        .addKeyframe(0.75f, 2, 0, 0, Easings.EASE_IN_QUAD)
                        .addKeyframe(1f, 0, 0, 0, Easings.EASE_OUT_BOUNCE))
                .scale(0.5f, s -> s
                        .addKeyframe(0.5f, (float)(Math.random()*2), (float)(Math.random()*2), (float)(Math.random()*2), Easings.EASE_OUT_ELASTIC)
                        .addKeyframe(1f, 0.5f, Easings.EASE_IN_QUAD))
                .rotation(0, 0, 0, r -> r
                        .addKeyframe(0.5f, 0, 180, 0, Easings.EASE_IN_OUT_QUAD)
                        .addKeyframe(1f, 0, 360, 0, Easings.EASE_OUT_QUAD))
                .overlay(new Vector3f(1, 0.3f, 0), 0.9f, o -> o
                        .addColorKeyframe(0.5f, new Vector3f(0.2f, 0.8f, 0.7f), Easings.EASE_IN_QUAD)
                        .addIntensityKeyframe(0.8f, 0.5f, Easings.EASE_OUT_QUAD)
                        .addColorKeyframe(1f, new Vector3f(0.1f))
                        .addIntensityKeyframe(1f, 0f, Easings.EASE_IN_QUAD))
                .build(60);

        entity.playAnimation(anim);
    }

    public static void runCoolEasings() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Vec3 pos = player.getEyePosition().add(player.getLookAngle().normalize().scale(6f));
        VfxEntity entity = VfxEntity.create(level, pos);
        level.addEntity(entity);
        float scale = 0.5f;

        VfxAnimation anim = VfxAnimationBuilder.create()
                .blockState(Blocks.MAGMA_BLOCK.defaultBlockState(), b -> {})
                .translation(0, 0, 0, t -> t
                        .addKeyframe(1f, (float)(Math.random() * 4 - 2), (float)(Math.random() * 4), (float)(Math.random() * 4 - 2), Easings.EASE_IN_QUAD))
                .scale(scale, s -> s
                        .addKeyframe(1f, scale * 5f, Easings.EASE_IN_OUT_ELASTIC))
                .rotation(0, 0, 0, r -> r
                        .addKeyframe(1f, (float)(Math.random() * 30), 360 + (float)(Math.random() * 360), (float)(Math.random() * 30), Easings.EASE_OUT_EXPO))
                .build(60);
        entity.playAnimation(anim);
    }

    public static void runPerformanceTest() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

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
            level.addEntity(entity);

            int duration = 1800 + (int)(Math.random() * 900);
            VfxAnimation anim = VfxAnimationBuilder.create()
                    .blockState(blocks[i % blocks.length], b -> {})
                    .translation(0, 0, 0, t -> t
                            .addKeyframe(1f, (float)(Math.random() * 4 - 2), (float)(Math.random() * 4), (float)(Math.random() * 4 - 2), Easings.random(player.getRandom())))
                    .scale(0.5f, s -> s
                            .addKeyframe(1f, (float)(Math.random() * 1.5f + 0.5f), (float)(Math.random() * 1.5f + 0.5f), (float)(Math.random() * 1.5f + 0.5f), Easings.random(player.getRandom())))
                    .rotation(0, 0, 0, rot -> rot
                            .addKeyframe(1f, (float)(Math.random() * 720), (float)(Math.random() * 720), (float)(Math.random() * 720), Easings.random(player.getRandom())))
                    .build(duration);
            entity.playAnimation(anim);
        }

        Minecraft.getInstance().player.sendSystemMessage(Component.literal("Spawned " + count + " VFX entities"));
    }

    public static void runVanillaPerformanceTest() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

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

            int duration = 1800 + (int) (Math.random() * 900);

            Vector3f endTranslation = new Vector3f(
                    (float) (Math.random() * 4 - 2),
                    (float) (Math.random() * 4),
                    (float) (Math.random() * 4 - 2)
            );
            Vector3f endScale = new Vector3f(
                    (float) (Math.random() * 1.5f + 0.5f),
                    (float) (Math.random() * 1.5f + 0.5f),
                    (float) (Math.random() * 1.5f + 0.5f)
            );
            Quaternionf endRotation = new Quaternionf().rotationXYZ(
                    (float) Math.toRadians(Math.random() * 720),
                    (float) Math.toRadians(Math.random() * 720),
                    (float) Math.toRadians(Math.random() * 720)
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

            level.addEntity(entity);
        }

        Minecraft.getInstance().player.sendSystemMessage(Component.literal("Spawned " + count + " vanilla Display entities"));
    }

    public static void runBulletTest() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        Vec3 spawnPos = player.getEyePosition().add(player.getLookAngle().scale(0.5f));
        Vec3 lookAngle = player.getLookAngle().normalize();

        float pitch = player.getXRot();
        float yaw = -player.getYRot();

        float blocksPerTick = 0.5f;

        VfxEntity bullet = VfxEntity.create(level, spawnPos);
        bullet.setInfinitePersist(true);
        bullet.setOnTick(e -> {
            Vec3 currentPos = e.position();
            e.setPos(currentPos.add(lookAngle.scale(blocksPerTick)));
        });
        level.addEntity(bullet);

        VfxAnimation bulletAnim = VfxAnimationBuilder.create()
                .scale(0.4f, 0.4f, 1.5f, s -> {})
                .blockState(Blocks.SHROOMLIGHT.defaultBlockState(), b -> {})
                .rotation(pitch, yaw, 0f, r -> {})
                .build(200);

        bullet.playAnimation(bulletAnim);
    }
}