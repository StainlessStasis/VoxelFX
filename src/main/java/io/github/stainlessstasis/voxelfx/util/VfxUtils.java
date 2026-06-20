package io.github.stainlessstasis.voxelfx.util;

import net.minecraft.util.EasingType;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Consumer;

public class VfxUtils {
    public static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;

    public static void forEachPointOnSphere(int count, boolean randomized, Consumer<Vector3f> action) {
        forEachPointOnSphere(count, randomized, null, action);
    }

    /**
     * Executes an action for a given number of points evenly distributed or randomized across a sphere.
     * Provides a normalized direction vector for each point.
     */
    public static void forEachPointOnSphere(int count, boolean randomized, @Nullable RandomSource random, Consumer<Vector3f> action) {
        for (int i = 0; i < count; i++) {
            double theta, phi;
            if (randomized) {
                theta = getRandom(random) * 2 * Math.PI;
                phi = Math.acos(2 * getRandom(random) - 1);
            } else {
                theta = 2 * Math.PI * i / GOLDEN_RATIO;
                phi = Math.acos(1 - 2 * (i + 0.5f) / count);
            }

            float x = (float) (Math.sin(phi) * Math.cos(theta));
            float y = (float) (Math.sin(phi) * Math.sin(theta));
            float z = (float) Math.cos(phi);

            action.accept(new Vector3f(x, y, z));
        }
    }

    /**
     * Executes an action for points along a flat ring/circle, providing a directional unit vector.
     */
    public static void forEachPointOnRing(int count, float jitter, Consumer<Vector3f> action) {
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            if (jitter > 0) {
                angle += randomBetween(-jitter, jitter);
            }
            float x = (float) Math.cos(angle);
            float z = (float) Math.sin(angle);

            action.accept(new Vector3f(x, 0, z));
        }
    }

    public static float randomBetween(float min, float max) {
        return randomBetween(min, max, null);
    }
    public static float randomBetween(float min, float max, @Nullable RandomSource random) {
        return min + (float)(getRandom(random) * (max - min));
    }

    public static double randomBetween(double min, double max) {
        return randomBetween(min, max, null);
    }
    public static double randomBetween(double min, double max, @Nullable RandomSource random) {
        return min + getRandom(random) * (max - min);
    }

    public static int randomBetween(int min, int max) {
        return randomBetween(min, max, null);
    }
    public static int randomBetween(int min, int max, @Nullable RandomSource random) {
        return min + (int)(getRandom(random) * (max - min));
    }

    @SafeVarargs
    public static <T> T randomOf(T... options) {
        return randomOf(null, options);
    }
    @SafeVarargs
    public static <T> T randomOf(@Nullable RandomSource random, T... options) {
        return options[(int) (getRandom(random) * options.length)];
    }
    public static <T> T randomOf(List<T> options) {
        return randomOf(null, options);
    }
    public static <T> T randomOf(@Nullable RandomSource random, List<T> options) {
        if (options == null || options.isEmpty()) return null;
        return options.get((int) (getRandom(random) * options.size()));
    }

    /**
     * @return The result of the RandomSource's nextDouble(), if not null. If null, Math.random() is used as a fallback.
     */
    public static double getRandom(@Nullable RandomSource random) {
        return random != null ? random.nextDouble() : Math.random();
    }

    /**
     * Gets a random easing from the EasingType registry.
     * If the registry is empty or not yet initialized, falls back to LINEAR.
     * If the RandomSource provided is null, falls back to Math.random().
     */
    public static EasingType getRandomEasing(@Nullable RandomSource random) {
        List<EasingType> easings = EasingType.SIMPLE_REGISTRY.values().stream().toList();

        if (easings.isEmpty()) {
            return EasingType.LINEAR;
        }

        int index = (random != null)
                ? random.nextInt(easings.size())
                : (int) (Math.random() * easings.size());

        return easings.get(index);
    }
}
