package io.github.stainlessstasis.voxelfx.animation;

import io.github.stainlessstasis.voxelfx.channel.DiscreteChannel;
import io.github.stainlessstasis.voxelfx.channel.Interpolators;
import io.github.stainlessstasis.voxelfx.channel.Keyframe;
import io.github.stainlessstasis.voxelfx.channel.InterpolatedChannel;
import io.github.stainlessstasis.voxelfx.entity.VfxEntity;
import io.github.stainlessstasis.voxelfx.util.VfxUtils;
import net.minecraft.util.EasingType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.function.Consumer;

public class VfxAnimationBuilder {
    public static final InterpolatedChannel<Vector3f, Vector3f> DEFAULT_TRANSLATION = new InterpolatedChannel<>(
            List.of(new Keyframe<>(0f, new Vector3f(0f), EasingType.LINEAR),
                    new Keyframe<>(1f, new Vector3f(0f), EasingType.LINEAR)),
            Interpolators::lerpVector3f
    );
    public static final InterpolatedChannel<Vector3f, Vector3f> DEFAULT_SCALE = new InterpolatedChannel<>(
            List.of(new Keyframe<>(0f, new Vector3f(1f), EasingType.LINEAR),
                    new Keyframe<>(1f, new Vector3f(1f), EasingType.LINEAR)),
            Interpolators::lerpVector3f
    );
    public static final InterpolatedChannel<Vector3f, Quaternionf> DEFAULT_ROTATION = new InterpolatedChannel<>(
            List.of(new Keyframe<>(0f, new Vector3f(0f), EasingType.LINEAR),
                    new Keyframe<>(1f, new Vector3f(0f), EasingType.LINEAR)),
            Interpolators::lerpDegrees
    );
    public static final InterpolatedChannel<Vector3f, Vector3f> DEFAULT_OVERLAY_COLOR = new InterpolatedChannel<>(
            List.of(new Keyframe<>(0f, new Vector3f(1f), EasingType.LINEAR),
                    new Keyframe<>(1f, new Vector3f(1f), EasingType.LINEAR)),
            Interpolators::lerpVector3f
    );
    public static final InterpolatedChannel<Float, float[]> DEFAULT_OVERLAY_INTENSITY = new InterpolatedChannel<>(
            List.of(new Keyframe<>(0f, 0f, EasingType.LINEAR),
                    new Keyframe<>(1f, 0f, EasingType.LINEAR)),
            Interpolators::lerpFloat
    );
    public static final DiscreteChannel<BlockState> DEFAULT_BLOCK_STATE = new DiscreteChannel<>(
            List.of(new Keyframe<>(0f, Blocks.AIR.defaultBlockState(), EasingType.LINEAR))
    );
    public static final DiscreteChannel<ItemStack> DEFAULT_ITEM_STACK = new DiscreteChannel<>(
            List.of(new Keyframe<>(0f, ItemStack.EMPTY, EasingType.LINEAR))
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
    private boolean translationDeclared = false;
    private boolean scaleDeclared = false;
    private boolean rotationDeclared = false;
    private boolean overlayDeclared = false;
    private boolean blockStateDeclared = false;
    private boolean itemStackDeclared = false;
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

    public VfxAnimationBuilder onFrameTranslation(VfxAnimation.Vector3fTickModifier modifier) {
        this.translationModifier = modifier;
        return this;
    }
    public VfxAnimationBuilder onFrameScale(VfxAnimation.Vector3fTickModifier modifier) {
        this.scaleModifier = modifier;
        return this;
    }
    public VfxAnimationBuilder onFrameRotation(VfxAnimation.QuaternionfTickModifier modifier) {
        this.rotationModifier = modifier;
        return this;
    }
    public VfxAnimationBuilder onFrameOverlayColor(VfxAnimation.Vector3fTickModifier modifier) {
        this.overlayColorModifier = modifier;
        return this;
    }
    public VfxAnimationBuilder onFrameOverlayIntensity(VfxAnimation.FloatTickModifier modifier) {
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
        translationDeclared = true;
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
        scaleDeclared = true;
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
        rotationDeclared = true;
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
        overlayDeclared = true;
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
        blockStateDeclared = true;
        BlockStateBuilder builder = new BlockStateBuilder(initial);
        builderConsumer.accept(builder);
        builder.end();
        return this;
    }

    public VfxAnimationBuilder itemStack(ItemStack initial, Consumer<ItemStackBuilder> builderConsumer) {
        itemStackDeclared = true;
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
                translationDeclared, scaleDeclared, rotationDeclared, overlayDeclared, blockStateDeclared, itemStackDeclared,
                inheritTranslation, inheritScale, inheritRotation, inheritOverlayColor, inheritOverlayIntensity, inheritBlockState, inheritItemStack,
                translationModifier, scaleModifier, rotationModifier, overlayColorModifier, overlayIntensityModifier,
                rotationPivot, durationTicks, loopCount, onStart, onEnd, onLoop, sortedKeyframeCallbacks
        );
    }

