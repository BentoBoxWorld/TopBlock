package world.bentobox.topblock;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.topblock.hooks.TopBlockHook;


public class TopBlockManager implements Listener {
    private static final TreeMap<BigInteger, String> LEVELS;
    private static final BigInteger THOUSAND = BigInteger.valueOf(1000);
    static {
        LEVELS = new TreeMap<>();

        LEVELS.put(THOUSAND, "k");
        LEVELS.put(THOUSAND.pow(2), "M");
        LEVELS.put(THOUSAND.pow(3), "G");
        LEVELS.put(THOUSAND.pow(4), "T");
    }
    private final TopBlock addon;
    private final PlaceholderManager phm;

    /**
     * @param island island
     * @param blockNumber the number of blocks mined this time around
     * @param lifetime the lifetime number of blocks mined
     * @param phaseName the name of the current phase
     *
     */
    public record TopTenData(Island island, int blockNumber, long lifetime, String phaseName) implements Comparable<TopTenData> {

        @Override
        public int compareTo(TopTenData o) {
            return Objects.compare(this, o,
                    Comparator.comparingLong(TopTenData::lifetime)
                    .thenComparingInt(TopTenData::blockNumber));
        }
    }

    // Top ten lists, one per hooked game mode
    private final Map<TopBlockHook, List<TopTenData>> topTens = new HashMap<>();


    /**
     * Top Block Manager - provides methods to get data
     * @param addon addon
     */
    public TopBlockManager(TopBlock addon) {
        this.addon = addon;
        this.phm = new PlaceholderManager(addon);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBentoBoxReady(BentoBoxReadyEvent e) {
        // Load the top ten from each hooked game mode every so often
        Bukkit.getScheduler().runTaskTimer(addon.getPlugin(), () -> {
            // Update TopTens
            refreshAll();
            // Update placeholders
            phm.updateTopTen();
        }, 0, addon.getSettings().getRefreshTime() * 20L * 60);
        // Register placeholders after everything is loaded
        Bukkit.getScheduler().runTaskLater(addon.getPlugin(),
                () -> addon.getHooks().forEach(phm::registerPlaceholders), 10L);
    }

    void refreshAll() {
        addon.getHooks().forEach(this::refresh);
    }

    void refresh(TopBlockHook hook) {
        List<TopTenData> data = new ArrayList<>();
        hook.getAllIslandData().stream().filter(i -> i.lifetime() > 0).forEach(i ->
        addon.getIslands().getIslandById(i.uniqueId())
                .filter(this::ownerInTopTen)
                .ifPresent(island ->
        data.add(new TopTenData(island, i.blockNumber(), i.lifetime(), i.phaseName()))));
        topTens.put(hook, data);
    }

    /**
     * Returns true if the island's owner should be listed in the top ten.
     * Offline owners always pass — admins must remove the perm from a player
     * who can actually log in. An online owner without the {@code <prefix>intopten}
     * permission is excluded.
     */
    private boolean ownerInTopTen(Island island) {
        UUID owner = island.getOwner();
        if (owner == null) {
            return false;
        }
        Player player = Bukkit.getPlayer(owner);
        if (player == null) {
            return true;
        }
        String permPrefix = addon.getPlugin().getIWM().getPermissionPrefix(island.getWorld());
        return player.hasPermission(permPrefix + "intopten");
    }

    /**
     * Get the string representation of the level. May be converted to shorthand notation, e.g., 104556 = 10.5k
     * @param lvl - long value to represent
     * @return string of the level.
     */
    public String formatLevel(@Nullable Long lvl) {
        if (lvl == null) return "";
        String level = String.valueOf(lvl);
        // Asking for the level of another player
        if(addon.getSettings().isShorthand()) {
            BigInteger levelValue = BigInteger.valueOf(lvl);

            Map.Entry<BigInteger, String> stage = LEVELS.floorEntry(levelValue);

            if (stage != null) { // level > 1000
                // 1 052 -> 1.0k
                // 1 527 314 -> 1.5M
                // 3 874 130 021 -> 3.8G
                // 4 002 317 889 -> 4.0T
                level = new DecimalFormat("#.#").format(levelValue.divide(stage.getKey().divide(THOUSAND)).doubleValue()/1000.0) + stage.getValue();
            }
        }
        return level;
    }

    /**
     * Get the top ten for a hooked game mode. Returns offline players or players with the intopten permission.
     * @param hook - game mode hook
     * @param size - size of the top ten
     * @return sorted top ten list
     */
    @NonNull
    public List<TopTenData> getTopTen(TopBlockHook hook, int size) {
        // Return the sorted list
        return topTens.getOrDefault(hook, List.of()).stream()
                .sorted(Collections.reverseOrder()).limit(size)
                .toList();
    }

    /**
     * Get the top ten for the game mode that owns the given world.
     * @param world - world of a hooked game mode
     * @param size - size of the top ten
     * @return sorted top ten list, empty if no game mode owns this world
     */
    @NonNull
    public List<TopTenData> getTopTen(World world, int size) {
        return addon.getHook(world).map(h -> getTopTen(h, size)).orElseGet(List::of);
    }

}
