package io.github.stainlessstasis.bdanimator.channel;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BlockStateChannel {
    private final List<Keyframe<BlockState>> keyframes;

    public BlockStateChannel(List<Keyframe<BlockState>> keyframes) {
        this.keyframes = List.copyOf(keyframes);
    }

    public BlockState getLastKeyframeValue() {
        return keyframes.getLast().value();
    }

    public BlockState evaluate(float t, @Nullable BlockState fallbackStartValue) {
        BlockState result = (fallbackStartValue != null) ? fallbackStartValue : keyframes.getFirst().value();
        for (int i = 1; i < keyframes.size(); i++) {
            Keyframe<BlockState> keyframe = keyframes.get(i);
            if (t >= keyframe.time()) {
                result = keyframe.value();
            } else {
                break;
            }
        }
        return result;
    }
}
