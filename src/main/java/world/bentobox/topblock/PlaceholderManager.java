package world.bentobox.topblock;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private GameModeAddon gm;
    private List<TopTenData> rList;

    public PlaceholderManager(TopBlock addon) {
        this.addon = addon;
        this.plugin = addon.getPlugin();

    }

    protected void registerPlaceholders(GameModeAddon gm) {
        if (plugin.getPlaceholdersManager() == null) return;
        this.gm = gm;
        updateTopTen();
        registerPlaceHolders();
    }

    /**
     * Update the top ten
     */
    public void updateTopTen() {
        rList = addon.getManager().getTopTen(TopBlock.TEN);
    }


    private void registerPlaceHolders() {
        // Register Top Ten Placeholders
        for (int i = 1; i <= TopBlock.TEN; i++) {
            registerPH(gm, i);
        }

    }

    private void registerPH(GameModeAddon gm, int r) {
        PlaceholdersManager bpm = plugin.getPlaceholdersManager();
        // Name of island owner
        bpm.registerPlaceholder(gm, "island_player_name_top_" + r, u -> getPlayerName(r));
        // Name of island team members
        bpm.registerPlaceholder(gm, "island_member_names_top_" + r, u -> getMemberNames(r));
        // Name of the phase they have reached
        bpm.registerPlaceholder(gm, "island_phase_name_top_" + r, u -> getPhaseName(r));
        // Phase Number
        bpm.registerPlaceholder(gm, "island_phase_number_top_" + r, u -> getPhaseNumber(r));
        // Block Count
        bpm.registerPlaceholder(gm, "island_count_top_" + r, u -> getBlockNumber(r));
        // Lifetime count
        bpm.registerPlaceholder(gm, "island_lifetime_top_" + r, u -> getLifetime(r));
    }

    private String getLifetime(int rank) {
        TopTenData r = rank - 1 < rList.size() ? rList.get(rank - 1) : null;
        if (r == null) return "";
        return String.valueOf(r.lifetime());
    }

    private String getBlockNumber(int rank) {
        TopTenData r = rank - 1 < rList.size() ? rList.get(rank - 1) : null;
        if (r == null) return "";
        return String.valueOf(r.blockNumber());
    }

    /**
     * Gets a comma separated string of island member names
     * @param r Top ten entry
     * @return comma separated string of island member names
     */
    String getMemberNames(int rank) {
        TopTenData r = rank - 1 < rList.size() ? rList.get(rank - 1) : null;
        if (r == null) return "";
        // Sort members by rank
        return r.island().getMembers().entrySet().stream()
                .filter(e -> e.getValue() >= RanksManager.MEMBER_RANK)
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .map(Map.Entry::getKey)
                .map(addon.getPlayers()::getName)
                .collect(Collectors.joining(","));
    }

    private String getPlayerName(int rank) {
        TopTenData r = rank - 1 < rList.size() ? rList.get(rank - 1) : null;
        if (r == null) return "";
        UUID owner = r.island().getOwner();
        if (owner == null) return "";
        return Objects.requireNonNull(addon.getPlayers().getName(owner), "");
    }

    private String getPhaseNumber(int rank) {
        TopTenData r = rank - 1 < rList.size() ? rList.get(rank - 1) : null;
        if (r == null) return "";
        long c = addon.getaOneBlock().getOneBlockManager().getBlockProbs().entrySet().stream()
                .filter(en -> en.getKey() < r.blockNumber()).count();
        return String.valueOf(c);
    }

    private String getPhaseName(int rank) {
        TopTenData r = rank - 1 < rList.size() ? rList.get(rank - 1) : null;
        if (r == null) return "";
        return r.phaseName();
    }

}
