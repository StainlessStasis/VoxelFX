package io.github.stainlessstasis.bdanimator.animation;

import io.github.stainlessstasis.bdanimator.channel.DiscreteChannel;
import io.github.stainlessstasis.bdanimator.channel.Interpolators;
import io.github.stainlessstasis.bdanimator.channel.Keyframe;
import io.github.stainlessstasis.bdanimator.channel.InterpolatedChannel;
import io.github.stainlessstasis.bdanimator.easing.Easing;
import io.github.stainlessstasis.bdanimator.easing.Easings;
import io.github.stainlessstasis.bdanimator.entity.VfxEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VfxAnimationBuilder {
    public static final InterpolatedChannel<Vector3f, Vector3f> DEFAULT_TRANSLATION = new InterpolatedChannel<>(
            List.of(new Keyframe<>(0f, new Vector3f(0f), Easings.STATIC_LINEAR),
                    new Keyframe<>(1f, new Vector3f(0f), Easings.STATIC_LINEAR)),
            Interpolators::lerpVector3f
    );
    public static final InterpolatedChannel<Vector3f, Vector3f> DEFAULT_SCALE = new InterpolatedChannel<>(
            List.of(new Keyframe<>(0f, new Vector3f(1f), Easings.STATIC_LINEAR),
                    new Keyframe<>(1f, new Vector3f(1f), Easings.STATIC_LINEAR)),
            Interpolators::lerpVector3f
    );
    public static final InterpolatedChannel<Vector3f, Quaternionf> DEFAULT_ROTATION = new InterpolatedChannel<>(
            List.of(new Keyframe<>(0f, new Vector3f(0f), Easings.STATIC_LINEAR),
                    new Keyframe<>(1f, new Vector3f(0f), Easings.STATIC_LINEAR)),
            Interpolators::lerpDegrees
    );
    public static final InterpolatedChannel<Vector3f, Vector3f> DEFAULT_OVERLAY_COLOR = new InterpolatedChannel<>(
            List.of(new Keyframe<>(0f, new Vector3f(1f), Easings.STATIC_LINEAR),
                    new Keyframe<>(1f, new Vector3f(1f), Easings.STATIC_LINEAR)),
            Interpolators::lerpVector3f
    );
    public static final InterpolatedChannel<Float, float[]> DEFAULT_OVERLAY_INTENSITY = new InterpolatedChannel<>(
            List.of(new Keyframe<>(0f, 0f, Easings.STATIC_LINEAR),
                    new Keyframe<>(1f, 0f, Easings.STATIC_LINEAR)),
            Interpolators::lerpFloat
    );
    public static final DiscreteChannel<BlockState> DEFAULT_BLOCK_STATE = new DiscreteChannel<>(
            List.of(new Keyframe<>(0f, Blocks.AIR.defaultBlockState(), Easings.STATIC_LINEAR))
    );
    public static final DiscreteChannel<ItemStack> DEFAULT_ITEM_STACK = new DiscreteChannel<>(
            List.of(new Keyframe<>(0f, ItemStack.EMPTY, Easings.STATIC_LINEAR))
    );

    public static VfxAnimationBuilder create() {
        return new VfxAnimationBuilder();
    }

    private static Vector3f toVector3f(Vec3 vec) {
        return new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
    }
    private static Vector3f toVector3f(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    private InterpolatedChannel<Vector3f, Vector3f> translationChannel;
    private InterpolatedChannel<Vector3f, Vector3f> scaleChannel;
    private InterpolatedChannel<Vector3f, Quaternionf> rotationChannel;
    private InterpolatedChannel<Vector3f, Vector3f> overlayColorChannel;
    private InterpolatedChannel<Float, float[]> overlayIntensityChannel;
    private DiscreteChannel<BlockState> blockStateChannel;
    private DiscreteChannel<ItemStack> itemStackChannel;
    private boolean inheritTranslation;
    private boolean inheritScale;
    private boolean inheritRotation;
    private boolean inheritOverlayColor;
    private boolean inheritOverlayIntensity;
    private boolean inheritBlockState;
    private boolean inheritItemStack;
    private @Nullable VfxAnimation.Vector3fTickModifier translationModifier;
    private @Nullable VfxAnimation.Vector3fTickModifier scaleModifier;
    private @Nullable VfxAnimation.QuaternionfTickModifier rotationModifier;
    private @Nullable VfxAnimation.Vector3fTickModifier overlayColorModifier;
    private @Nullable VfxAnimation.FloatTickModifier overlayIntensityModifier;
    private Vector3f rotationPivot = new Vector3f(0.5f);
    private @Nullable Consumer<VfxEntity> onStart;
    private @Nullable Consumer<VfxEntity> onEnd;
    private @Nullable Consumer<VfxEntity> onLoop;
    private int loopCount = 0;
    private final Map<Float, Consumer<VfxEntity>> keyframeCallbacks = new LinkedHashMap<>();

    public VfxAnimationBuilder onStart(Consumer<VfxEntity> callback) {
        this.onStart = callback;
        return this;
    }
    public VfxAnimationBuilder onEnd(Consumer<VfxEntity> callback) {
        this.onEnd = callback;
        return this;
    }
    public VfxAnimationBuilder onLoop(Consumer<VfxEntity> callback) {
        this.onLoop = callback;
        return this;
    }
    public VfxAnimationBuilder loop(int count) {
        this.loopCount = count;
        return this;
    }
    public VfxAnimationBuilder loopInfinite() {
        this.loopCount = -1;
        return this;
    }
    public VfxAnimationBuilder onKeyframeReached(float time, Consumer<VfxEntity> callback) {
        this.keyframeCallbacks.put(time, callback);
        return this;
    }

    public VfxAnimationBuilder inheritTranslation() {
        this.inheritTranslation = true;
        return this;
    }
    public VfxAnimationBuilder inheritScale() {
        this.inheritScale = true;
        return this;
    }
    public VfxAnimationBuilder inheritRotation() {
        this.inheritRotation = true;
        return this;
    }
    public VfxAnimationBuilder inheritOverlayColor() {
        this.inheritOverlayColor = true;
        return this;
    }
    public VfxAnimationBuilder inheritOverlayIntensity() {
        this.inheritOverlayIntensity = true;
        return this;
    }
    public VfxAnimationBuilder inheritBlockState() {
        this.inheritBlockState = true;
        return this;
    }
    public VfxAnimationBuilder inheritItemStack() {
        this.inheritItemStack = true;
        return this;
    }
    public VfxAnimationBuilder inheritAll() {
        return inheritTranslation().inheritScale().inheritRotation().inheritOverlayColor().inheritOverlayIntensity().inheritBlockState().inheritItemStack();
    }

    public VfxAnimationBuilder onTickTranslation(VfxAnimation.Vector3fTickModifier modifier) {
        this.translationModifier = modifier;
        return this;
    }
    public VfxAnimationBuilder onTickScale(VfxAnimation.Vector3fTickModifier modifier) {
        this.scaleModifier = modifier;
        return this;
    }
    public VfxAnimationBuilder onTickRotation(VfxAnimation.QuaternionfTickModifier modifier) {
        this.rotationModifier = modifier;
        return this;
    }
    public VfxAnimationBuilder onTickOverlayColor(VfxAnimation.Vector3fTickModifier modifier) {
        this.overlayColorModifier = modifier;
        return this;
    }
    public VfxAnimationBuilder onTickOverlayIntensity(VfxAnimation.FloatTickModifier modifier) {
        this.overlayIntensityModifier = modifier;
        return this;
    }

    public VfxAnimationBuilder rotationPivot(Vec3 pivot) {
        this.rotationPivot = toVector3f(pivot);
        return this;
    }
    public VfxAnimationBuilder rotationPivot(float x, float y, float z) {
        this.rotationPivot = toVector3f(x, y, z);
        return this;
    }
    public VfxAnimationBuilder rotationPivot(float uniformPosition) {
        this.rotationPivot = new Vector3f(uniformPosition);
        return this;
    }

    public VfxAnimationBuilder translation(Vector3f start, Consumer<Vector3fBuilder> builderConsumer) {
        Vector3fBuilder builder = new Vector3fBuilder(start, kfs -> translationChannel = new InterpolatedChannel<>(kfs, Interpolators::lerpVector3f));
        builderConsumer.accept(builder);
        builder.end();
        return this;
    }
    public VfxAnimationBuilder translation(Vec3 start, Consumer<Vector3fBuilder> builderConsumer) {
        return translation(toVector3f(start), builderConsumer);
    }
    public VfxAnimationBuilder translation(float x, float y, float z, Consumer<Vector3fBuilder> builderConsumer) {
        return translation(toVector3f(x, y, z), builderConsumer);
    }
    public VfxAnimationBuilder translation(float uniformTranslation, Consumer<Vector3fBuilder> builderConsumer) {
        return translation(new Vector3f(uniformTranslation), builderConsumer);
    }
    public VfxAnimationBuilder translation(Consumer<Vector3fBuilder> builderConsumer) {
        return translation(new Vector3f(), builderConsumer);
    }

    public VfxAnimationBuilder scale(Vector3f start, Consumer<Vector3fBuilder> builderConsumer) {
        Vector3fBuilder builder = new Vector3fBuilder(start, kfs -> scaleChannel = new InterpolatedChannel<>(kfs, Interpolators::lerpVector3f));
        builderConsumer.accept(builder);
        builder.end();
        return this;
    }
    public VfxAnimationBuilder scale(Vec3 start, Consumer<Vector3fBuilder> builderConsumer) {
        return scale(toVector3f(start), builderConsumer);
    }
    public VfxAnimationBuilder scale(float x, float y, float z, Consumer<Vector3fBuilder> builderConsumer) {
        return scale(toVector3f(x, y, z), builderConsumer);
    }
    public VfxAnimationBuilder scale(float uniformScale, Consumer<Vector3fBuilder> builderConsumer) {
        return scale(toVector3f(uniformScale, uniformScale, uniformScale), builderConsumer);
    }
    public VfxAnimationBuilder scale(Consumer<Vector3fBuilder> builderConsumer) {
        return scale(new Vector3f(1f), builderConsumer);
    }

    public VfxAnimationBuilder rotation(Vector3f startDegrees, Consumer<Vector3fBuilder> builderConsumer) {
        Vector3fBuilder builder = new Vector3fBuilder(startDegrees, kfs -> rotationChannel = new InterpolatedChannel<>(kfs, Interpolators::lerpDegrees));
        builderConsumer.accept(builder);
        builder.end();
        return this;
    }
    public VfxAnimationBuilder rotation(Vec3 startDegrees, Consumer<Vector3fBuilder> builderConsumer) {
        return rotation(toVector3f(startDegrees), builderConsumer);
    }
    public VfxAnimationBuilder rotation(float pitch, float yaw, float roll, Consumer<Vector3fBuilder> builderConsumer) {
        return rotation(toVector3f(pitch, yaw, roll), builderConsumer);
    }
    public VfxAnimationBuilder rotation(float uniformRotation, Consumer<Vector3fBuilder> builderConsumer) {
        return rotation(new Vector3f(uniformRotation), builderConsumer);
    }
    public VfxAnimationBuilder rotation(Consumer<Vector3fBuilder> builderConsumer) {
        return rotation(new Vector3f(), builderConsumer);
    }

    public VfxAnimationBuilder overlay(Vector3f startColor, float startIntensity, Consumer<OverlayBuilder> builderConsumer) {
        OverlayBuilder builder = new OverlayBuilder(startColor, startIntensity);
        builderConsumer.accept(builder);
        builder.end();
        return this;
    }
    public VfxAnimationBuilder overlay(Vec3 startColor, float startIntensity, Consumer<OverlayBuilder> builderConsumer) {
        return overlay(toVector3f(startColor), startIntensity, builderConsumer);
    }
    public VfxAnimationBuilder overlay(float r, float g, float b, float startIntensity, Consumer<OverlayBuilder> builderConsumer) {
        return overlay(toVector3f(r, g, b), startIntensity, builderConsumer);
    }
    public VfxAnimationBuilder overlay(float uniformColor, float startIntensity, Consumer<OverlayBuilder> builderConsumer) {
        return overlay(new Vector3f(uniformColor), startIntensity, builderConsumer);
    }
    public VfxAnimationBuilder overlay(Consumer<OverlayBuilder> builderConsumer) {
        return overlay(new Vector3f(1f), 0.5f, builderConsumer);
    }

    public VfxAnimationBuilder blockState(BlockState initial, Consumer<BlockStateBuilder> builderConsumer) {
        BlockStateBuilder builder = new BlockStateBuilder(initial);
        builderConsumer.accept(builder);
        builder.end();
        return this;
    }

    public VfxAnimationBuilder itemStack(ItemStack initial, Consumer<ItemStackBuilder> builderConsumer) {
        ItemStackBuilder builder = new ItemStackBuilder(initial);
        builderConsumer.accept(builder);
        builder.end();
        return this;
    }

    public VfxAnimation build(int durationTicks) {
        if (translationChannel == null) translationChannel = DEFAULT_TRANSLATION;
        if (scaleChannel == null) scaleChannel = DEFAULT_SCALE;
        if (rotationChannel == null) rotationChannel = DEFAULT_ROTATION;
        if (overlayColorChannel == null) overlayColorChannel = DEFAULT_OVERLAY_COLOR;
        if (overlayIntensityChannel == null) overlayIntensityChannel = DEFAULT_OVERLAY_INTENSITY;
        if (blockStateChannel == null) blockStateChannel = DEFAULT_BLOCK_STATE;
        if (itemStackChannel == null) itemStackChannel = DEFAULT_ITEM_STACK;
        List<KeyframeCallbackEntry> sortedKeyframeCallbacks = keyframeCallbacks.entrySet().stream()
                .map(entry -> new KeyframeCallbackEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingDouble(KeyframeCallbackEntry::time))
                .toList();
        return new VfxAnimation(
                translationChannel, scaleChannel, rotationChannel, overlayColorChannel, overlayIntensityChannel, blockStateChannel, itemStackChannel,
                inheritTranslation, inheritScale, inheritRotation, inheritOverlayColor, inheritOverlayIntensity, inheritBlockState, inheritItemStack,
                translationModifier, scaleModifier, rotationModifier, overlayColorModifier, overlayIntensityModifier,
                rotationPivot, durationTicks, loopCount, onStart, onEnd, onLoop, sortedKeyframeCallbacks
        );
    }

    public static class Vector3fBuilder {
        private final List<Keyframe<Vector3f>> keyframes = new ArrayList<>();
        private final Consumer<List<Keyframe<Vector3f>>> channelCreator;

        private Vector3fBuilder(Vector3f start, Consumer<List<Keyframe<Vector3f>>> channelCreator) {
            this.keyframes.add(new Keyframe<>(0f, start, Easings.LINEAR.get()));
            this.channelCreator = channelCreator;
        }

        public Vector3fBuilder addKeyframe(float time, Vector3f value, Supplier<Easing> easing) {
            keyframes.add(new Keyframe<>(time, value, easing.get()));
            return this;
        }
        public Vector3fBuilder addKeyframe(float time, Vector3f value) {
            return addKeyframe(time, value, Easings.LINEAR);
        }

        public Vector3fBuilder addKeyframe(float time, Vec3 value, Supplier<Easing> easing) {
            return addKeyframe(time, toVector3f(value), easing);
        }
        public Vector3fBuilder addKeyframe(float time, Vec3 value) {
            return addKeyframe(time, toVector3f(value), Easings.LINEAR);
        }

        public Vector3fBuilder addKeyframe(float time, float x, float y, float z, Supplier<Easing> easing) {
            return addKeyframe(time, toVector3f(x, y, z), easing);
        }
        public Vector3fBuilder addKeyframe(float time, float x, float y, float z) {
            return addKeyframe(time, toVector3f(x, y, z), Easings.LINEAR);
        }

        public Vector3fBuilder addKeyframe(float time, float uniformValue, Supplier<Easing> easing) {
            return addKeyframe(time, new Vector3f(uniformValue), easing);
        }
        public Vector3fBuilder addKeyframe(float time, float uniformValue) {
            return addKeyframe(time, new Vector3f(uniformValue), Easings.LINEAR);
        }

        public Vector3fBuilder addRandomKeyframe(float time, Vector3f min, Vector3f max, Supplier<Easing> easing) {
            return addKeyframe(time,
                    min.x + (float)(Math.random() * (max.x - min.x)),
                    min.y + (float)(Math.random() * (max.y - min.y)),
                    min.z + (float)(Math.random() * (max.z - min.z)),
                    easing
            );
        }
        public Vector3fBuilder addRandomKeyframe(float time, Vector3f min, Vector3f max) {
            return addRandomKeyframe(time, min, max, Easings.LINEAR);
        }

        public Vector3fBuilder addRandomKeyframe(float time, float min, float max, Supplier<Easing> easing) {
            float val = min + (float)(Math.random() * (max - min));
            return addKeyframe(time, new Vector3f(val), easing);
        }
        public Vector3fBuilder addRandomKeyframe(float time, float min, float max) {
            return addRandomKeyframe(time, min, max, Easings.LINEAR);
        }

        public Vector3fBuilder addRandomKeyframe(float time, float minX, float maxX, float minY, float maxY, float minZ, float maxZ, Supplier<Easing> easing) {
            return addKeyframe(time, new Vector3f(
                    minX + (float)(Math.random() * (maxX - minX)),
                    minY + (float)(Math.random() * (maxY - minY)),
                    minZ + (float)(Math.random() * (maxZ - minZ))
            ), easing);
        }
        public Vector3fBuilder addRandomKeyframe(float time, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
            return addRandomKeyframe(time, minX, maxX, minY, maxY, minZ, maxZ, Easings.LINEAR);
        }

        public Vector3fBuilder holdKeyframe(float time) {
            keyframes.add(new Keyframe<>(time, keyframes.getLast().value(), keyframes.getLast().easing()));
            return this;
        }

        private void end(Vector3f value, Supplier<Easing> easing) {
            keyframes.add(new Keyframe<>(1f, value, easing.get()));
            channelCreator.accept(keyframes);
        }

        private void end() {
            end(keyframes.getLast().value(), Easings.LINEAR);
        }
    }

    public class OverlayBuilder {
        private final List<Keyframe<Vector3f>> colorKeyframes = new ArrayList<>();
        private final List<Keyframe<Float>> intensityKeyframes = new ArrayList<>();

        private OverlayBuilder(Vector3f startColor, float startIntensity) {
            colorKeyframes.add(new Keyframe<>(0f, startColor, Easings.LINEAR.get()));
            intensityKeyframes.add(new Keyframe<>(0f, startIntensity, Easings.LINEAR.get()));
        }

        public OverlayBuilder addColorKeyframe(float time, Vector3f color, Supplier<Easing> easing) {
            colorKeyframes.add(new Keyframe<>(time, color, easing.get()));
            return this;
        }
        public OverlayBuilder addColorKeyframe(float time, Vector3f color) {
            return addColorKeyframe(time, color, Easings.LINEAR);
        }

        public OverlayBuilder addColorKeyframe(float time, Vec3 color, Supplier<Easing> easing) {
            return addColorKeyframe(time, toVector3f(color), easing);
        }
        public OverlayBuilder addColorKeyframe(float time, Vec3 color) {
            return addColorKeyframe(time, toVector3f(color), Easings.LINEAR);
        }

        public OverlayBuilder addColorKeyframe(float time, float r, float g, float b, Supplier<Easing> easing) {
            return addColorKeyframe(time, toVector3f(r, g, b), easing);
        }
        public OverlayBuilder addColorKeyframe(float time, float r, float g, float b) {
            return addColorKeyframe(time, toVector3f(r, g, b), Easings.LINEAR);
        }

        public OverlayBuilder addColorKeyframe(float time, float uniformColor, Supplier<Easing> easing) {
            return addColorKeyframe(time, new Vector3f(uniformColor), easing);
        }
        public OverlayBuilder addColorKeyframe(float time, float uniformColor) {
            return addColorKeyframe(time, new Vector3f(uniformColor), Easings.LINEAR);
        }

        public OverlayBuilder holdColorKeyframe(float time) {
            colorKeyframes.add(new Keyframe<>(time, colorKeyframes.getLast().value(), colorKeyframes.getLast().easing()));
            return this;
        }

        public OverlayBuilder addIntensityKeyframe(float time, float intensity, Supplier<Easing> easing) {
            intensityKeyframes.add(new Keyframe<>(time, intensity, easing.get()));
            return this;
        }
        public OverlayBuilder addIntensityKeyframe(float time, float intensity) {
            return addIntensityKeyframe(time, intensity, Easings.LINEAR);
        }

        public OverlayBuilder holdIntensityKeyframe(float time) {
            intensityKeyframes.add(new Keyframe<>(time, intensityKeyframes.getLast().value(), intensityKeyframes.getLast().easing()));
            return this;
        }

        private void end() {
            colorKeyframes.add(new Keyframe<>(1f, colorKeyframes.getLast().value(), Easings.LINEAR.get()));
            intensityKeyframes.add(new Keyframe<>(1f, intensityKeyframes.getLast().value(), Easings.LINEAR.get()));
            overlayColorChannel = new InterpolatedChannel<>(colorKeyframes, Interpolators::lerpVector3f);
            overlayIntensityChannel = new InterpolatedChannel<>(intensityKeyframes, Interpolators::lerpFloat);
        }
    }

    private abstract static class DiscreteBuilder<T, B extends DiscreteBuilder<T, B>> {
        protected final List<Keyframe<T>> keyframes = new ArrayList<>();

        private DiscreteBuilder(T initial) {
            keyframes.add(new Keyframe<>(0f, initial, Easings.LINEAR.get()));
        }

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        public B addKeyframe(float time, T state) {
            keyframes.add(new Keyframe<>(time, state, Easings.LINEAR.get()));
            return self();
        }

        public B holdKeyframe(float time) {
            keyframes.add(new Keyframe<>(time, keyframes.getLast().value(), keyframes.getLast().easing()));
            return self();
        }

        void end() {
            keyframes.add(new Keyframe<>(1f, keyframes.getLast().value(), Easings.LINEAR.get()));
        }
    }

    public class BlockStateBuilder extends DiscreteBuilder<BlockState, BlockStateBuilder> {
        private BlockStateBuilder(BlockState initial) {
            super(initial);
        }

        @Override
        void end() {
            super.end();
            blockStateChannel = new DiscreteChannel<>(keyframes);
        }
    }

    public class ItemStackBuilder extends DiscreteBuilder<ItemStack, ItemStackBuilder> {
        private ItemStackBuilder(ItemStack initial) {
            super(initial);
        }

        @Override
        void end() {
            super.end();
            itemStackChannel = new DiscreteChannel<>(keyframes);
        }
    }
}