    public static class Vector3fBuilder {
        private final List<Keyframe<Vector3f>> keyframes = new ArrayList<>();
        private final Consumer<List<Keyframe<Vector3f>>> channelCreator;

        private Vector3fBuilder(Vector3f start, Consumer<List<Keyframe<Vector3f>>> channelCreator) {
            this.keyframes.add(new Keyframe<>(0f, start, EasingType.LINEAR));
            this.channelCreator = channelCreator;
        }

        public Vector3fBuilder addKeyframe(float time, Vector3f value, EasingType easing) {
            keyframes.add(new Keyframe<>(time, value, easing));
            return this;
        }
        public Vector3fBuilder addKeyframe(float time, Vector3f value) {
            return addKeyframe(time, value, EasingType.LINEAR);
        }

        public Vector3fBuilder addKeyframe(float time, Vec3 value, EasingType easing) {
            return addKeyframe(time, toVector3f(value), easing);
        }
        public Vector3fBuilder addKeyframe(float time, Vec3 value) {
            return addKeyframe(time, toVector3f(value), EasingType.LINEAR);
        }

        public Vector3fBuilder addKeyframe(float time, float x, float y, float z, EasingType easing) {
            return addKeyframe(time, toVector3f(x, y, z), easing);
        }
        public Vector3fBuilder addKeyframe(float time, float x, float y, float z) {
            return addKeyframe(time, toVector3f(x, y, z), EasingType.LINEAR);
        }

        public Vector3fBuilder addKeyframe(float time, float uniformValue, EasingType easing) {
            return addKeyframe(time, new Vector3f(uniformValue), easing);
        }
        public Vector3fBuilder addKeyframe(float time, float uniformValue) {
            return addKeyframe(time, new Vector3f(uniformValue), EasingType.LINEAR);
        }

        public Vector3fBuilder addRandomKeyframe(float time, Vector3f min, Vector3f max, EasingType easing) {
            return addKeyframe(time,
                    min.x + (float)(Math.random() * (max.x - min.x)),
                    min.y + (float)(Math.random() * (max.y - min.y)),
                    min.z + (float)(Math.random() * (max.z - min.z)),
                    easing
            );
        }
        public Vector3fBuilder addRandomKeyframe(float time, Vector3f min, Vector3f max) {
            return addRandomKeyframe(time, min, max, EasingType.LINEAR);
        }

        public Vector3fBuilder addRandomKeyframe(float time, float min, float max, EasingType easing) {
            float val = min + (float)(Math.random() * (max - min));
            return addKeyframe(time, new Vector3f(val), easing);
        }
        public Vector3fBuilder addRandomKeyframe(float time, float min, float max) {
            return addRandomKeyframe(time, min, max, EasingType.LINEAR);
        }

        public Vector3fBuilder addRandomKeyframe(float time, float minX, float maxX, float minY, float maxY, float minZ, float maxZ, EasingType easing) {
            return addKeyframe(time, new Vector3f(
                    minX + (float)(Math.random() * (maxX - minX)),
                    minY + (float)(Math.random() * (maxY - minY)),
                    minZ + (float)(Math.random() * (maxZ - minZ))
            ), easing);
        }
        public Vector3fBuilder addRandomKeyframe(float time, float minX, float maxX, float minY, float maxY, float minZ, float maxZ) {
            return addRandomKeyframe(time, minX, maxX, minY, maxY, minZ, maxZ, EasingType.LINEAR);
        }

        public Vector3fBuilder addRandomDeltaKeyframe(float time, float deltaMin, float deltaMax, EasingType easing) {
            Vector3f base = keyframes.getLast().value();
            return addKeyframe(time, new Vector3f(
                    base.x + VfxUtils.randomBetween(deltaMin, deltaMax),
                    base.y + VfxUtils.randomBetween(deltaMin, deltaMax),
                    base.z + VfxUtils.randomBetween(deltaMin, deltaMax)
            ), easing);
        }
        public Vector3fBuilder addRandomDeltaKeyframe(float time, float deltaMin, float deltaMax) {
            return addRandomDeltaKeyframe(time, deltaMin, deltaMax, EasingType.LINEAR);
        }

        public Vector3fBuilder holdKeyframe(float time) {
            keyframes.add(new Keyframe<>(time, keyframes.getLast().value(), keyframes.getLast().easing()));
            return this;
        }

        private void end(Vector3f value, EasingType easing) {
            keyframes.add(new Keyframe<>(1f, value, easing));
            channelCreator.accept(keyframes);
        }

        private void end() {
            end(keyframes.getLast().value(), EasingType.LINEAR);
        }
    }

    public class OverlayBuilder {
        private final List<Keyframe<Vector3f>> colorKeyframes = new ArrayList<>();
        private final List<Keyframe<Float>> intensityKeyframes = new ArrayList<>();

