package com.kipti.bnb.foundation.config.conditions;

import com.kipti.bnb.CreateBitsnBobs;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class BnbResourceConditions {

    public static final Identifier FEATURE_ENABLED = CreateBitsnBobs.asResource("feature_enabled");
    public static final Identifier FEATURE_ITEM_ENABLED = CreateBitsnBobs.asResource("feature_item_enabled");

    // NeoForge: data files use "neoforge:conditions" with these types (converted from "fabric:load_conditions")
    public static void register() {
        Registry.register(NeoForgeRegistries.CONDITION_SERIALIZERS, FEATURE_ENABLED, BnbFeatureEnabledCondition.CODEC);
        Registry.register(NeoForgeRegistries.CONDITION_SERIALIZERS, FEATURE_ITEM_ENABLED, BnbFeatureItemEnabledCondition.CODEC);
    }

}
