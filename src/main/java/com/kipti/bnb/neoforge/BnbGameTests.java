package com.kipti.bnb.neoforge;

import com.kipti.bnb.CreateBitsnBobs;
import com.kipti.bnb.registry.content.blocks.BnbKineticBlocks;
import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.kinetics.base.DirectionalKineticBlock;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.content.kinetics.base.RotatedPillarKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

/**
 * ZP5 smoke tests for the NeoForge port, run by {@code runGameTestServer}. They reuse Create's empty test structure
 * ({@code create:gametest/empty_9x7x5}).
 */
public final class BnbGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CreateBitsnBobs.MOD_ID);
    private static final ResourceKey<Consumer<GameTestHelper>> CONTENT_REGISTERED = key("content_registered");
    private static final ResourceKey<Consumer<GameTestHelper>> FLANGED_COGWHEEL_MESHES = key("flanged_cogwheel_meshes_with_create");

    static {
        FUNCTIONS.register(CONTENT_REGISTERED.identifier().getPath(), () -> BnbGameTests::contentRegistered);
        FUNCTIONS.register(FLANGED_COGWHEEL_MESHES.identifier().getPath(), () -> BnbGameTests::flangedCogwheelMeshesWithCreate);
    }

    private BnbGameTests() {
    }

    private static ResourceKey<Consumer<GameTestHelper>> key(final String path) {
        return ResourceKey.create(Registries.TEST_FUNCTION, CreateBitsnBobs.asResource(path));
    }

    public static void register(final IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(BnbGameTests::registerTests);
    }

    private static void registerTests(final RegisterGameTestsEvent event) {
        final Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(CreateBitsnBobs.asResource("default"));
        final Identifier structure = Identifier.fromNamespaceAndPath("create", "gametest/empty_9x7x5");
        for (final ResourceKey<Consumer<GameTestHelper>> test : new ResourceKey[]{CONTENT_REGISTERED, FLANGED_COGWHEEL_MESHES}) {
            event.registerTest(test.identifier(),
                    new FunctionGameTestInstance(test, new TestData<>(environment, structure, 400, 0, true)));
        }
    }

    private static long countOwn(final Registry<?> registry) {
        return registry.keySet().stream().filter(id -> id.getNamespace().equals(CreateBitsnBobs.MOD_ID)).count();
    }

    /**
     * The blocks, items and block entity types are registered, and the data pack's recipes loaded through the
     * converted {@code neoforge:conditions}.
     */
    private static void contentRegistered(final GameTestHelper helper) {
        final long blocks = countOwn(BuiltInRegistries.BLOCK);
        final long items = countOwn(BuiltInRegistries.ITEM);
        final long blockEntities = countOwn(BuiltInRegistries.BLOCK_ENTITY_TYPE);
        final long entities = countOwn(BuiltInRegistries.ENTITY_TYPE);
        final long recipes = helper.getLevel().recipeAccess().getRecipes().stream()
                .filter(holder -> holder.id().identifier().getNamespace().equals(CreateBitsnBobs.MOD_ID)).count();
        final String state = blocks + " blocks, " + items + " items, " + blockEntities + " block entity types, "
                + entities + " entity types, " + recipes + " recipes";
        helper.assertTrue(blocks > 100 && items > 100 && blockEntities > 5 && recipes > 50, state);
        CreateBitsnBobs.LOGGER.info("content_registered passed: {}", state);
        helper.succeed();
    }

    /**
     * A creative motor turns a Bits 'n' Bobs small flanged cogwheel, which must mesh with a Create cogwheel next to it
     * and turn it the other way.
     */
    private static void flangedCogwheelMeshesWithCreate(final GameTestHelper helper) {
        final BlockPos motor = new BlockPos(2, 1, 2);
        final BlockPos flanged = new BlockPos(2, 2, 2);
        final BlockPos cogwheel = new BlockPos(3, 2, 2);
        for (int x = 0; x < 9; x++)
            for (int z = 0; z < 5; z++)
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        helper.setBlock(motor, AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(DirectionalKineticBlock.FACING, Direction.UP));
        helper.setBlock(flanged, BnbKineticBlocks.SMALL_FLANGED_COGWHEEL.getDefaultState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
        helper.setBlock(cogwheel, AllBlocks.COGWHEEL.defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, Direction.Axis.Y));
        helper.succeedWhen(() -> {
            final float motorSpeed = helper.getBlockEntity(motor, KineticBlockEntity.class).getSpeed();
            final float flangedSpeed = helper.getBlockEntity(flanged, KineticBlockEntity.class).getSpeed();
            final float cogwheelSpeed = helper.getBlockEntity(cogwheel, KineticBlockEntity.class).getSpeed();
            final String state = "motor " + motorSpeed + " rpm, flanged cogwheel " + flangedSpeed + " rpm, Create cogwheel " + cogwheelSpeed + " rpm";
            helper.assertTrue(motorSpeed != 0 && flangedSpeed == motorSpeed && cogwheelSpeed == -flangedSpeed, state);
            CreateBitsnBobs.LOGGER.info("flanged_cogwheel_meshes_with_create passed at tick {}: {}", helper.getTick(), state);
        });
    }
}
