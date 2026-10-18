package com.kipti.bnb.foundation.config.conditions;

import com.kipti.bnb.registry.core.BnbFeatureFlag;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.NotNull;

public record BnbFeatureEnabledCondition(String featureFlagKey) implements ICondition {

    public static final MapCodec<BnbFeatureEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(
            c -> c.group(
                    Codec.STRING.fieldOf("feature").forGetter(BnbFeatureEnabledCondition::featureFlagKey)
            ).apply(c, BnbFeatureEnabledCondition::new)
    );

    // NeoForge: ICondition replaces Fabric's ResourceCondition; the codec is registered in BnbResourceConditions
    @Override
    public @NotNull MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(final IContext context) {
        return BnbFeatureFlag.isEnabled(this.featureFlagKey);
    }

}
