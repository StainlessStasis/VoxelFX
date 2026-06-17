package io.github.stainlessstasis.bdanimator.animation;

import io.github.stainlessstasis.bdanimator.easing.Easing;
import io.github.stainlessstasis.bdanimator.registry.BDAnimatorEntities;
import io.github.stainlessstasis.bdanimator.entity.VfxEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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

            VfxEntity entity = new VfxEntity(BDAnimatorEntities.VFX_ENTITY.get(), level);
            entity.setPos(center);
            level.addEntity(entity);

            VfxAnimation anim = VfxAnimationBuilder.create()
                    .blockState(sequence[0], b -> b
                            .addKeyframe(fireTransition, sequence[1])
                            .addKeyframe(smokeTransition, sequence[2]))
                    .translation(0, 0, 0, t -> t
                            .addKeyframe(1f, dirX * radius, 0, dirZ * radius, Easing.EASE_OUT_EXPO))
                    .scale(startScale, s -> s
                            .addKeyframe(0.7f, startScale * 0.75f, Easing.EASE_IN_QUAD)
                            .addKeyframe(1f, endScale, Easing.EASE_IN_QUAD))
                    .rotation(randomPitch, randomYaw, randomRoll, r -> r
                            .addKeyframe(1f, randomEndPitch, randomEndYaw, randomEndRoll, Easing.EASE_OUT_QUAD))
                    .overlay(new Vector3f(1f, 0.5f, 0f), 0.8f, o -> o
                            .addColorKeyframe(fireTransition, new Vector3f(0.8f, 0.0f, 0f), Easing.EASE_IN_QUAD)
                            .addColorKeyframe(smokeTransition, new Vector3f(0.1f, 0.0f, 0.0f), Easing.EASE_IN_QUAD)
                            .addIntensityKeyframe(smokeTransition, 0.2f, Easing.EASE_IN_QUAD)
                            .addColorKeyframe(1f, new Vector3f(0.05f, 0.05f, 0.05f))
                            .addIntensityKeyframe(1f, 0f, Easing.EASE_IN_QUAD))
                    .build((int)(duration + Math.random() * 10));

            entity.playAnimation(anim);
        }
    }

    public static void runKeyframeTest() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        VfxEntity entity = new VfxEntity(BDAnimatorEntities.VFX_ENTITY.get(), level);
        Vec3 pos = player.getEyePosition().add(player.getLookAngle().normalize().scale(4f));
        entity.setPos(pos);
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
                        .addKeyframe(0.25f, 0, 3, 0, Easing.EASE_OUT_QUAD)
                        .addKeyframe(0.5f, 2, -3, 0, Easing.EASE_IN_OUT_QUAD)
                        .addKeyframe(0.75f, 2, 0, 0, Easing.EASE_IN_QUAD)
                        .addKeyframe(1f, 0, 0, 0, Easing.EASE_OUT_BOUNCE))
                .scale(0.5f, s -> s
                        .addKeyframe(0.5f, (float)(Math.random()*2), (float)(Math.random()*2), (float)(Math.random()*2), Easing.EASE_OUT_ELASTIC)
                        .addKeyframe(1f, 0.5f, Easing.EASE_IN_QUAD))
                .rotation(0, 0, 0, r -> r
                        .addKeyframe(0.5f, 0, 180, 0, Easing.EASE_IN_OUT_QUAD)
                        .addKeyframe(1f, 0, 360, 0, Easing.EASE_OUT_QUAD))
                .overlay(new Vector3f(1, 0.3f, 0), 0.9f, o -> o
                        .addColorKeyframe(0.5f, new Vector3f(0.2f, 0.8f, 0.7f), Easing.EASE_IN_QUAD)
                        .addIntensityKeyframe(0.8f, 0.5f, Easing.EASE_OUT_QUAD)
                        .addColorKeyframe(1f, new Vector3f(0.1f))
                        .addIntensityKeyframe(1f, 0f, Easing.EASE_IN_QUAD))
                .build(60);

        entity.playAnimation(anim);
    }

    public static void runCoolEasings() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        VfxEntity entity = new VfxEntity(BDAnimatorEntities.VFX_ENTITY.get(), level);
        Vec3 pos = player.getEyePosition().add(player.getLookAngle().normalize().scale(6f));
        entity.setPos(pos);
        level.addEntity(entity);
        float scale = 0.5f;

        VfxAnimation anim = VfxAnimationBuilder.create()
                .blockState(Blocks.MAGMA_BLOCK.defaultBlockState(), b -> {})
                .translation(0, 0, 0, t -> t
                        .addKeyframe(1f, (float)(Math.random() * 4 - 2), (float)(Math.random() * 4), (float)(Math.random() * 4 - 2), Easing.EASE_IN_QUAD))
                .scale(scale, s -> s
                        .addKeyframe(1f, scale * 5f, Easing.EASE_IN_OUT_ELASTIC))
                .rotation(0, 0, 0, r -> r
                        .addKeyframe(1f, (float)(Math.random() * 30), 360 + (float)(Math.random() * 360), (float)(Math.random() * 30), Easing.EASE_OUT_EXPO))
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

            VfxEntity entity = new VfxEntity(BDAnimatorEntities.VFX_ENTITY.get(), level);
            entity.setPos(x, y, z);
            level.addEntity(entity);

            int duration = 1800 + (int)(Math.random() * 900);
            VfxAnimation anim = VfxAnimationBuilder.create()
                    .blockState(blocks[i % blocks.length], b -> {})
                    .translation(0, 0, 0, t -> t
                            .addKeyframe(1f, (float)(Math.random() * 4 - 2), (float)(Math.random() * 4), (float)(Math.random() * 4 - 2), Easing.random(player.getRandom())))
                    .scale(0.5f, s -> s
                            .addKeyframe(1f, (float)(Math.random() * 1.5f + 0.5f), (float)(Math.random() * 1.5f + 0.5f), (float)(Math.random() * 1.5f + 0.5f), Easing.random(player.getRandom())))
                    .rotation(0, 0, 0, rot -> rot
                            .addKeyframe(1f, (float)(Math.random() * 720), (float)(Math.random() * 720), (float)(Math.random() * 720), Easing.random(player.getRandom())))
                    .build(duration);
            entity.playAnimation(anim);
        }

        Minecraft.getInstance().player.sendSystemMessage(Component.literal("Spawned " + count + " VFX entities"));
    }
}