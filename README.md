# TopBlock
[![Build Status](https://ci.codemc.org/buildStatus/icon?job=BentoBoxWorld/TopBlock)](https://ci.codemc.org/job/BentoBoxWorld/job/TopBlock/)

## About

Add-on for BentoBox to calculate island levels for AOneBlock specifically. Ranks are determined by how many magic blocks have been mined - the count.

## How to use

1. Place the level addon jar in the addons folder of the BentoBox plugin. Make sure you have AOneBlock installed too!
2. Restart the server
3. The addon will create a data folder and inside the folder will be a config.yml
4. Edit the config.yml how you want.
5. Restart the server if you make a change

## Commands

`/ob topblock` - this shows the Top Ten

## Permissions
Permissions are given automatically to players as listed below. If your permissions plugin strips permissions then you may have to allocate these manually.

```
permissions:
  'aoneblock.island.topblock':
    description: Player can use TopBlock command
    default: true
  'aoneblock.intopten':
    description: Player's island will be listed in the top ten. Remove from admins or testers to hide them.
    default: true
```

If an island owner is **online** and does not have `aoneblock.intopten`, their island is excluded from the top ten and from placeholders. Offline owners are always included — to hide an admin or tester, remove the perm from the player who can actually log in. Removing the perm from an entire group (e.g. ops) excludes everyone in that group while online.

## Placeholders

```
%aoneblock_island_player_name_top_RANK% where RANK is 1 to 10 - Island owner's name
%aoneblock_island_member_names_top_RANK% where RANK is 1 to 10 - Name of island team members
%aoneblock_island_phase_name_top_RANK% where RANK is 1 to 10 - Name of the phase they have reached
%aoneblock_island_phase_number_top_RANK% where RANK is 1 to 10 - Phase number, e.g. Plains is 1, Underground is 2, etc.
%aoneblock_island_count_top_RANK% where RANK is 1 to 10 - Block Count of magic blocks mined this round
%aoneblock_island_lifetime_top_RANK% where RANK is 1 to 10 - Lifetime count of magic blocks mined
```
