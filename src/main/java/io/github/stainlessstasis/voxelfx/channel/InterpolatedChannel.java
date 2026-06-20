package io.github.stainlessstasis.voxelfx.channel;

import net.minecraft.util.EasingType;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class InterpolatedChannel<S, T> implements Channel<T> {
    private final List<Keyframe<S>> keyframes;
    private final LerpFunction<S, T> lerpFunc;

    public InterpolatedChannel(List<Keyframe<S>> keyframes, LerpFunction<S, T> lerpFunc) {
        this.keyframes = List.copyOf(keyframes);
        this.lerpFunc = lerpFunc;
    }

    public static <S, T> InterpolatedChannel<S, T> holdChannel(S value, InterpolatedChannel.LerpFunction<S, T> lerp) {
        return new InterpolatedChannel<>(
                List.of(new Keyframe<>(0f, value, EasingType.LINEAR),
                        new Keyframe<>(1f, value, EasingType.LINEAR)),
                lerp
        );
    }

    @Override
    public T evaluate(float t, T destination) {
        return evaluate(t, destination, null);
    }

    public T evaluate(float t, T destination, @Nullable S fallbackStartValue) {
        var values = getValues(t, fallbackStartValue);
        this.lerpFunc.lerp(values.start(), values.end(), values.easedT(), destination);
        return destination;
    }

    public S resolveValueAt(float t, @Nullable S fallbackStartValue, SelfLerpFunction<S> lerpFunc) {
        var values = getValues(t, fallbackStartValue);
        return lerpFunc.lerp(values.start(), values.end(), values.easedT());
    }

    private Values<S> getValues(float t, @Nullable S fallbackStartValue) {
        Keyframe<S> prev = keyframes.getFirst();
        Keyframe<S> next = keyframes.getLast();

        for (int i = 0; i < keyframes.size() - 1; i++) {
            if (t <= keyframes.get(i + 1).time()) {
                prev = keyframes.get(i);
                next = keyframes.get(i + 1);
                break;
            }
        }

        float segmentT = Mth.inverseLerp(t, prev.time(), next.time());
        float easedT = next.easing().apply(segmentT);

        S startVal = (prev == keyframes.getFirst() && fallbackStartValue != null) ? fallbackStartValue : prev.value();
        S endVal = (next == keyframes.get(1) && keyframes.size() == 2 && fallbackStartValue != null) ? fallbackStartValue : next.value();
        return new Values<>(startVal, endVal, easedT);
    }

    private record Values<S>(S start, S end, float easedT) {}

    @FunctionalInterface
    public interface LerpFunction<S, T> {
        void lerp(S start, S end, float t, T destination);
    }

    @FunctionalInterface
    public interface SelfLerpFunction<S> {
        S lerp(S start, S end, float t);
    }
}
