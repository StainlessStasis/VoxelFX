package io.github.stainlessstasis.bdanimator.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import io.github.stainlessstasis.bdanimator.animation.VfxAnimation;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.NonNull;

public class VfxEntityRenderer extends EntityRenderer<VfxEntity, VfxEntityRenderState> {
    public static final float PI_180 = (float) (Math.PI/180f);
    protected final BlockModelResolver blockModelResolver;

    public VfxEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
    }

    @Override
    public @NonNull VfxEntityRenderState createRenderState() {
        return new VfxEntityRenderState();
    }

    @Override
    public void extractRenderState(@NonNull VfxEntity entity, @NonNull VfxEntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);

        VfxAnimation anim = entity.getCurrentAnimation();
        if (anim == null) return;

        float t = entity.getAnimationProgress(partialTicks);
        var context = new VfxAnimation.AnimationContext(entity, entity.tickCount+partialTicks, partialTicks);
        var snapshot = entity.getLastSnapshot();

        anim.translationChannel().evaluate(t, state.translation, anim.inheritTranslation() ? snapshot.translation() : null);
        if (anim.translationModifier() != null) {
            anim.translationModifier().apply(state.translation, context);
        }
        anim.scaleChannel().evaluate(t, state.scale, anim.inheritScale() ? snapshot.scale() : null);
        if (anim.scaleModifier() != null) {
            anim.scaleModifier().apply(state.scale, context);
        }
        anim.rotationChannel().evaluate(t, state.rotation, anim.inheritRotation() ? snapshot.rotation() : null);
        if (anim.rotationModifier() != null) {
            anim.rotationModifier().apply(state.rotation, context);
        }
        anim.overlayColorChannel().evaluate(t, state.overlayColor, anim.inheritOverlayColor() ? snapshot.overlayColor() : null);
        if (anim.overlayColorModifier() != null) {
            anim.overlayColorModifier().apply(state.overlayColor, context);
        }
        anim.overlayIntensityChannel().evaluate(t, state.overlayIntensity, anim.inheritOverlayIntensity() ? snapshot.overlayIntensity() : null);
        if (anim.overlayIntensityModifier() != null) {
            anim.overlayIntensityModifier().apply(state.overlayIntensity, context);
        }

        state.blockState = anim.blockStateChannel().evaluate(t, anim.inheritBlockState() ? snapshot.blockState() : null);
        entity.updateBakedModel(state.blockState, this.blockModelResolver);
        state.blockModel = entity.getCachedModel();

        state.brightnessOverride = entity.getBrightnessOverride();
        state.rotationPivot = anim.rotationPivot();

        state.billboardMode = entity.getBillboardMode();
        state.entityYRot = entity.getYRot(partialTicks);
        state.entityXRot = entity.getXRot(partialTicks);

        Camera camera = this.entityRenderDispatcher.camera;
        if (camera != null) {
            state.cameraXRot = camera.xRot();
            state.cameraYRot = camera.yRot();
        }
    }

    @Override
    public void submit(@NonNull VfxEntityRenderState state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector collector, @NonNull CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        poseStack.pushPose();

        poseStack.mulPose(calculateOrientation(state, new Quaternionf()));

        poseStack.translate(state.translation.x, state.translation.y, state.translation.z);
        poseStack.mulPose(state.rotation);
        poseStack.scale(state.scale.x, state.scale.y, state.scale.z);
        poseStack.translate(-state.rotationPivot.x, -state.rotationPivot.y, -state.rotationPivot.z);

        int light = state.brightnessOverride != -1 ? state.brightnessOverride : state.lightCoords;
        if (state.blockModel != null) {
            state.blockModel.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }

        applyOverlayColor(state, poseStack, collector);

        poseStack.popPose();
    }

    private Quaternionf calculateOrientation(VfxEntityRenderState state, Quaternionf output) {
        return switch (state.billboardMode) {
            case FIXED -> output.rotationYXZ(-PI_180 * state.entityYRot, PI_180 * state.entityXRot, 0f);
            case HORIZONTAL -> output.rotationYXZ(-PI_180 * state.entityYRot, PI_180 * transformXRot(state.cameraXRot), 0f);
            case VERTICAL -> output.rotationYXZ(-PI_180 * transformYRot(state.cameraYRot), PI_180 * state.entityXRot, 0f);
            case CENTER -> output.rotationYXZ(-PI_180 * transformYRot(state.cameraYRot), PI_180 * transformXRot(state.cameraXRot), 0f);
        };
    }

    private static float transformYRot(float cameraYRot) {
        return cameraYRot - 180.0F;
    }

    private static float transformXRot(float cameraXRot) {
        return -cameraXRot;
    }

    private void applyOverlayColor(VfxEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.overlayIntensity[0] <= 0.01f) return;

        VoxelShape shape = state.blockState.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (shape.isEmpty()) return;

        int r = (int)(state.overlayColor.x * 255);
        int g = (int)(state.overlayColor.y * 255);
        int b = (int)(state.overlayColor.z * 255);
        int a = (int)(state.overlayIntensity[0] * 255);

        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float x1 = (float) minX;
            float y1 = (float) minY;
            float z1 = (float) minZ;
            float x2 = (float) maxX;
            float y2 = (float) maxY;
            float z2 = (float) maxZ;

            collector.submitCustomGeometry(poseStack, RenderTypes.debugFilledBox(), (pose, buffer) -> {
                Matrix4f mat = pose.pose();

                // Bottom
                buffer.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
                buffer.addVertex(mat, x2, y1, z1).setColor(r, g, b, a);
                buffer.addVertex(mat, x2, y1, z2).setColor(r, g, b, a);
                buffer.addVertex(mat, x1, y1, z2).setColor(r, g, b, a);
                // Top
                buffer.addVertex(mat, x1, y2, z2).setColor(r, g, b, a);
                buffer.addVertex(mat, x2, y2, z2).setColor(r, g, b, a);
                buffer.addVertex(mat, x2, y2, z1).setColor(r, g, b, a);
                buffer.addVertex(mat, x1, y2, z1).setColor(r, g, b, a);
                // North
                buffer.addVertex(mat, x2, y1, z1).setColor(r, g, b, a);
                buffer.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
                buffer.addVertex(mat, x1, y2, z1).setColor(r, g, b, a);
                buffer.addVertex(mat, x2, y2, z1).setColor(r, g, b, a);
                // South
                buffer.addVertex(mat, x1, y1, z2).setColor(r, g, b, a);
                buffer.addVertex(mat, x2, y1, z2).setColor(r, g, b, a);
                buffer.addVertex(mat, x2, y2, z2).setColor(r, g, b, a);
                buffer.addVertex(mat, x1, y2, z2).setColor(r, g, b, a);
                // West
                buffer.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
                buffer.addVertex(mat, x1, y1, z2).setColor(r, g, b, a);
                buffer.addVertex(mat, x1, y2, z2).setColor(r, g, b, a);
                buffer.addVertex(mat, x1, y2, z1).setColor(r, g, b, a);
                // East
                buffer.addVertex(mat, x2, y1, z2).setColor(r, g, b, a);
                buffer.addVertex(mat, x2, y1, z1).setColor(r, g, b, a);
                buffer.addVertex(mat, x2, y2, z1).setColor(r, g, b, a);
                buffer.addVertex(mat, x2, y2, z2).setColor(r, g, b, a);
            });
        });
    }

    @Override
    protected boolean affectedByCulling(@NonNull VfxEntity entity) {
        return entity.isAffectedByCulling();
    }

    @Override
    protected @NonNull AABB getBoundingBoxForCulling(@NonNull VfxEntity entity) {
        return entity.getBoundingBox().inflate(entity.getCullingRadius());
    }

    @Override
    protected boolean shouldShowName(@NonNull VfxEntity entity, double distanceToCameraSq) {
        return false;
    }

    @Override
    protected float getShadowRadius(@NonNull VfxEntityRenderState state) {
        return 0f;
    }

    @Override
    protected float getShadowStrength(@NonNull VfxEntityRenderState state) {
        return 0f;
    }
}