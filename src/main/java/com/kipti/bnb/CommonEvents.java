package com.kipti.bnb;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainCandidate;
import com.kipti.bnb.content.kinetics.cogwheel_chain.riding.ServerCogwheelChainRidingHandler;
import com.kipti.bnb.foundation.caching.LevelSafeStorage;
import com.kipti.bnb.foundation.caching.LevelSimpleCache;
import com.kipti.bnb.foundation.command.BnbCommands;
import com.kipti.bnb.network.BnbNetwork;
import com.kipti.bnb.network.packets.to_client.ServerConfigPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class CommonEvents {

    private CommonEvents() {
    }

    // NeoForge: Fabric API's server events, mapped to NeoForge's game bus
    public static void register() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> ServerCogwheelChainRidingHandler.tick());
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> ServerCogwheelChainRidingHandler.reset());
        NeoForge.EVENT_BUS.addListener(LevelEvent.Unload.class, event -> {
            if (event.getLevel() instanceof final ServerLevel level) {
                LevelSimpleCache.clearAllForLevel(level);
                LevelSafeStorage.clearAllForLevel(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof final ServerPlayer player)
                BnbNetwork.sendToClient(player, ServerConfigPacket.ofLocalConfig());
        });
        // Fabric's END_DATA_PACK_RELOAD: NeoForge syncs data packs to everyone (no single player) after a reload
        NeoForge.EVENT_BUS.addListener(OnDatapackSyncEvent.class, event -> {
            if (event.getPlayer() == null)
                CogwheelChainCandidate.clearCache();
        });
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> BnbCommands.register(event.getDispatcher()));
    }
}
