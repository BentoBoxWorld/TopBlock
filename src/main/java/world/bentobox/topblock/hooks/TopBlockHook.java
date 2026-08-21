package world.bentobox.topblock.hooks;

import java.util.List;

import world.bentobox.bentobox.api.addons.GameModeAddon;

/**
 * Abstraction over a supported magic-block game mode (AOneBlock, ChunkBlock).
 * The two game modes have twin APIs but unrelated types, so each implementation
 * must be the only class that references its game mode's packages — that way a
 * game mode that is not installed is never class-loaded.
 *
 * @author tastybento
 */
public interface TopBlockHook {

    /**
     * @return the game mode addon this hook wraps
     */
    GameModeAddon getGameMode();

    /**
     * @return magic block progress for every island in this game mode
     */
    List<IslandBlockData> getAllIslandData();

    /**
     * @param blockNumber block count to compare phase start blocks against
     * @return the number of phases that start strictly below the given block count
     */
    long getPhaseCount(int blockNumber);
}
