package com.kipti.bnb.foundation.config.conditions;

import com.kipti.bnb.registry.core.BnbFeatureFlag;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public record BnbFeatureItemEnabledCondition(ResourceKey<Item> itemKey) implements ICondition {

    public static final MapCodec<BnbFeatureItemEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(
            c -> c.group(
                    ResourceKey.codec(BuiltInRegistries.ITEM.key()).fieldOf("item").forGetter(BnbFeatureItemEnabledCondition::itemKey)
            ).apply(c, BnbFeatureItemEnabledCondition::new)
    );

    // NeoForge: ICondition replaces Fabric's ResourceCondition; the codec is registered in BnbResourceConditions
    @Override
    public @NotNull MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(final IContext context) {
        return BuiltInRegistries.ITEM.getOptional(this.itemKey)
                .map(BnbFeatureFlag::isEnabled)
                .orElse(true);
    }

}
