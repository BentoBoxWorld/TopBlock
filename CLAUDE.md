# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

TopBlock is a BentoBox addon that produces a Top Ten ranking for the AOneBlock game mode based on how many magic blocks each island has mined. It is **not** standalone — it depends on the BentoBox plugin and the AOneBlock addon being present at runtime, and refuses to enable otherwise.

## Build & Test

Maven project, Java 21, Paper 1.21.11 API, BentoBox 3.14.0, AOneBlock 1.18.0.

- Build (default goal is `clean package`): `mvn package` — produces a shaded jar in `target/` named `TopBlock-<revision><build.number>.jar`. The shade plugin bundles only `lv.id.bonne:panelutils`; everything else is `provided`.
- Run tests: `mvn test`
- Run a single test class: `mvn test -Dtest=TopBlockManagerTest`
- Run a single test method: `mvn test -Dtest=TopBlockManagerTest#testFormatLevelShorthandKilo`
- The Surefire config sets a long list of `--add-opens` JVM flags — required for Mockito + MockBukkit reflection on Java 21; do not remove them when tweaking the build.

Version handling is driven by Maven properties: `build.version` is the human version (currently 1.1.0), `revision` resolves to `${build.version}-SNAPSHOT` locally and to `${build.version}` under the `master` profile (activated by `GIT_BRANCH=origin/master` on Jenkins). `build.number` is `-LOCAL` locally, `-b<num>` on CI, empty on master. Don't hand-edit `<version>` — bump `build.version`.

## Runtime entry points (Pladdon pattern)

There are **two** main classes and the distinction matters:

- `TopBlockPladdon` (referenced by `plugin.yml`) is the Bukkit-facing `Pladdon`. Spigot loads this; its only job is `getAddon() → new TopBlock()`.
- `TopBlock` (referenced by `addon.yml`) is the BentoBox `Addon`. All real lifecycle (`onLoad`, `onEnable`, `onDisable`) lives here.

`onEnable` looks up the AOneBlock addon via `getPlugin().getAddonsManager().getAddonByName("aoneblock")`; if missing or not a `GameModeAddon`, the addon disables itself. The `/<gamemode> topblock` command is registered against AOneBlock's player command, not as a top-level command.

## Data flow

`TopBlockManager` is a `Listener` that reacts to `BentoBoxReadyEvent` (handler is `public void onBentoBoxReady` — Bukkit silently skips private @EventHandler methods, which is what broke the addon historically) to start a repeating Bukkit task. The task period is `settings.getRefreshTime() * 20L * 60` ticks (minutes → ticks). Each tick of the task:

1. Calls `AOneBlock.getBlockListener().getAllIslands()` — this reads every island, so the refresh interval is intentionally coarse (default 5 min, min 1 min).
2. Builds a fresh `List<TopTenData>` (record of island + blockNumber + lifetime + phaseName) — sorted at read time via `Comparator` on `lifetime` then `blockNumber`.
3. Updates `PlaceholderManager`'s cached snapshot.

Placeholders are registered once via a `runTaskLater` 10-tick delay after the first ready event (so PAPI / BentoBox's `PlaceholdersManager` is up). Names follow `island_<field>_top_<1..10>` and are scoped to the AOneBlock `GameModeAddon`. The `TopBlock.TEN` constant is the source of truth for the list size.

## Panel

`TopLevelPanel` uses BentoBox's `TemplatedPanelBuilder`. The template file is shipped in `src/main/resources/panels/top_panel.yml` and copied to the data folder on load via `saveResource("panels/top_panel.yml", false)` — players' edits to the on-disk file persist across restarts. Localization keys live under `topblock.gui.buttons.island.*` in `src/main/resources/locales/en-US.yml`. The icon material can be overridden per-player via the `<permissionPrefix>topblock.icon.<MATERIAL>` permission.

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

JaCoCo excludes `**/*Names*` to avoid synthetic-field issues on JavaBeans — keep that exclusion if adding similar classes.
