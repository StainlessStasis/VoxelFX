package io.github.stainlessstasis.voxelfx.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.*;

@EventBusSubscriber
public class VfxEntityCache {
    public static final VfxEntityCache INSTANCE = new VfxEntityCache();
    private final List<VfxEntity> active = new ArrayList<>();

    private VfxEntityCache() {}

    public void add(VfxEntity entity) {
        active.add(entity);
    }

    public void remove(VfxEntity entity) {
        active.remove(entity);
    }

    public void clear() {
        active.clear();
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Pre event) {
        Iterator<VfxEntity> it = INSTANCE.active.iterator();
        while (it.hasNext()) {
            VfxEntity entity = it.next();
            entity.tick();
            if (entity.isRemoved()) {
                it.remove();
            }
        }
    }

    @SubscribeEvent
    static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        if (INSTANCE.active.isEmpty()) return;

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        LevelRenderState levelRenderState = event.getLevelRenderState();
        PoseStack poseStack = event.getPoseStack();
        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        CameraRenderState cameraState = levelRenderState.cameraRenderState;
        Vec3 camPos = cameraState.pos;

        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);

        for (VfxEntity entity : INSTANCE.active) {
            VfxEntityRenderer renderer = (VfxEntityRenderer) dispatcher.getRenderer(entity);

            VfxEntityRenderState state = renderer.createRenderState();
            renderer.extractRenderState(entity, state, partialTick);

            poseStack.pushPose();
            poseStack.translate(state.x - camPos.x, state.y - camPos.y, state.z - camPos.z);
            renderer.submit(state, poseStack, collector, cameraState);
            poseStack.popPose();
        }
    }
}