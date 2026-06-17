# Mantle NeoForge 1.21.1 port - parallel agent brief

You are porting the **Mantle** library from Minecraft 1.20.1/Forge to **1.21.1/NeoForge**.
Repo root: `/ai/work/tinkers/Mantle`. The build system, JDK 21, and all package-import
renames are already done (no `net.minecraftforge.*` remain). Your job: fix the remaining
**semantic API reworks** in YOUR assigned package(s) only.

**Before editing:** read [`forge-to-neoforge.md`](forge-to-neoforge.md) (before/after code for
every pattern below), then read a few of your package's files to see the real errors.

## Rules
- **ONLY edit files under your assigned package(s).** Never edit another package. If you need
  another package's API, ASSUME the stable signatures below - do not change them.
- **Do NOT run gradle / compile.** The module will not compile until every agent finishes;
  the aggregate is compiled centrally afterward. Use `grep`/read freely.
- Write correct, idiomatic NeoForge 1.21.1 code following the doc. Preserve behavior + javadoc.
- Use `// TODO(neoport): <reason>` ONLY where a fix genuinely needs a cross-package decision or
  data-component design you cannot resolve locally. Minimize these.

## Key conventions (full code in forge-to-neoforge.md)
- **Registration:** `RegistryObject` is already `DeferredHolder<?, X>`. `DeferredRegister<T>.register`
  returns `DeferredHolder<T, I>`. `ForgeRegistries.X` -> `BuiltInRegistries.X` or `NeoForgeRegistries.X`.
  `IForgeRegistry` and `RegisterEvent.getForgeRegistry()` are gone; register via `DeferredRegister`
  on the mod bus, or `RegisterEvent.register(key, name, supplier)`.
- **Networking (STABLE API - only the `network` package implements it; everyone else just calls it):**
  packets implement `net.minecraft.network.protocol.common.custom.CustomPacketPayload` with a
  `Type<X>` + `StreamCodec`. Send via `MantleNetwork.INSTANCE`:
  `sendToServer(payload)`, `sendTo(payload, ServerPlayer)`,
  `sendToClientsAround(payload, ServerLevel, BlockPos)`, `sendToTracking(payload, Entity)`,
  `sendToTrackingAndSelf(payload, Entity)`. These signatures are UNCHANGED except the arg is now a
  `CustomPacketPayload`, so most callers need no change. `ISimplePacket extends CustomPacketPayload`.
- **Capabilities that STORE per-entity/-stack data** -> NeoForge **data attachments**
  (`AttachmentType`, `entity.getData/setData`). Behavior caps -> `BlockCapability`/`ItemCapability`
  + `RegisterCapabilitiesEvent`. `LazyOptional` is gone (return `@Nullable`).
- **Recipes/ingredients/conditions** -> vanilla codecs: `MapCodec<T>` + `StreamCodec<RegistryFriendlyByteBuf, T>`.
  `RecipeSerializer` uses a `MapCodec` + `StreamCodec` (no `fromJson`/`toNetwork` methods). `ICondition`
  is codec-based (`net.neoforged.neoforge.common.conditions`).
- **MobType was REMOVED in 1.21.** Replace with entity type tags (`EntityTypeTags`), `DamageTypeTags`,
  or `LivingEntity` accessors (e.g. `isInvertedHealAndHarm()`, `getMobType` is gone). For
  `MobTypePredicate`, port its registry to a tag-based predicate.
- **Fluids:** `FluidStack` is component-backed (`new FluidStack(fluid, amount)` ok; `getTag` -> components).
  `ForgeFlowingFluid` -> `BaseFlowingFluid`; `FluidType` via `NeoForgeRegistries.Keys.FLUID_TYPES`.
  `IFluidHandler` in `net.neoforged.neoforge.fluids.capability` via `Capabilities.FluidHandler`.
- `new ResourceLocation(ns,path)` is private -> `ResourceLocation.fromNamespaceAndPath` / `.parse`.
- `DataResult.getOrThrow(fn)` -> `getOrThrow()` (no-arg) or `getOrThrow(Function<String,E>)`.
- Advancement `TriggerInstance` -> wrap in `Criterion`.
- GUI: `RenderGuiOverlayEvent` -> `RenderGuiLayerEvent`; `RegisterGuiOverlaysEvent` -> `RegisterGuiLayersEvent`.

## Output (your final message = a report back to the orchestrator, not human-facing)
1. Files changed + the key API decision in each (one line each).
2. Cross-package assumptions you made.
3. Remaining `TODO(neoport)` items as `file:line - reason`.
Do not dump code.
