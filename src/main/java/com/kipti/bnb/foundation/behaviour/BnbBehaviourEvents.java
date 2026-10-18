package com.kipti.bnb.foundation.behaviour;

import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Delivers vanilla interaction events to the behaviours this mod attaches to other mods' block entities. */
public final class BnbBehaviourEvents {

    private BnbBehaviourEvents() {
    }

    // NeoForge: UseBlockCallback -> PlayerInteractEvent.RightClickBlock (both sides, like Fabric's);
    // PlayerBlockBreakEvents.BEFORE -> BreakBlockEvent, which NeoForge also fires on the client, so that half is skipped
    public static void register() {
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, neoEvent -> {
            final InteractionResult result = onUse(neoEvent.getLevel(), neoEvent.getPos(), neoEvent.getEntity(), neoEvent.getHand());
            if (result != InteractionResult.PASS) {
                neoEvent.setCancellationResult(result);
                neoEvent.setCanceled(true);
            }
        });

        NeoForge.EVENT_BUS.addListener(BreakBlockEvent.class, neoEvent -> {
            if (neoEvent.getLevel() instanceof final Level level && !level.isClientSide())
                notifyBroken(level, neoEvent.getPos(), neoEvent.getPlayer(), level.getBlockEntity(neoEvent.getPos()));
        });
    }

    private static InteractionResult onUse(final Level level, final BlockPos pos, final Player player, final InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof final SmartBlockEntity blockEntity))
            return InteractionResult.PASS;
        final BlockUseEvent event = new BlockUseEvent(level, pos, player, player.getItemInHand(hand));
        for (final BlockEntityBehaviour<?> behaviour : blockEntity.getAllBehaviours()) {
            if (behaviour instanceof final SuperBlockEntityBehaviour superBehaviour)
                superBehaviour.onItemUse(event);
            if (event.isCanceled())
                return event.getCancellationResult();
        }
        return InteractionResult.PASS;
    }

    private static void notifyBroken(final Level level, final BlockPos pos, final Player player, final BlockEntity blockEntity) {
        if (!(blockEntity instanceof final SmartBlockEntity smartBlockEntity))
            return;
        for (final BlockEntityBehaviour<?> behaviour : smartBlockEntity.getAllBehaviours())
            if (behaviour instanceof final SuperBlockEntityBehaviour superBehaviour)
                superBehaviour.onBlockBroken(player);
    }
}
