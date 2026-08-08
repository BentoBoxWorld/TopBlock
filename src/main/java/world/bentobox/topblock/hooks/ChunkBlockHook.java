package world.bentobox.topblock.hooks;

import java.util.List;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.chunkblock.ChunkBlock;

/**
 * Hook for the ChunkBlock game mode.
 *
 * @author tastybento
 */
public class ChunkBlockHook implements TopBlockHook {

    private final ChunkBlock addon;

    public ChunkBlockHook(GameModeAddon addon) {
        this.addon = (ChunkBlock) addon;
    }

    @Override
    public GameModeAddon getGameMode() {
        return addon;
    }

    @Override
    public List<IslandBlockData> getAllIslandData() {
        return addon.getBlockListener().getAllIslands().stream()
                .map(i -> new IslandBlockData(i.getUniqueId(), i.getBlockNumber(), i.getLifetime(), i.getPhaseName()))
                .toList();
    }

    @Override
    public long getPhaseCount(int blockNumber) {
        return addon.getOneBlockManager().getBlockProbs().headMap(blockNumber).size();
    }
}
