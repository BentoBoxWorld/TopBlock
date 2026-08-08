# TopBlock
[![Build Status](https://ci.codemc.org/buildStatus/icon?job=BentoBoxWorld/TopBlock)](https://ci.codemc.org/job/BentoBoxWorld/job/TopBlock/)

## About

TopBlock is a [BentoBox](https://github.com/BentoBoxWorld/BentoBox) addon that produces a Top Ten ranking for the [AOneBlock](https://github.com/BentoBoxWorld/AOneBlock) and [ChunkBlock](https://github.com/BentoBoxWorld/ChunkBlock) game modes based on how many magic blocks each island has mined. Either game mode (or both) can be installed — each gets its own independent top ten, command, and placeholders.

## Requirements

- Paper 1.21.x (Spigot is no longer supported)
- Java 21
- BentoBox 3.14.0 or later
- At least one of:
  - AOneBlock 1.18.0 or later
  - ChunkBlock 1.0.1 or later

## How to use

1. Drop the TopBlock jar into your server's `plugins/BentoBox/addons/` folder. AOneBlock and/or ChunkBlock must already be installed there too.
2. Restart the server. TopBlock will create `addons/TopBlock/config.yml` and `addons/TopBlock/panels/top_panel.yml`.
3. Edit `config.yml` if you want to tune anything (see below) and restart the server again to apply.

## Configuration

`addons/TopBlock/config.yml`:

| Option         | Default | Description                                                                                                       |
|----------------|---------|-------------------------------------------------------------------------------------------------------------------|
| `refresh-time` | `5`     | How often the Top Ten is recalculated, in minutes. Minimum 1. Each refresh reads every island, so don't set it too low. |
| `shorthand`    | `false` | If `true`, format large counts using units — `10,500` becomes `10.5k`, `1,527,314` becomes `1.5M`, etc.            |

The panel layout lives in `addons/TopBlock/panels/top_panel.yml`. Edits are preserved across restarts.

## Commands

`/ob topblock` (alias: `/oneblock topblock`) — opens the AOneBlock Top Ten panel.

If ChunkBlock is installed, its player command gets a `topblock` subcommand too, which opens ChunkBlock's own Top Ten panel.

To get into the top ten, a player just needs to mine at least one magic block on their island. Each game mode keeps a separate ranking. The lists refresh every `refresh-time` minutes; a player who just started mining may need to wait that long before appearing.

## Permissions

Permissions are given automatically to players. If your permissions plugin strips defaults, allocate them manually:

```yaml
permissions:
  'aoneblock.island.topblock':
    description: Player can use the TopBlock command
    default: true
  'aoneblock.intopten':
    description: Player's island will be listed in the top ten. Remove from admins or testers to hide them.
    default: true
  'chunkblock.island.topblock':
    description: Player can use the TopBlock command
    default: true
  'chunkblock.intopten':
    description: Player's island will be listed in the top ten. Remove from admins or testers to hide them.
    default: true
```

If an island owner is **online** and lacks `<gamemode>.intopten`, their island is excluded from that game mode's top ten panel and placeholders. **Offline** owners are always included — to hide an admin or tester, remove the perm from the player who can actually log in. Removing the perm from an entire group (e.g. ops) excludes everyone in that group while online.

The icon shown for each rank can be overridden per player by granting `<gamemode>.topblock.icon.<MATERIAL>` (for example `aoneblock.topblock.icon.diamond_block`). Without an override, the rank icon is the player's head.

## Placeholders

```
%aoneblock_island_player_name_top_RANK%   - Island owner's name
%aoneblock_island_member_names_top_RANK%  - Comma-separated team members (highest rank first)
%aoneblock_island_phase_name_top_RANK%    - Name of the phase the island has reached
%aoneblock_island_phase_number_top_RANK%  - Phase number, e.g. Plains is 1, Underground is 2
%aoneblock_island_count_top_RANK%         - Block count of magic blocks mined this round
%aoneblock_island_lifetime_top_RANK%      - Lifetime count of magic blocks mined
```

If ChunkBlock is installed, the same placeholders exist with the `chunkblock` prefix (e.g. `%chunkblock_island_count_top_1%`) and report ChunkBlock's own ranking.

`RANK` is `1` to `10`. If fewer than `RANK` islands qualify for the top ten, the placeholder returns an empty string.

## Building from source

```bash
mvn clean package
```

Produces `target/TopBlock-<version>.jar`.

## License

[EPL-2.0](LICENSE)
