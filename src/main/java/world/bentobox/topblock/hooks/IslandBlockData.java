package world.bentobox.topblock.hooks;

/**
 * Game-mode-neutral snapshot of an island's magic block progress.
 * @param uniqueId BentoBox island unique id
 * @param blockNumber the number of blocks mined this time around
 * @param lifetime the lifetime number of blocks mined
 * @param phaseName the name of the current phase
 *
 * @author tastybento
 */
public record IslandBlockData(String uniqueId, int blockNumber, long lifetime, String phaseName) {
}
