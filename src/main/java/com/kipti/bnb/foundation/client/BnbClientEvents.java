package com.kipti.bnb.foundation.client;

import com.kipti.bnb.content.decoration.weathered_girder.WeatheredGirderWrenchBehaviour;
import com.kipti.bnb.content.kinetics.cogwheel_chain.edit.CogwheelChainPartialEditDisplayHandler;
import com.kipti.bnb.content.kinetics.cogwheel_chain.edit.CogwheelChainPartialEditInteractionHandler;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainCandidate;
import com.kipti.bnb.content.kinetics.cogwheel_chain.placement.CogwheelChainPlacementEffect;
import com.kipti.bnb.content.kinetics.cogwheel_chain.placement.CogwheelChainPlacementInteraction;
import com.kipti.bnb.content.kinetics.cogwheel_chain.riding.CogwheelChainRidingHelper;
import com.kipti.bnb.content.kinetics.cogwheel_chain.riding.PlayerSkyhookRendererBridge;
import com.kipti.bnb.content.kinetics.cogwheel_chain.shape.CogwheelChainInteractionHandler;
import com.kipti.bnb.neoforge.client.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The client half of the interactions upstream drove from NeoForge's client event bus.
 * <p>
 * NeoForge: the Fabric port's listeners are mapped back to the NeoForge events upstream used:
 * {@code ClientTickEvent.Post}; {@code InputEvent.InteractionKeyMappingTriggered} for a right click with and without a
 * targeted block; the chain's selection outline is drawn in {@code SubmitCustomGeometryEvent}, and the vanilla block
 * outline is hidden through {@code ExtractBlockOutlineRenderStateEvent} while a chain is selected.
 */
public final class BnbClientEvents {

    private BnbClientEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
            final Minecraft client = Minecraft.getInstance();
            WeatheredGirderWrenchBehaviour.tick();

            if (client.level == null || client.player == null) {
                return;
            }

            CogwheelChainInteractionHandler.clientTick();
            CogwheelChainRidingHelper.clientTick(client.player);
            CogwheelChainPartialEditDisplayHandler.tick(client.player);
            CogwheelChainPlacementEffect.tick(client.player);
        });

        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, event -> {
            if (event.isUseItem() && Minecraft.getInstance().player != null
                    && CogwheelChainPlacementInteraction.onRightClick(event.getHand())) {
                event.setSwingHand(false);
                event.setCanceled(true);
            }
        });

        NeoForge.EVENT_BUS.addListener(SubmitCustomGeometryEvent.class,
                event -> CogwheelChainInteractionHandler.beforeBlockOutline(LevelRenderContext.of(event)));
        NeoForge.EVENT_BUS.addListener(ExtractBlockOutlineRenderStateEvent.class, event -> {
            if (CogwheelChainInteractionHandler.hasValidSelection())
                event.setCanceled(true);
        });

        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
            CogwheelChainPlacementInteraction.clearPlacingChain();
            CogwheelChainPartialEditInteractionHandler.clearEditState();
            CogwheelChainRidingHelper.reset();
            PlayerSkyhookRendererBridge.clear();
            CogwheelChainCandidate.clearCache();
        });
    }
}
