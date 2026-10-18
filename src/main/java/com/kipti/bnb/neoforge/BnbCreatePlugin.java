package com.kipti.bnb.neoforge;

import com.kipti.bnb.CreateBitsnBobs;
import com.zurrtum.create.api.registry.CreateRegisterPlugin;

/**
 * Registers Bits 'n' Bobs' blocks while Create registers its own blocks during vanilla bootstrap. Listed in
 * {@code META-INF/services/com.zurrtum.create.api.registry.CreateRegisterPlugin}, which replaces the Fabric
 * {@code create_plugin} entrypoint.
 */
public final class BnbCreatePlugin implements CreateRegisterPlugin {
    private static boolean blocksRegistered;

    @Override
    public void onBlockRegister() {
        if (blocksRegistered) {
            throw new IllegalStateException("Create Fly invoked Bits 'n' Bobs block registration more than once");
        }
        CreateBitsnBobs.registerBlocksEarly();
        blocksRegistered = true;
    }

    public static void verifyEarlyRegistrationComplete() {
        if (!blocksRegistered) {
            throw new IllegalStateException("Create Fly did not invoke Bits 'n' Bobs early block registration");
        }
    }
}
