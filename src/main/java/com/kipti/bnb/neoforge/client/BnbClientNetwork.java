package com.kipti.bnb.neoforge.client;

import com.kipti.bnb.foundation.config.BnbServerSettings;
import com.kipti.bnb.network.BnbPackets;
import com.kipti.bnb.network.ClientboundPacketPayload;
import com.kipti.bnb.network.packets.to_client.ServerConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client handlers for the clientbound packets. The payload types themselves are registered on both sides in
 * {@link BnbPackets#register}; NeoForge runs these handlers on the client thread.
 */
public final class BnbClientNetwork {

    private BnbClientNetwork() {
    }

    public static void register(final IEventBus modBus) {
        modBus.addListener(RegisterClientPayloadHandlersEvent.class, event -> {
            for (final BnbPackets packet : BnbPackets.values()) {
                if (packet.isServerbound())
                    continue;
                if (packet == BnbPackets.SERVER_CONFIG)
                    registerServerConfig(event);
                else
                    registerOne(event, packet.getType());
            }
        });
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> BnbServerSettings.clearSynced());
    }

    private static void registerServerConfig(final RegisterClientPayloadHandlersEvent event) {
        event.register(BnbPackets.SERVER_CONFIG.<ServerConfigPacket>getType(), (payload, context) -> {
            if (Minecraft.getInstance().hasSingleplayerServer() || !(context.player() instanceof final LocalPlayer player))
                return;
            payload.handle(player);
        });
    }

    private static <T extends CustomPacketPayload> void registerOne(final RegisterClientPayloadHandlersEvent event,
                                                                    final CustomPacketPayload.Type<T> type) {
        event.register(type, (payload, context) -> {
            if (context.player() instanceof final LocalPlayer player)
                ((ClientboundPacketPayload) payload).handle(player);
        });
    }

}
