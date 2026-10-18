# Porting notes: Create Fly: Bits 'n' Bobs (Fabric 26.2) → NeoForge 26.3

Written 2026-10-18. Checked against Minecraft 26.3, NeoForge 26.3.0.89, Java 25 and Zoompack 5's NeoForge port of
Create Fly (`create-fly-neoforge-6.0.9+neoforge-26.3`, ZP5 `mods/create-fly-neoforge`, commit `8da5b8c6`).

Branch `zp5/26.3` of [chaevsfe/Create-Bits-n-Bobs](https://github.com/chaevsfe/Create-Bits-n-Bobs), starting from
`fabric-26.2` at `5cb55f93` (2.3.5.1+fly-mc26.2, Modrinth project `create-fly-bits-n-bobs-unofficial-port`). That
branch is chaevsfe's Fabric port of [Aztech-Modding/Create-Bits-n-Bobs](https://github.com/Aztech-Modding/Create-Bits-n-Bobs)
(NeoForge 1.21.1, Creators-of-Create) to Create Fly.

Licence: MIT (see `LICENSE` and `NOTICE`). Mod id `bits_n_bobs`, package `com.kipti.bnb`.

## Why a native port, not Sinytra Connector

- There is no Fabric Create Fly for 26.3. The only Create on 26.3 is ZP5's NeoForge port, so the Fabric jar would have
  to link against a NeoForge mod through Connector, plus the 26.2 → 26.3 vanilla changes would still need a recompile.
- The port's `create_plugin` entrypoint is a Java `ServiceLoader` service in the NeoForge Create port, which Connector
  doesn't translate.
- The Fabric surface is small and already isolated: 5 files in `fabric/`, about 30 Fabric API imports (events,
  networking, resource conditions, one registry builder, creative tabs), so a native port was quick and needs no
  Connector or Forgified Fabric API at runtime.

## Build

- ModDevGradle 2.0.148, NeoForge 26.3.0.89, Gradle 9.2.1 (the wrapper ZP5 uses), Java 25.
- Compile and dev-run dependencies live in `libs/` (gitignored):
  - `libs/create-fly-neoforge-6.0.9+neoforge-26.3.jar`, `libs/create-accesstransformer.cfg` (its
    `META-INF/accesstransformer.cfg`) and `libs/create-interfaces.json` (`mods/create-fly-neoforge/interfaces.json`),
    snapshotted from ZP5's Create port. Refresh all three together.
  - `libs/StrutYourStuff-1.3.1+neoforge-26.3-zp5.1.jar`, built from the `zp5/26.3` branch of the Strut Your Stuff
    clone next to this one (`../struts`, see its PORTING.md).
- `./gradlew build` gives `build/libs/CreateBitsnBobs-<version>+neoforge-26.3-<port_build>.jar`. `port_build` in
  `gradle.properties` is bumped for every released jar. `check` still runs upstream's `checkForeignNamespaces`.
- `./gradlew runGameTestServer` runs the `bits_n_bobs` game tests (`neoforge/BnbGameTests`) with Create and Strut
  Your Stuff loaded from `libs/`. `-PmixinDebug` turns on verbose mixin logging.
- The Fabric files stay in the tree and are excluded in `build.gradle.kts` (`com/kipti/bnb/fabric/**`,
  `fabric.mod.json`), so rebases onto upstream don't conflict on them.
- Strut Your Stuff ships as its own jar instead of being nested (the Fabric jar nests it with Create REI Viewer).
  Create REI Viewer is dropped: ZP5 uses JEI, and Bits 'n' Bobs adds no recipe types of its own (its recipes are
  vanilla crafting/stonecutting and Create splashing/item application, which Create's JEI plugin shows).

## Loader and registration

| Fabric | NeoForge |
|---|---|
| `main` entrypoint `fabric/BnbFabric` | `@Mod` `neoforge/BnbNeoForge` |
| `client` entrypoint `fabric/client/BnbFabricClient` | `@Mod(dist = CLIENT)` `neoforge/client/BnbNeoForgeClient`, which runs `CreateBitsnBobsClient.initClient` from the first `RegisterEvent` at `EventPriority.LOWEST`, right after `CreateBitsnBobs.init()` (see below) |
| `create_plugin` entrypoint (`onBlockRegister`) | `META-INF/services/com.zurrtum.create.api.registry.CreateRegisterPlugin` → `neoforge/BnbCreatePlugin`. Blocks and their items still register during vanilla bootstrap, next to Create's |
| Everything else registered from the mod initializer | `CreateBitsnBobs.init()` runs from the first `RegisterEvent`. NeoForge opens every registry while it posts the `RegisterEvent`s, and only then |
| `FabricRegistryBuilder` (`BnbRegistries.COGWHEEL_CHAIN_TYPES`) | `RegistryBuilder`, registered in `NewRegistryEvent` |
| `FabricCreativeModeTab.builder()` | vanilla `CreativeModeTab.builder()` |
| `FabricLoader#isModLoaded` / `isDevelopmentEnvironment` | Create's `com.zurrtum.create.neoforge.Platform#isModLoaded` / `!FMLLoader.getCurrent().isProduction()` |
| Fabric `ResourceLoader.registerBuiltinPack` ("Material Cogwheel Items") | `AddPackFindersEvent`, always-on built-in client pack from `resourcepacks/cogwheel_items` |
| `@Environment(EnvType.CLIENT)` (83 files) | removed (NeoForge does not strip classes; client code is only reached from the client entrypoint or `BnbClientHooks`) |

The client initialisation reads block entity types and items, and flushes the renderer hooks the registrate queues
while registering them, so it must run after `CreateBitsnBobs.init()`. `zp5.1` ran it at mod construction, which
crashed every client: `BnbTooltipBehaviours` loaded `BnbBlockEntities` before any `RegisterEvent`, and creating a
`BlockEntityType` outside one throws `Registry is already frozen`. GameTest and dedicated-server runs never construct
the client entrypoint, so only a client boot shows this (checked in a full-pack client on 2026-10-19 for `zp5.2`).
Nothing on the client may build an `ItemStack` or `FluidStack` before it joins a world either: on 26.3 the client binds
item and fluid data components from the server's registry data (`RegistryDataCollector`).

## Fabric API → NeoForge

| Fabric API | NeoForge |
|---|---|
| `PayloadTypeRegistry` + `ServerPlayNetworking` (`BnbPackets`) | `RegisterPayloadHandlersEvent`: serverbound with a handler, clientbound registered bare |
| `ClientPlayNetworking.registerGlobalReceiver` | `RegisterClientPayloadHandlersEvent` (`neoforge/client/BnbClientNetwork`) |
| `ServerPlayNetworking.send`, `PlayerLookup.tracking/all` (`BnbNetwork`) | `PacketDistributor.sendToPlayer` / `sendToPlayersTrackingEntity`, the server's player list |
| `ClientPlayNetworking.send` | `ClientPacketDistributor.sendToServer` (`neoforge/client/BnbClientBridge`) |
| `ResourceCondition` / `ResourceConditionType` | `ICondition` with a `MapCodec`, registered in `NeoForgeRegistries.CONDITION_SERIALIZERS` |
| Data: `"fabric:load_conditions": [{"condition": …}]` (335 files) | `"neoforge:conditions": [{"type": …}]`, converted by `tools/zp5/fabric_conditions_to_neoforge.py` |
| `ServerTickEvents.END_SERVER_TICK`, `ServerLifecycleEvents.SERVER_STOPPING`, `ServerLevelEvents.UNLOAD` | `ServerTickEvent.Post`, `ServerStoppingEvent`, `LevelEvent.Unload` (server levels) |
| `ServerPlayConnectionEvents.JOIN` | `PlayerEvent.PlayerLoggedInEvent` |
| `ServerLifecycleEvents.END_DATA_PACK_RELOAD` | `OnDatapackSyncEvent` with no player (fires after `/reload`) |
| `CommandRegistrationCallback` | `RegisterCommandsEvent` |
| `UseBlockCallback` (`BnbBehaviourEvents`, `DragInteractionClientHandler`) | `PlayerInteractEvent.RightClickBlock`, cancelled with the callback's result |
| `PlayerBlockBreakEvents.BEFORE` (`BnbBehaviourEvents`, `HeadlampBlock`) | `BreakBlockEvent`. NeoForge also fires it on the client: behaviours only act on the server; the headlamp removal runs on both sides, which cancels the client's predicted break, and only the server pops the item |
| `UseBlockCallback` + `UseItemCallback` (chain placement, `BnbClientEvents`) | `InputEvent.InteractionKeyMappingTriggered` (use), which upstream's NeoForge code used |
| `ClientTickEvents.END_CLIENT_TICK`, `ClientPlayConnectionEvents.DISCONNECT`, `ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE` | `ClientTickEvent.Post`, `ClientPlayerNetworkEvent.LoggingOut`, client `LevelEvent.Load` |
| `LevelExtractionEvents.END_EXTRACTION` / `LevelRenderEvents.COLLECT_SUBMITS` (cogwheel chains) | `ExtractLevelRenderStateEvent` / `SubmitCustomGeometryEvent` |
| `LevelRenderEvents.BEFORE_BLOCK_OUTLINE` (chain selection) | the outline is drawn in `SubmitCustomGeometryEvent`, and `ExtractBlockOutlineRenderStateEvent` is cancelled while a chain is selected |
| `LevelRenderContext` | `neoforge/client/LevelRenderContext`, a record with the same accessors built from `SubmitCustomGeometryEvent` (its pose stack is camera-relative, like Fabric's) |

## Minecraft 26.2 → 26.3 and Create Fly changes

- **Block codecs removed.** The `CODEC` fields and `codec()` overrides were dropped from five blocks.
- **Renames.** `PushReaction` constants (`NORMAL` → `PUSH_PULL`, `DESTROY` → `POPPED`, `BLOCK` → `IMMOVEABLE`).
- **New parameters.** `Inventory#placeItemBackInInventory` takes a `Prediction` (`SERVER_ONLY` here);
  `Properties#isViewBlocking` takes an AABB argument.
- **`ValueInput#contains`** is gone; NeoForge's `keySet().contains(...)` replaces it.
- **Model quads.** `MaterialInfo` gained the glint render types, and `shade` became `shadeDirectionOverride`.
- **Input.** GLFW became SDL3: the drag screen hides the cursor with `SDLMouse.SDL_HideCursor` and re-centres it.
- **Pack format.** The built-in client pack is `min_format`/`max_format` `[97, 1]`.
- **Data.** `tools/zp5/data_26_3.py` (from the Create port) converted 145 loot tables, 492 constructs in all, and 9
  block models (`shade`). A lint afterwards finds 0 legacy constructs, and no advancement points at a missing recipe.
- **Create's access transformer** makes `BlockBehaviour#getShape` and `#rotate` public, so 11 overrides became public.
- **Create Fly split `EncasedCogVisual`** into `EncasedSmallCogVisual` / `EncasedLargeCogVisual` (`BnbBlockEntities`,
  `CogwheelMaterialVisualMixin`).

## Mixins

`tools/zp5/check_mixins.py` (from the Create port, extended for dotted mixin names) checks targets, methods and
`@At` targets against the patched 26.3 jar, NeoForge, Create and Strut Your Stuff. It found four breaks, now fixed:

| Mixin | Problem | Fix |
|---|---|---|
| `dyeable.create_blocks.BlockItemMixin` | `updateCustomBlockEntityTag` is static in 26.3 and lost its `BlockState` | `@WrapOperation` on that call in `BlockItem#place` (same point in the placement) |
| `dyeable.fluid_tank.FluidTankItemMixin` | `FluidTankItem` no longer overrides `updateCustomBlockEntityTag` | the tank dye is applied from the `BlockItemMixin` wrapper |
| `client.cogwheel_material.BlockEntityRenderDispatcherMixin` | NeoForge added a `Frustum` overload of `tryExtractRenderState` | wraps the 5-argument one; the 4-argument one only delegates |
| `client.cogwheel_material.CogwheelMaterialVisualMixin` | `EncasedCogVisual`'s constructor no longer exists | retargeted to the new visuals' `(context, blockEntity, partialTick)` constructor |

## Status

Verified 2026-10-18 (Windows, Java 25.0.1):
- `./gradlew build` passes, including `checkForeignNamespaces`.
- `tools/zp5/check_mixins.py`: 0 problems in both configs.
- `./gradlew runGameTestServer` with Create and Strut Your Stuff: the world loads and all 4 game tests pass, with no
  warnings or errors in the log:
  - `bits_n_bobs:content_registered`: 148 blocks, 111 items, 16 block entity types, 2 entity types and 163 recipes
    registered. 177 recipe files ship; the other 14 belong to features that are off by default (gigantic cogwheel,
    throttle lever and wooden strut are experimental, the brass lamp is legacy), so the converted `neoforge:conditions`
    work.
  - `bits_n_bobs:flanged_cogwheel_meshes_with_create`: a creative motor turns a small flanged cogwheel at 16 RPM,
    which turns a Create cogwheel next to it at −16 RPM.
  - Create's own `create:water_wheel_drives_press` still passes.
- With `-PmixinDebug`, 22 of the 28 common mixins applied with no failures. The other 6 (`ItemInputMixin`,
  `ServerEntityMixin`, `ConnectivityHandlerMixin`, `ContraptionColliderMixin`, `BeltSlicerMixin`,
  `FluidTransportBehaviourMixin`) target classes a game-test server never loads; the static check covers them.

Not verified:
- **Anything client-side**: rendering, Flywheel visuals, Ponder scenes, the cogwheel chain renderer and interactions,
  nixie text, the drag screen, the built-in cogwheel item pack. That needs `runClient`, which opens a game window, so
  it waits for the user's OK.
- Trains, contraptions and multiplayer packet round trips.

Upstream's `deferred/` directory (not compiled) is unchanged; see its README for what the Fabric port set aside.
