package com.kipti.bnb.content.kinetics.cogwheel_chain.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Hosts the cogwheel chain on 26.2. The block entities that carry the chain behaviour belong to Create, and
 * both the renderer and the visualizer registries hold one entry per block entity type, so the chain cannot be
 * drawn by a renderer of its own without replacing Create's. It is drawn from the level's own extraction and
 * submit passes instead, which Flywheel does not take part in, so one path serves both backends.
 * <p>
 * NeoForge: Fabric's END_EXTRACTION / COLLECT_SUBMITS are NeoForge's ExtractLevelRenderStateEvent /
 * SubmitCustomGeometryEvent; a client level change is a client LevelEvent.Load.
 */
public final class CogwheelChainLevelRenderer {

    private CogwheelChainLevelRenderer() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ExtractLevelRenderStateEvent.class, event -> {
            final Vec3 cameraPos = event.getCamera().position();
            if (cameraPos == null)
                return;
            CogwheelChainRenderStates.extract(event.getLevel(), cameraPos);
        });

        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent.class, event -> {
            final Vec3 cameraPos = event.getLevelRenderState().cameraRenderState.pos;
            if (cameraPos == null)
                return;
            CogwheelChainRenderStates.submit(event.getPoseStack(), event.getSubmitNodeCollector(), cameraPos);
        });

        NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, event -> {
            if (event.getLevel() instanceof ClientLevel)
                CogwheelChainRenderStates.clear();
        });
    }
}
