package com.kipti.bnb.neoforge.client;

import com.kipti.bnb.foundation.client.BnbClientHooks;
import com.kipti.bnb.network.ServerboundPacketPayload;
import com.zurrtum.create.client.flywheel.lib.visualization.VisualizationHelper;
import com.zurrtum.create.client.foundation.utility.ServerSpeedProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class BnbClientBridge implements BnbClientHooks.Bridge {

    @Override
    public void queueVisualUpdate(final BlockEntity blockEntity) {
        VisualizationHelper.queueUpdate(blockEntity);
    }

    @Override
    public void sendToServer(final ServerboundPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    @Override
    public float serverSpeedModifier() {
        return ServerSpeedProvider.get();
    }

}
