package io.github.stainlessstasis.voxelfx.demo;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record ShockwaveConfig(
    List<BlockState[]> palettes,
    int ringCount,
    int baseDuration,
    float maxRadius,
    float minScale,
    float maxScale
) {
    public static class Builder {
        private List<BlockState[]> palettes = List.of();
        private int ringCount = 48;
        private int baseDuration = 20;
        private float maxRadius = 10f;
        private float minScale = 2f;
        private float maxScale = 2.75f;

        public Builder palettes(List<BlockState[]> palettes) { this.palettes = palettes; return this; }
        public Builder ringCount(int ringCount) { this.ringCount = ringCount; return this; }
        public Builder baseDuration(int baseDuration) { this.baseDuration = baseDuration; return this; }
        public Builder maxRadius(float maxRadius) { this.maxRadius = maxRadius; return this; }
        public Builder scaleRange(float min, float max) { this.minScale = min; this.maxScale = max; return this; }

        public ShockwaveConfig build() {
            if (palettes.isEmpty()) {
                palettes = getDefault().palettes;
            }
            return new ShockwaveConfig(palettes, ringCount, baseDuration, maxRadius, minScale, maxScale);
        }
    }

    public static ShockwaveConfig getDefault() {
        BlockState[][] defaultPalettes = {
                { Blocks.SHROOMLIGHT.defaultBlockState(), Blocks.ORANGE_CONCRETE.defaultBlockState(), Blocks.GRAY_STAINED_GLASS.defaultBlockState() },
                { Blocks.SHROOMLIGHT.defaultBlockState(), Blocks.ORANGE_STAINED_GLASS.defaultBlockState(), Blocks.WHITE_STAINED_GLASS.defaultBlockState() },
                { Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.RED_STAINED_GLASS.defaultBlockState(), Blocks.BLACK_STAINED_GLASS.defaultBlockState() }
        };

        return new Builder()
                .palettes(List.of(defaultPalettes))
                .build();
    }

    public static Builder create() {
        return new Builder();
    }
}