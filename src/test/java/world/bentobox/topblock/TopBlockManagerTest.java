package world.bentobox.topblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.aoneblock.AOneBlock;
import world.bentobox.aoneblock.dataobjects.OneBlockIslands;
import world.bentobox.aoneblock.listeners.BlockListener;
import world.bentobox.topblock.TopBlockManager.TopTenData;
import world.bentobox.topblock.config.ConfigSettings;

class TopBlockManagerTest extends CommonTestSetup {

    @Mock
    private TopBlock addon;
    @Mock
    private AOneBlock aob;
    @Mock
    private BlockListener bl;

    private TopBlockManager tbm;
    private ConfigSettings settings;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        settings = new ConfigSettings();
        when(addon.getPlugin()).thenReturn(plugin);
        when(addon.getSettings()).thenReturn(settings);
        when(addon.getaOneBlock()).thenReturn(aob);
        when(addon.getIslands()).thenReturn(im);
        when(aob.getBlockListener()).thenReturn(bl);
        when(im.getIslandById(anyString())).thenReturn(Optional.of(island));
        when(island.getWorld()).thenReturn(world);
        when(iwm.getPermissionPrefix(any())).thenReturn("aoneblock.");

        tbm = new TopBlockManager(addon);
    }

    private static OneBlockIslands ob(int blockNumber, long lifetime, String phase) {
        OneBlockIslands i = new OneBlockIslands(UUID.randomUUID().toString());
        i.setBlockNumber(blockNumber);
        i.setLifetime(lifetime);
        i.setPhaseName(phase);
        return i;
    }

    @Test
    void testGetTopTenEmptyByDefault() {
        assertTrue(tbm.getTopTen(10).isEmpty());
    }

    @Test
    void testGetOneBlockDataPopulatesTopTen() {
        when(bl.getAllIslands()).thenReturn(List.of(
                ob(50, 100, "Plains"),
                ob(80, 250, "Underground")));

        tbm.getOneBlockData();

        List<TopTenData> top = tbm.getTopTen(10);
        assertEquals(2, top.size());
        // Sorted descending by lifetime
        assertEquals(250L, top.get(0).lifetime());
        assertEquals(100L, top.get(1).lifetime());
    }

    @Test
    void testGetOneBlockDataFiltersZeroLifetime() {
        when(bl.getAllIslands()).thenReturn(List.of(
                ob(0, 0, "Plains"),
                ob(80, 250, "Underground")));

        tbm.getOneBlockData();

        List<TopTenData> top = tbm.getTopTen(10);
        assertEquals(1, top.size());
        assertEquals(250L, top.get(0).lifetime());
    }

    @Test
    void testGetOneBlockDataSkipsIslandsWithoutBentoBoxIsland() {
        when(im.getIslandById(anyString())).thenReturn(Optional.empty());
        when(bl.getAllIslands()).thenReturn(List.of(ob(80, 250, "Underground")));

        tbm.getOneBlockData();

        assertTrue(tbm.getTopTen(10).isEmpty());
    }

    @Test
    void testGetOneBlockDataReplacesPreviousResults() {
        when(bl.getAllIslands()).thenReturn(List.of(ob(80, 250, "Underground")));
        tbm.getOneBlockData();
        assertEquals(1, tbm.getTopTen(10).size());

        when(bl.getAllIslands()).thenReturn(List.of());
        tbm.getOneBlockData();
        assertTrue(tbm.getTopTen(10).isEmpty());
    }

    @Test
    void testGetTopTenLimitsSize() {
        when(bl.getAllIslands()).thenReturn(List.of(
                ob(10, 10, "a"),
                ob(20, 20, "b"),
                ob(30, 30, "c")));
        tbm.getOneBlockData();

        assertEquals(2, tbm.getTopTen(2).size());
    }

    @Test
    void testFormatLevelNullReturnsEmpty() {
        assertEquals("", tbm.formatLevel(null));
    }

    @Test
    void testFormatLevelNoShorthandReturnsRawString() {
        settings.setShorthand(false);
        assertEquals("104556", tbm.formatLevel(104556L));
    }

    @Test
    void testFormatLevelShorthandUnderThousandUnchanged() {
        settings.setShorthand(true);
        assertEquals("999", tbm.formatLevel(999L));
    }

    @Test
    void testFormatLevelShorthandKilo() {
        settings.setShorthand(true);
        assertEquals("10.5k", tbm.formatLevel(10500L));
    }

    @Test
    void testFormatLevelShorthandMega() {
        settings.setShorthand(true);
        assertEquals("1.5M", tbm.formatLevel(1_527_314L));
    }

    @Test
    void testFormatLevelShorthandGiga() {
        settings.setShorthand(true);
        assertEquals("3.9G", tbm.formatLevel(3_874_130_021L));
    }

    @Test
    void testGetOneBlockDataExcludesOnlineOwnerWithoutIntoptenPerm() {
        Player p = org.mockito.Mockito.mock(Player.class);
        when(p.hasPermission("aoneblock.intopten")).thenReturn(false);
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(p);

        when(bl.getAllIslands()).thenReturn(List.of(
                new OneBlockIslands(UUID.randomUUID().toString()) {{
                    setBlockNumber(80);
                    setLifetime(250);
                    setPhaseName("Underground");
                }}));
        tbm.getOneBlockData();

        assertTrue(tbm.getTopTen(10).isEmpty());
    }

    @Test
    void testGetOneBlockDataIncludesOnlineOwnerWithIntoptenPerm() {
        Player p = org.mockito.Mockito.mock(Player.class);
        when(p.hasPermission("aoneblock.intopten")).thenReturn(true);
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(p);

        when(bl.getAllIslands()).thenReturn(List.of(
                new OneBlockIslands(UUID.randomUUID().toString()) {{
                    setBlockNumber(80);
                    setLifetime(250);
                    setPhaseName("Underground");
                }}));
        tbm.getOneBlockData();

        assertEquals(1, tbm.getTopTen(10).size());
    }

    @Test
    void testGetOneBlockDataIncludesOfflineOwner() {
        // CommonTestSetup already stubs Bukkit.getPlayer -> null
        when(bl.getAllIslands()).thenReturn(List.of(
                new OneBlockIslands(UUID.randomUUID().toString()) {{
                    setBlockNumber(80);
                    setLifetime(250);
                    setPhaseName("Underground");
                }}));
        tbm.getOneBlockData();

        assertEquals(1, tbm.getTopTen(10).size());
    }

    @Test
    void testGetOneBlockDataExcludesIslandWithoutOwner() {
        when(island.getOwner()).thenReturn(null);
        when(bl.getAllIslands()).thenReturn(List.of(
                new OneBlockIslands(UUID.randomUUID().toString()) {{
                    setBlockNumber(80);
                    setLifetime(250);
                    setPhaseName("Underground");
                }}));
        tbm.getOneBlockData();

        assertTrue(tbm.getTopTen(10).isEmpty());
    }

    @Test
    void testTopTenDataRecordFields() {
        TopTenData d = new TopTenData(island, 42, 1234L, "phasy");
        assertNotNull(d);
        assertEquals(42, d.blockNumber());
        assertEquals(1234L, d.lifetime());
        assertEquals("phasy", d.phaseName());
        assertEquals(island, d.island());
    }
}
