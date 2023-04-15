package world.bentobox.topblock;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.aoneblock.AOneBlock;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.database.objects.Island;


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

    // Top ten lists
    private final List<TopTenData> topTen = new ArrayList<>();


    /**
     * Top Block Manager - provides methods to get data
     * @param addon addon
     */
    public TopBlockManager(TopBlock addon) {
        this.addon = addon;

    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void startMonitoring(BentoBoxReadyEvent e) {
        // Load the top ten from AOneBlock every so often
        Bukkit.getScheduler().runTaskTimer(addon.getPlugin(), this::getOneBlockData, 0, addon.getSettings().getRefreshTime() * 20L * 60);
        // Register placeholders after everything is loaded
        Bukkit.getScheduler().runTaskLater(addon.getPlugin(), () -> new PlaceholderManager(addon).registerPlaceholders(addon.getaOneBlock()), 10L);
    }

    void getOneBlockData() {
        AOneBlock ob = addon.getaOneBlock();
        topTen.clear();
        ob.getBlockListener().getAllIslands().stream().filter(i -> i.getLifetime() > 0).forEach(i -> {
            // Get player island.
            Island island = addon.getIslands().getIslandById(i.getUniqueId()).orElse(null);
            topTen.add(new TopTenData(island, i.getBlockNumber(), i.getLifetime(), i.getPhaseName()));
        });
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
     * Get the top ten. Returns offline players or players with the intopten permission.
     * @param size - size of the top ten
     * @return sorted top ten map
     */
    @NonNull
    public List<TopTenData> getTopTen(int size) {
        // Return the sorted map
        return topTen.stream()
                .sorted(Collections.reverseOrder()).limit(size)
                .toList();
    }

}