        private OverlayBuilder(Vector3f startColor, float startIntensity) {
            colorKeyframes.add(new Keyframe<>(0f, startColor, EasingType.LINEAR));
            intensityKeyframes.add(new Keyframe<>(0f, startIntensity, EasingType.LINEAR));
        }

        public OverlayBuilder addColorKeyframe(float time, Vector3f color, EasingType easing) {
            colorKeyframes.add(new Keyframe<>(time, color, easing));
            return this;
        }
        public OverlayBuilder addColorKeyframe(float time, Vector3f color) {
            return addColorKeyframe(time, color, EasingType.LINEAR);
        }

        public OverlayBuilder addColorKeyframe(float time, Vec3 color, EasingType easing) {
            return addColorKeyframe(time, toVector3f(color), easing);
        }
        public OverlayBuilder addColorKeyframe(float time, Vec3 color) {
            return addColorKeyframe(time, toVector3f(color), EasingType.LINEAR);
        }

        public OverlayBuilder addColorKeyframe(float time, float r, float g, float b, EasingType easing) {
            return addColorKeyframe(time, toVector3f(r, g, b), easing);
        }
        public OverlayBuilder addColorKeyframe(float time, float r, float g, float b) {
            return addColorKeyframe(time, toVector3f(r, g, b), EasingType.LINEAR);
        }

        public OverlayBuilder addColorKeyframe(float time, float uniformColor, EasingType easing) {
            return addColorKeyframe(time, new Vector3f(uniformColor), easing);
        }
        public OverlayBuilder addColorKeyframe(float time, float uniformColor) {
            return addColorKeyframe(time, new Vector3f(uniformColor), EasingType.LINEAR);
        }

        public OverlayBuilder holdColorKeyframe(float time) {
            colorKeyframes.add(new Keyframe<>(time, colorKeyframes.getLast().value(), colorKeyframes.getLast().easing()));
            return this;
        }

        public OverlayBuilder addIntensityKeyframe(float time, float intensity, EasingType easing) {
            intensityKeyframes.add(new Keyframe<>(time, intensity, easing));
            return this;
        }
        public OverlayBuilder addIntensityKeyframe(float time, float intensity) {
            return addIntensityKeyframe(time, intensity, EasingType.LINEAR);
        }

        public OverlayBuilder holdIntensityKeyframe(float time) {
            intensityKeyframes.add(new Keyframe<>(time, intensityKeyframes.getLast().value(), intensityKeyframes.getLast().easing()));
            return this;
        }

        public OverlayBuilder addRandomColorKeyframe(float time, Vector3f min, Vector3f max, EasingType easing) {
            return addColorKeyframe(time, new Vector3f(
                    VfxUtils.randomBetween(min.x, max.x),
                    VfxUtils.randomBetween(min.y, max.y),
                    VfxUtils.randomBetween(min.z, max.z)
            ), easing);
        }
        public OverlayBuilder addRandomColorKeyframe(float time, Vector3f min, Vector3f max) {
            return addRandomColorKeyframe(time, min, max, EasingType.LINEAR);
        }

        public OverlayBuilder addRandomIntensityKeyframe(float time, float min, float max, EasingType easing) {
            return addIntensityKeyframe(time, VfxUtils.randomBetween(min, max), easing);
        }
        public OverlayBuilder addRandomIntensityKeyframe(float time, float min, float max) {
            return addRandomIntensityKeyframe(time, min, max, EasingType.LINEAR);
        }

        private void end() {
            colorKeyframes.add(new Keyframe<>(1f, colorKeyframes.getLast().value(), EasingType.LINEAR));
            intensityKeyframes.add(new Keyframe<>(1f, intensityKeyframes.getLast().value(), EasingType.LINEAR));
            overlayColorChannel = new InterpolatedChannel<>(colorKeyframes, Interpolators::lerpVector3f);
            overlayIntensityChannel = new InterpolatedChannel<>(intensityKeyframes, Interpolators::lerpFloat);
        }
    }

    private abstract static class DiscreteBuilder<T, B extends DiscreteBuilder<T, B>> {
        protected final List<Keyframe<T>> keyframes = new ArrayList<>();

        private DiscreteBuilder(T initial) {
            keyframes.add(new Keyframe<>(0f, initial, EasingType.LINEAR));
        }

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        public B addKeyframe(float time, T state) {
            keyframes.add(new Keyframe<>(time, state, EasingType.LINEAR));
            return self();
        }

        public final B addRandomKeyframe(float time, T... options) {
            return addKeyframe(time, VfxUtils.randomOf(options));
        }

        public B holdKeyframe(float time) {
            keyframes.add(new Keyframe<>(time, keyframes.getLast().value(), keyframes.getLast().easing()));
            return self();
        }

        void end() {
            keyframes.add(new Keyframe<>(1f, keyframes.getLast().value(), EasingType.LINEAR));
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