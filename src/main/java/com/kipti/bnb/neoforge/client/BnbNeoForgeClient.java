package com.kipti.bnb.neoforge.client;

import com.kipti.bnb.CreateBitsnBobs;
import com.kipti.bnb.CreateBitsnBobsClient;
import com.kipti.bnb.foundation.client.BnbClientHooks;
import com.zurrtum.create.neoforge.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * NeoForge client entrypoint, replacing the Fabric {@code client} entrypoint ({@code fabric/client/BnbFabricClient}).
 * <p>
 * The client initialisation reads block entity types, items and the renderer hooks that {@link CreateBitsnBobs#init()}
 * registers from the first {@link RegisterEvent} (see {@code BnbNeoForge}). It can't run while mods are constructed,
 * as Create's does, so it runs from the same first event at the lowest priority, right after the common one.
 */
@Mod(value = CreateBitsnBobs.MOD_ID, dist = Dist.CLIENT)
public final class BnbNeoForgeClient {
    private static boolean initialised;

    public BnbNeoForgeClient(final IEventBus modBus) {
        BnbClientHooks.setBridge(new BnbClientBridge());
        BnbClientNetwork.register(modBus);
        modBus.addListener(EventPriority.LOWEST, RegisterEvent.class, event -> {
            if (!initialised) {
                initialised = true;
                CreateBitsnBobsClient.initClient(modBus);
            }
        });
        modBus.addListener(AddPackFindersEvent.class, BnbNeoForgeClient::addCogwheelItemsPack);
    }

    /**
     * The always-on "Material Cogwheel Items" resource pack, which Fabric API's ResourceLoader registered as a
     * built-in pack.
     */
    private static void addCogwheelItemsPack(final AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES)
            return;
        for (final Path root : Platform.getModRootPaths(CreateBitsnBobs.MOD_ID)) {
            final Path packRoot = root.resolve("resourcepacks/cogwheel_items");
            if (!Files.isDirectory(packRoot))
                continue;
            final PackLocationInfo info = new PackLocationInfo(CreateBitsnBobs.MOD_ID + "/cogwheel_items",
                    Component.literal("Material Cogwheel Items"), PackSource.BUILT_IN, Optional.empty());
            final Pack pack = Pack.readMetaAndCreate(info, new PathPackResources.PathResourcesSupplier(packRoot),
                    PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Pack.Position.TOP, false));
            if (pack != null)
                event.addRepositorySource(consumer -> consumer.accept(pack));
            return;
        }
    }
}
