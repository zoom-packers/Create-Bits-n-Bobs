package com.kipti.bnb.registry.core;

import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import net.minecraft.core.Registry;
import net.neoforged.neoforge.registries.RegistryBuilder;

public class BnbRegistries {

    // NeoForge: registered in NewRegistryEvent (neoforge/BnbNeoForge), replacing FabricRegistryBuilder#buildAndRegister
    public static final Registry<CogwheelChainType> COGWHEEL_CHAIN_TYPES =
            new RegistryBuilder<>(BnbResourceKeys.COGWHEEL_CHAIN_TYPE).create();

}
