package io.github.stainlessstasis.voxelfx.channel;

import net.minecraft.util.EasingType;

public record Keyframe<T>(float time, T value, EasingType easing) {}
