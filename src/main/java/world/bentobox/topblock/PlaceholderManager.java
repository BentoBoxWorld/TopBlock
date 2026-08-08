package world.bentobox.topblock;

import java.util.Collections;
import java.util.HashMap;
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
import world.bentobox.topblock.hooks.TopBlockHook;

/**
 * Handles TopBlock placeholders. Placeholders are registered once per hooked
 * game mode, scoped to that game mode's addon, so each game mode gets its own
 * independent set, e.g. {@code %aoneblock_island_count_top_1%} and
 * {@code %chunkblock_island_count_top_1%}.
 * @author tastybento
 *
 */
public class PlaceholderManager {

    private final TopBlock addon;
    private final BentoBox plugin;
    // Cached top ten snapshot per hooked game mode
    private final Map<TopBlockHook, List<TopTenData>> topTens = new HashMap<>();

    public PlaceholderManager(TopBlock addon) {
        this.addon = addon;
        this.plugin = addon.getPlugin();

    }

    protected void registerPlaceholders(TopBlockHook hook) {
        if (plugin.getPlaceholdersManager() == null) return;
        topTens.put(hook, addon.getManager().getTopTen(hook, TopBlock.TEN));
        // Register Top Ten Placeholders
        for (int i = 1; i <= TopBlock.TEN; i++) {
            registerPH(hook, i);
        }
    }

    /**
     * Update the cached top ten for every registered game mode
     */
    public void updateTopTen() {
        topTens.replaceAll((hook, list) -> addon.getManager().getTopTen(hook, TopBlock.TEN));
    }

    private void registerPH(TopBlockHook hook, int r) {
        PlaceholdersManager bpm = plugin.getPlaceholdersManager();
        GameModeAddon gm = hook.getGameMode();
        // Name of island owner
        bpm.registerPlaceholder(gm, "island_player_name_top_" + r, u -> getPlayerName(hook, r));
        // Name of island team members
        bpm.registerPlaceholder(gm, "island_member_names_top_" + r, u -> getMemberNames(hook, r));
        // Name of the phase they have reached
        bpm.registerPlaceholder(gm, "island_phase_name_top_" + r, u -> getPhaseName(hook, r));
        // Phase Number
        bpm.registerPlaceholder(gm, "island_phase_number_top_" + r, u -> getPhaseNumber(hook, r));
        // Block Count
        bpm.registerPlaceholder(gm, "island_count_top_" + r, u -> getBlockNumber(hook, r));
        // Lifetime count
        bpm.registerPlaceholder(gm, "island_lifetime_top_" + r, u -> getLifetime(hook, r));
    }

    private TopTenData getEntry(TopBlockHook hook, int rank) {
        List<TopTenData> rList = topTens.getOrDefault(hook, List.of());
        return rank - 1 < rList.size() ? rList.get(rank - 1) : null;
    }

    private String getLifetime(TopBlockHook hook, int rank) {
        TopTenData r = getEntry(hook, rank);
        if (r == null) return "";
        return String.valueOf(r.lifetime());
    }

    private String getBlockNumber(TopBlockHook hook, int rank) {
        TopTenData r = getEntry(hook, rank);
        if (r == null) return "";
        return String.valueOf(r.blockNumber());
    }

    /**
     * Gets a comma separated string of island member names
     * @param hook game mode hook
     * @param rank Top ten rank
     * @return comma separated string of island member names
     */
    String getMemberNames(TopBlockHook hook, int rank) {
        TopTenData r = getEntry(hook, rank);
        if (r == null) return "";
        // Sort members by rank
        return r.island().getMembers().entrySet().stream()
                .filter(e -> e.getValue() >= RanksManager.MEMBER_RANK)
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .map(Map.Entry::getKey)
                .map(addon.getPlayers()::getName)
                .collect(Collectors.joining(","));
    }

    private String getPlayerName(TopBlockHook hook, int rank) {
        TopTenData r = getEntry(hook, rank);
        if (r == null) return "";
        UUID owner = r.island().getOwner();
        if (owner == null) return "";
        return Objects.requireNonNull(addon.getPlayers().getName(owner), "");
    }

    private String getPhaseNumber(TopBlockHook hook, int rank) {
        TopTenData r = getEntry(hook, rank);
        if (r == null) return "";
        return String.valueOf(hook.getPhaseCount(r.blockNumber()));
    }

    private String getPhaseName(TopBlockHook hook, int rank) {
        TopTenData r = getEntry(hook, rank);
        if (r == null) return "";
        return r.phaseName();
    }

}
