# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

TopBlock is a BentoBox addon that produces a Top Ten ranking for the AOneBlock and ChunkBlock game modes based on how many magic blocks each island has mined. It is **not** standalone — it depends on the BentoBox plugin plus at least one of the AOneBlock or ChunkBlock addons being present at runtime, and refuses to enable otherwise. Each hooked game mode gets its own independent top ten, command, and placeholders.

## Build & Test

Maven project, Java 21, Paper 1.21.11 API, BentoBox 3.14.0, AOneBlock 1.18.0, ChunkBlock 1.0.1 (both game modes `provided`; Level is a `test`-only dependency because mocking ChunkBlock requires its hard dependency on the classpath).

- Build (default goal is `clean package`): `mvn package` — produces a shaded jar in `target/` named `TopBlock-<revision><build.number>.jar`. The shade plugin bundles only `lv.id.bonne:panelutils`; everything else is `provided`.
- Run tests: `mvn test`
- Run a single test class: `mvn test -Dtest=TopBlockManagerTest`
- Run a single test method: `mvn test -Dtest=TopBlockManagerTest#testFormatLevelShorthandKilo`
- The Surefire config sets a long list of `--add-opens` JVM flags — required for Mockito + MockBukkit reflection on Java 21; do not remove them when tweaking the build.

Version handling is driven by Maven properties: `build.version` is the human version (currently 2.1.1), `revision` resolves to `${build.version}-SNAPSHOT` locally and to `${build.version}` under the `master` profile (activated by `GIT_BRANCH=origin/master` on Jenkins). `build.number` is `-LOCAL` locally, `-b<num>` on CI, empty on master. Don't hand-edit `<version>` — bump `build.version`.

## Runtime entry points (Pladdon pattern)

There are **two** main classes and the distinction matters:

- `TopBlockPladdon` (referenced by `plugin.yml`) is the Bukkit-facing `Pladdon`. Spigot loads this; its only job is `getAddon() → new TopBlock()`.
- `TopBlock` (referenced by `addon.yml`) is the BentoBox `Addon`. All real lifecycle (`onLoad`, `onEnable`, `onDisable`) lives here.

`onEnable` looks up each supported game mode via `getPlugin().getAddonsManager().getAddonByName(...)` (`"aoneblock"`, `"chunkblock"` — lookup is case-insensitive); for each one present, enabled, and a `GameModeAddon`, it registers a `topblock` subcommand on that game mode's player command and adds a hook (see below). If no game mode hooks, the addon disables itself. `addon.yml` declares `softdepend: AOneBlock, ChunkBlock` (soft, because either alone is enough).

## Game mode hooks

AOneBlock and ChunkBlock have twin APIs (`getBlockListener().getAllIslands()`, `getOneBlockManager().getBlockProbs()`, `OneBlockIslands` data objects) but in unrelated packages, so `world.bentobox.topblock.hooks` abstracts them:

- `TopBlockHook` — interface: `getGameMode()`, `getAllIslandData()` (returns neutral `IslandBlockData` records), `getPhaseCount(blockNumber)`.
- `AOneBlockHook` / `ChunkBlockHook` — the **only** classes allowed to import their game mode's packages. Class loading is lazy, so a missing game mode is never class-loaded as long as its hook is only instantiated after the presence check in `onEnable`. Keep it that way: never import `world.bentobox.aoneblock.*` or `world.bentobox.chunkblock.*` anywhere else in main code.

`TopBlock.getHooks()` lists active hooks; `TopBlock.getHook(World)` resolves the hook owning a world via `GameModeAddon.inWorld`.

## Data flow

`TopBlockManager` is a `Listener` that reacts to `BentoBoxReadyEvent` (handler is `public void onBentoBoxReady` — Bukkit silently skips private @EventHandler methods, which is what broke the addon historically) to start a repeating Bukkit task. The task period is `settings.getRefreshTime() * 20L * 60` ticks (minutes → ticks). Each tick of the task:

