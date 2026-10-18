package com.kipti.bnb.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * Stand-in for Fabric API's {@code LevelRenderContext}, with the accessors Bits 'n' Bobs' renderers use. Built from
 * NeoForge's {@link SubmitCustomGeometryEvent}, whose pose stack is camera-relative like Fabric's.
 */
public record LevelRenderContext(LevelRenderState levelState, SubmitNodeCollector submitNodeCollector, PoseStack poseStack) {
    public static LevelRenderContext of(final SubmitCustomGeometryEvent event) {
        return new LevelRenderContext(event.getLevelRenderState(), event.getSubmitNodeCollector(), event.getPoseStack());
    }
}
