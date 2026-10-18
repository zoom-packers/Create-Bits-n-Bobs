package com.kipti.bnb.neoforge;

import com.kipti.bnb.CreateBitsnBobs;
import com.kipti.bnb.network.BnbPackets;
import com.kipti.bnb.registry.core.BnbRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge entrypoint, replacing the Fabric {@code main} entrypoint ({@code fabric/BnbFabric}).
 * <p>
 * Blocks (and their items) are registered during vanilla bootstrap by {@link BnbCreatePlugin}, alongside Create's.
 * Fabric leaves the built-in registries open after bootstrap, so upstream registers everything else from its mod
 * initializer. NeoForge only opens them while it posts {@link RegisterEvent}s, and opens all of them for the whole
 * sequence, so that registration runs from the first one.
 */
@Mod(CreateBitsnBobs.MOD_ID)
public final class BnbNeoForge {
    private static boolean initialised;

    public BnbNeoForge(final IEventBus modBus) {
        BnbCreatePlugin.verifyEarlyRegistrationComplete();
        modBus.addListener(NewRegistryEvent.class, event -> event.register(BnbRegistries.COGWHEEL_CHAIN_TYPES));
        modBus.addListener(RegisterEvent.class, event -> {
            if (!initialised) {
                initialised = true;
                CreateBitsnBobs.init();
            }
        });
        modBus.addListener(RegisterPayloadHandlersEvent.class, BnbPackets::register);
        BnbGameTests.register(modBus);
    }
}
