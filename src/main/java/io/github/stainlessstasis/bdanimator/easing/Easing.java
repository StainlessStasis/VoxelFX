package io.github.stainlessstasis.bdanimator.easing;

import net.minecraft.util.RandomSource;

import static io.github.stainlessstasis.bdanimator.easing.EasingConstants.*;

// Credit --- Easing formulas from: https://easings.net/
public enum Easing {
    LINEAR(t -> t),
    EASE_IN_SINE(t -> (float) (1 - Math.cos((t*Math.PI) / 2))),
    EASE_OUT_SINE(t -> (float) Math.sin((t * Math.PI) / 2)),
    EASE_IN_OUT_SINE(t -> (float) (-(Math.cos(Math.PI*t) - 1) / 2)),
    EASE_IN_QUAD(t -> t*t),
    EASE_OUT_QUAD(t -> 1 - (1 - t) * (1 - t)),
    EASE_IN_OUT_QUAD(t -> t < 0.5 ?
            2*t*t :
            (float) (1 - Math.pow(-2 * t + 2, 2) / 2)),
    EASE_IN_CUBIC(t -> t*t*t),
    EASE_OUT_CUBIC(t -> (float) (1 - Math.pow(1-t, 3))),
    EASE_IN_OUT_CUBIC(t -> t < 0.5 ?
            4*t*t*t :
            (float) (1 - Math.pow(-2 * t + 2, 3) / 2)),
    EASE_IN_QUART(t -> t*t*t*t),
    EASE_OUT_QUART(t -> (float) (1 - Math.pow(1 - t, 4))),
    EASE_IN_OUT_QUART(t -> t < 0.5 ?
            8*t*t*t*t :
            (float) (1 - Math.pow(-2 * t + 2, 4) / 2)),
    EASE_IN_QUINT(t -> t*t*t*t*t),
    EASE_OUT_QUINT(t -> (float) (1 - Math.pow(1-t, 5))),
    EASE_IN_OUT_QUINT(t -> t < 0.5 ?
            16*t*t*t*t*t :
            (float) (1 - Math.pow(-2 * t + 2, 5) / 2)),
    EASE_IN_EXPO(t -> t==0 ? 0 : (float) Math.pow(2, 10 * t - 10)),
    EASE_OUT_EXPO(t -> t==1 ? 1 : (float) (1 - Math.pow(2, -10 * t))),
    EASE_IN_OUT_EXPO(t -> t==0 ? 0 :
            (float) (t == 1 ? 1 :
             t < 0.5 ? Math.pow(2, 20 * t - 10) / 2 :
             (2 - Math.pow(2, -20 * t + 10)) / 2)),
    EASE_IN_CIRC(t -> (float) (1 - Math.sqrt(1 - Math.pow(t, 2)))),
    EASE_OUT_CIRC(t -> (float) Math.sqrt(1 - Math.pow(t-1, 2))),
    EASE_IN_OUT_CIRC(t -> (float) (t < 0.5 ?
            (1 - Math.sqrt(1 - Math.pow(2*t, 2))) / 2 :
            (Math.sqrt(1 - Math.pow(-2*t+2, 2)) + 1) / 2)),
    EASE_IN_BACK(t -> C3*t*t*t - C1*t*t),
    EASE_OUT_BACK(t -> (float) (1 + C3 * Math.pow(t-1, 3) + C1 * Math.pow(t-1, 2))),
    EASE_IN_OUT_BACK(t -> (float) (t < 0.5 ?
            (Math.pow(2*t, 2) * ((C2+1) * 2 * t - C2)) / 2 :
            (Math.pow(2*t-2, 2) * ((C2+1) * (t*2-2) + C2) + 2) / 2)),
    EASE_IN_ELASTIC(t -> t==0 ? 0 : (float) (t == 1 ? 1 : -Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * C4))),
    EASE_OUT_ELASTIC(t -> t==0 ? 0 : (float) (t == 1 ? 1 : Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * C4) + 1)),
    EASE_IN_OUT_ELASTIC(t -> t==0 ? 0 :
            (float) (t == 1 ? 1 : t < 0.5 ? -(Math.pow(2, 20 * t - 10) * Math.sin((20 * t - 11.125) * C5)) / 2 :
            (Math.pow(2, -20 * t + 10) * Math.sin((20 * t - 11.125) * C5)) / 2 + 1)),
    EASE_OUT_BOUNCE(t -> {
            if (t < 1/D1) {return N1*t*t;}
            else if (t < 2f/D1) {return (float) (N1 * (t -= 1.5f/D1) * t + 0.75);}
            else if (t < 2.5f/D1){return (float) (N1 * (t -= 2.25f/D1) * t + 0.9375);}
            else {return N1 * (t -= 2.625f/D1) * t + 0.984375f;}
    }),
    EASE_IN_BOUNCE(t -> 1 - EASE_OUT_BOUNCE.apply(1-t)),
    EASE_IN_OUT_BOUNCE(t -> t < 0.5 ?
            (1 - EASE_OUT_BOUNCE.apply(1-2*t)) / 2 :
            (1 + EASE_OUT_BOUNCE.apply(2*t-1)) / 2);

    private static final Easing[] VALUES = values();
    public static Easing random(RandomSource random) {
        return VALUES[random.nextInt(VALUES.length)];
    }

    private final EasingFunction formula;
    Easing(EasingFunction formula) {
        this.formula = formula;
    }

    public float apply(float t) {
        return formula.apply(Math.clamp(t, 0f, 1f));
    }

    @FunctionalInterface
    public interface EasingFunction {
        float apply(float t);
    }
}
