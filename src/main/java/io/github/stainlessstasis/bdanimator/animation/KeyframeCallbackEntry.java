package io.github.stainlessstasis.bdanimator.animation;

import io.github.stainlessstasis.bdanimator.entity.VfxEntity;

import java.util.function.Consumer;

public record KeyframeCallbackEntry(float time, Consumer<VfxEntity> callback) {
}
