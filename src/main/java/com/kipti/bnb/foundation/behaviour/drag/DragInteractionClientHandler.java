package com.kipti.bnb.foundation.behaviour.drag;

import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class DragInteractionClientHandler {

    private DragInteractionClientHandler() {
    }

    public static void register() {
        // NeoForge: UseBlockCallback -> PlayerInteractEvent.RightClickBlock
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, event -> {
            if (!event.getLevel().isClientSide())
                return;

            final BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
            if (!(blockEntity instanceof final SmartBlockEntity sbe))
                return;

            final DragInteractionBehaviour behaviour = sbe.getBehaviour(DragInteractionBehaviour.TYPE);
            if (behaviour == null)
                return;

            Minecraft.getInstance().setScreenAndShow(new DragInteractionScreen(
                    event.getPos(),
                    behaviour.getValue(),
                    behaviour.getMin(),
                    behaviour.getMax()
            ));
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        });
    }
}
