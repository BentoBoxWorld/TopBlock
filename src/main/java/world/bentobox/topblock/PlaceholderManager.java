package world.bentobox.topblock;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.topblock.TopBlockManager.TopTenData;

/**
 * Handles TopBlock placeholders
 * @author tastybento
 *
 */
public class PlaceholderManager {

    private final TopBlock addon;
    private final BentoBox plugin;

    public PlaceholderManager(TopBlock addon) {
        this.addon = addon;
        this.plugin = addon.getPlugin();

    }

    protected void registerPlaceholders(GameModeAddon gm) {
        if (plugin.getPlaceholdersManager() == null) return;
        PlaceholdersManager bpm = plugin.getPlaceholdersManager();
        @NonNull List<TopTenData> rList = addon.getManager().getTopTen(TopBlock.TEN);
        // Register Top Ten Placeholders
        for (int i = 0; i < TopBlock.TEN; i++) {
            TopTenData r = i < rList.size() ? rList.get(i) : null;
            int rank = i + 1;
            registerPH(bpm, gm, rank, r);
        }
    }

    private void registerPH(PlaceholdersManager bpm, GameModeAddon gm, int rank, TopTenData r) {
        // Name of island owner
        bpm.registerPlaceholder(gm, "island_player_name_top_" + rank, u -> r == null ? "" : getPlayerName(r));
        // Name of island team members
        bpm.registerPlaceholder(gm, "island_member_names_top_" + rank, u -> r == null ? "" : getMemberNames(r));
        // Name of the phase they have reached
        bpm.registerPlaceholder(gm, "island_phase_name_top_" + rank, u -> r == null ? "" : r.phaseName());
        // Phase Number
        bpm.registerPlaceholder(gm, "island_phase_number_top_" + rank, u -> r == null ? "" : getPhaseNumber(r));
        // Block Count
        bpm.registerPlaceholder(gm, "island_count_top_" + rank, u -> r == null ? "" : String.valueOf(r.blockNumber()));
        // Lifetime count
        bpm.registerPlaceholder(gm, "island_lifetime_top_" + rank, u -> r == null ? "" : String.valueOf(r.lifetime()));
    }

    /**
     * Gets a comma separated string of island member names
     * @param r Top ten entry
     * @return comma separated string of island member names
     */
    String getMemberNames(TopTenData r) {
        // Sort members by rank
        return r.island().getMembers().entrySet().stream()
                .filter(e -> e.getValue() >= RanksManager.MEMBER_RANK)
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .map(Map.Entry::getKey)
                .map(addon.getPlayers()::getName)
                .collect(Collectors.joining(","));
    }

    private String getPlayerName(TopTenData r) {
        UUID owner = r.island().getOwner();
        if (owner == null) return "";
        return Objects.requireNonNull(addon.getPlayers().getName(owner), "");
    }

    private String getPhaseNumber(TopTenData r) {
        long c = addon.getaOneBlock().getOneBlockManager().getBlockProbs().entrySet().stream()
                .filter(en -> en.getKey() < r.blockNumber()).count();
        return String.valueOf(c);
    }


}
