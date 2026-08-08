package world.bentobox.topblock.hooks;

import java.util.List;

import world.bentobox.aoneblock.AOneBlock;
import world.bentobox.bentobox.api.addons.GameModeAddon;

/**
 * Hook for the AOneBlock game mode.
 *
 * @author tastybento
 */
public class AOneBlockHook implements TopBlockHook {

    private final AOneBlock addon;

    public AOneBlockHook(GameModeAddon addon) {
        this.addon = (AOneBlock) addon;
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
