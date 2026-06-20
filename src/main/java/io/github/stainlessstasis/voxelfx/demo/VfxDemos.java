package io.github.stainlessstasis.voxelfx.demo;

import com.mojang.math.Transformation;
import io.github.stainlessstasis.voxelfx.animation.VfxAnimation;
import io.github.stainlessstasis.voxelfx.animation.VfxAnimationBuilder;
import io.github.stainlessstasis.voxelfx.entity.VfxEntity;
import io.github.stainlessstasis.voxelfx.task.CancellableRunnable;
import io.github.stainlessstasis.voxelfx.task.ClientTaskScheduler;
import io.github.stainlessstasis.voxelfx.util.VfxUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Brightness;
import net.minecraft.util.EasingType;
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

    /**
     * Easings are functions which can drastically affect the way an animation looks, giving it a different feeling.<br>
     *
     * <p>Each easing (except linear) has 3 types:
     *  <ul>
     *  <li><b>IN</b> - starts slow, accelerates toward the end</li>
     *  <li><b>OUT</b> - starts fast, decelerates toward the end</li>
     *  <li><b>IN_OUT</b> - slow at both ends, fast in the middle</li>
     *  </ul>
     *
     * <p>Unsure which easing to use? Try matching the easing with the "feeling" of your animation:
     * <ul>
     *   <li><b>LINEAR</b> - constant speed, no acceleration. Feels robotic and unnatural for physical objects.
     *   Best for things that should track a value mechanically, like a constant spin.</li>
     *
     *   <li><b>SINE</b> - the gentlest curve, barely distinguishable from linear at a glance.
     *   Feels organic and calm, like breathing or floating. Good for looping idle animations and subtle ambient motion.</li>
     *
     *   <li><b>QUAD</b> - a noticeable but natural-feeling curve. Feels responsive and grounded, like everyday physical objects.
     *   When in doubt, start here.</li>
     *
     *   <li><b>CUBIC</b> - more pronounced than QUAD. Feels deliberate and weighty, like something with real mass starting or stopping.
     *   Good for larger movements that should feel purposeful.</li>
     *
     *   <li><b>QUART</b> - aggressive curve, especially at the extremes. Feels snappy and high-energy.
     *   OUT_QUART in particular is great for explosions and impacts that need to feel violent.</li>
     *
     *   <li><b>QUINT</b> - very aggressive. Almost all motion is compressed into a tiny window.
     *   Feels explosive and extreme. Use sparingly for maximum dramatic effect.</li>
     *
     *   <li><b>EXPO</b> - the most extreme of the "smooth" curves. IN_EXPO barely moves for most of its duration then launches suddenly.
     *   OUT_EXPO does the reverse - nearly instant movement that fades into stillness.</li>
     *
     *   <li><b>CIRC</b> - follows a circular arc. Feels similar to EXPO but more sharp and precise.
     *   Good for mechanical effects.</li>
     *
     *   <li><b>BACK</b> - overshoots the target slightly before settling. Feels springy and alive, like something with momentum that corrects itself.
     *   Great for objects that should feel like they have personality.</li>
     *
     *   <li><b>ELASTIC</b> - overshoots dramatically and oscillates like a spring before settling.
     *   Feels bouncy and exaggerated. Use for anything that should feel rubbery and alive. Can be overwhelming if overused.</li>
     *
     *   <li><b>BOUNCE</b> - simulates a physical bounce, decelerating in discrete steps.
     *   OUT_BOUNCE feels like dropping a ball and watching it settle. Feels playful and physical.
     *   IN_BOUNCE is rarely useful on its own but can work well when paired with other animations in a queue.</li>
     * </ul>
     */
    public static void demoKeyframesAndEasings(ClientLevel level, LocalPlayer player) {
        Vec3 pos = getFrontPosition(player);
        float spacing = 3f;
        List<EasingType> easings = List.of(EasingType.LINEAR, EasingType.OUT_BOUNCE, EasingType.IN_OUT_ELASTIC, EasingType.OUT_EXPO);
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
                        .addKeyframe(1f, 0, 360, 0, EasingType.LINEAR))
                .loopInfinite()
                .build(60));
    }

    // STOP / PAUSE / RESUME / PLAYBACK SPEED
    public static void demoPlaybackControls(ClientLevel level, LocalPlayer player) {
        Vec3 pos = getFrontPosition(player);
        VfxEntity entity = VfxEntity.create(level, pos);

        entity.playAnimation(VfxAnimationBuilder.create()
                .blockState(Blocks.EMERALD_BLOCK.defaultBlockState(), builder -> {})
                .scale(1f, builder -> {})
                .translation(builder -> builder
                        .addKeyframe(1f, 0, 5, 0, EasingType.LINEAR))
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
                        .addIntensityKeyframe(0.25f, 0.8f, EasingType.OUT_QUAD)
                        .addColorKeyframe(0.5f, new Vector3f(0f, 1f, 0f))
                        .addColorKeyframe(0.75f, new Vector3f(0f, 0f, 1f))
                        .addColorKeyframe(0.9f, new Vector3f(1f, 0f, 0f))
                        .addIntensityKeyframe(0.9f, 0.8f)
                        .addIntensityKeyframe(1f, 0f, EasingType.IN_QUAD))
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
                        .addKeyframe(1f, 0, 0, 0, EasingType.IN_QUART))
                .onStart(vfxEntity -> level.playLocalSound(pos.x, pos.y, pos.z,
                        SoundEvents.ANVIL_FALL, SoundSource.AMBIENT, 1f, 0.8f, false))
                .build(20);

        // stage 2: squish on impact, inheriting where it landed
        VfxAnimation squish = VfxAnimationBuilder.create()
                .inheritBlockState()
                .inheritTranslation()
                .scale(1f, builder -> builder
                        .addKeyframe(0.3f, 2f, 0.2f, 2f, EasingType.OUT_EXPO)
                        .addKeyframe(1f, 1f, 1f, 1f, EasingType.OUT_BOUNCE))
                .onStart(vfxEntity -> level.playLocalSound(pos.x, pos.y, pos.z,
                        SoundEvents.ANVIL_LAND, SoundSource.AMBIENT, 1f, 1.2f, false))
                .build(30);

        // stage 3: explode outward, inheriting the scale
        VfxAnimation explode = VfxAnimationBuilder.create()
                .inheritBlockState()
                .inheritTranslation()
                .inheritScale()
                .scale(builder -> builder
                        .addKeyframe(1f, 4f, EasingType.OUT_EXPO))
                .overlay(1f, 0.5f, 0f, 0f, builder -> builder
                        .addIntensityKeyframe(0.2f, 0.9f, EasingType.OUT_QUAD)
                        .addIntensityKeyframe(1f, 0f, EasingType.IN_QUAD))
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
                        .addIntensityKeyframe(0.3f, 0.6f, EasingType.OUT_QUAD)
                        .addIntensityKeyframe(0.5f, 0f, EasingType.IN_QUAD)
                        .addIntensityKeyframe(1f, 0f))
                .scale(1f, builder -> builder
                        .addKeyframe(0.3f, 1.3f, EasingType.OUT_QUAD)
                        .addKeyframe(0.5f, 1f, EasingType.IN_QUAD))
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
                        .addKeyframe(0.5f, 1f, EasingType.OUT_ELASTIC)
                        .addKeyframe(1f, 0.5f, EasingType.IN_QUAD))
                .loopInfinite()
                .build(60));
    }

    public static void demoEntityBinding(ClientLevel level, LocalPlayer player) {
        Vec3 pos = getFrontPosition(player);
        Snowball snowball = new Snowball(EntityType.SNOWBALL, level);
        snowball.setPos(pos);
        Vec3 look = player.getLookAngle();
        snowball.setDeltaMovement(look.scale(1.5f));

        VfxEntity trail = VfxEntity.createBoundTo(level, snowball);
        // manually tick the snowball since clientside entities dont really work due to 26.2 changes. dont add it to the level
        trail.setOnTick(vfxEntity -> {
            if (snowball.isAlive()) {
                snowball.tick();
            }
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
                        .addKeyframe(1f, 360, 360, 0, EasingType.LINEAR))
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
                                    .addIntensityKeyframe(1.0f, 0.0f, EasingType.OUT_QUAD))
                            .scale(s -> s
                                    .addKeyframe(1.0f, 2f, EasingType.OUT_EXPO))
                            .build(12));
                })
                .loopInfinite()
                .build(40));
    }

    public static void demoShockwave(ClientLevel level, LocalPlayer player) {
        EffectPresets.shockwave(level, player.position(), ShockwaveConfig.getDefault());
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


            int duration = 1800 + (int) VfxUtils.randomBetween(0f, 900f);
            VfxAnimation anim = VfxAnimationBuilder.create()
                    .blockState(blocks[i % blocks.length], b -> {})
                    .translation(0, 0, 0, t -> t
                            .addRandomKeyframe(1f, -2f, 2f, 0f, 4f, -2f, 2f, VfxUtils.getRandomEasing(player.getRandom())))
                    .scale(0.5f, s -> s
                            .addRandomKeyframe(1f, 0.5f, 2f, 0.5f, 2f, 0.5f, 2f, VfxUtils.getRandomEasing(player.getRandom())))
                    .rotation(0, 0, 0, rot -> rot
                            .addRandomKeyframe(1f, 0f, 720f, 0f, 720f, 0f, 720f, VfxUtils.getRandomEasing(player.getRandom())))
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

            int duration = 1800 + (int) VfxUtils.randomBetween(0f, 900f);

            Vector3f endTranslation = new Vector3f(
                    VfxUtils.randomBetween(-2f, 2f),
                    VfxUtils.randomBetween(0f, 4f),
                    VfxUtils.randomBetween(-2f, 2f)
            );
            Vector3f endScale = new Vector3f(
                    VfxUtils.randomBetween(0.5f, 2f),
                    VfxUtils.randomBetween(0.5f, 2f),
                    VfxUtils.randomBetween(0.5f, 2f)
            );
            Quaternionf endRotation = new Quaternionf().rotationXYZ(
                    (float) Math.toRadians(VfxUtils.randomBetween(0f, 720f)),
                    (float) Math.toRadians(VfxUtils.randomBetween(0f, 720f)),
                    (float) Math.toRadians(VfxUtils.randomBetween(0f, 720f))
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