1. Calls `refreshAll()` — for every hook, reads every island of that game mode via `hook.getAllIslandData()`, so the refresh interval is intentionally coarse (default 5 min, min 1 min). `getAllIslandData()` is a full synchronous database read (`loadObjects()` in the game mode), so `refresh(hook)` runs it on an async task and only hops back to the main thread (`processIslandData`) for the island registry / player / permission lookups — keep the async/sync split when changing this code.
2. `processIslandData` builds a fresh `List<TopTenData>` (record of island + blockNumber + lifetime + phaseName) per hook, kept in a `Map<TopBlockHook, List<TopTenData>>` — sorted at read time via `Comparator` on `lifetime` then `blockNumber`.
3. It then updates `PlaceholderManager`'s cached per-hook snapshots (per hook, as each async load completes).

Placeholders are registered once per hook via a `runTaskLater` 10-tick delay after the first ready event (so PAPI / BentoBox's `PlaceholdersManager` is up). Names follow `island_<field>_top_<1..10>` and are scoped to each hook's `GameModeAddon`, so the PAPI prefix keeps game modes apart (`%aoneblock_...%` vs `%chunkblock_...%`). The `TopBlock.TEN` constant is the source of truth for the list size.

## Panel

`TopLevelPanel` uses BentoBox's `TemplatedPanelBuilder`. The template file is shipped in `src/main/resources/panels/top_panel.yml` and copied to the data folder on load via `saveResource("panels/top_panel.yml", false)` — players' edits to the on-disk file persist across restarts. The panel shows the top ten of whichever game mode owns the command's world (`getTopTen(world, TEN)`). Localization keys live under `topblock.gui.buttons.island.*` in `src/main/resources/locales/en-US.yml`. The icon material can be overridden per-player via the `<permissionPrefix>topblock.icon.<MATERIAL>` permission.

The panel has no click actions (TopBlock doesn't bundle Warp/Visit hooks like Level does). The YAML still declares `warp`/`visit` actions with tooltips, but no click handler is registered — clicking does nothing.

## Resource filtering

`pom.xml` filters `src/main/resources` (so `${version}` etc. in `addon.yml` / `plugin.yml` get substituted) **except** `src/main/resources/locales`, which is copied verbatim to `./locales` to avoid Maven mangling YAML colons / placeholder syntax in translations.

## Tests

JUnit 5 + Mockito + MockBukkit. Test classes extend `CommonTestSetup` which:
- Mocks `Bukkit` statically and provides a real `MockBukkit.mock()` server (needed for Tag/Material initialisation).
- Injects the BentoBox singleton via `WhiteBox.setInternalState(BentoBox.class, "instance", plugin)`.
- Sets up the standard graph of mocks: `IslandWorldManager`, `IslandsManager`, `PlayersManager`, `LocalesManager`, `PlaceholdersManager`, `Notifier`, `HooksManager`, `BlueprintsManager`.
- Calls `User.setPlugin(plugin)` and pre-creates a `User` instance for `mockPlayer` (uuid `tastybento`).

`TestWorldSettings` returns `"TopBlock"` for friendly name and `"topblock."` for permission prefix. The addon test (`TopBlockTest`) builds an in-memory `addon.jar` containing `config.yml` + `panels/top_panel.yml` because `Addon.saveResource` reads from a real JarFile.

Manager and placeholder tests mock `TopBlockHook` directly (no game mode classes needed); `AOneBlockHookTest` / `ChunkBlockHookTest` cover the real hook mapping. A freshly constructed `Addon` starts in `State.DISABLED` and only AddonsManager sets ENABLED — so enable-path tests call `setState(State.LOADED)` before `onEnable()` and treat DISABLED afterwards as "the addon disabled itself"; don't assert ENABLED after `onEnable()`.

JaCoCo excludes `**/*Names*` to avoid synthetic-field issues on JavaBeans — keep that exclusion if adding similar classes.
