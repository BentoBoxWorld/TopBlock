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

## Permissions
Permissions are given automatically to players as listed below. If your permissions plugin strips permissions then you may have to allocate these manually. Note that if a player doesn't have the `intopten` permission, they will not be listed in the top ten.

```
permissions:    
  'aoneblock.intopten':
    description: Player is in the top ten.
    default: true
  'aoneblock.island.topblock':
    description: Player can use TopBlock command
    default: true
```

