package com.kipti.bnb.network;

import com.kipti.bnb.foundation.client.BnbClientHooks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

public final class BnbNetwork {

    private BnbNetwork() {
    }

    public static void sendToClient(final ServerPlayer player, final ClientboundPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToClientsTrackingEntity(final Entity entity, final ClientboundPacketPayload payload) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
    }

    public static void sendToAllClients(final MinecraftServer server, final ClientboundPacketPayload payload) {
        for (final ServerPlayer player : server.getPlayerList().getPlayers())
            PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToServer(final ServerboundPacketPayload payload) {
        BnbClientHooks.sendToServer(payload);
    }
}
