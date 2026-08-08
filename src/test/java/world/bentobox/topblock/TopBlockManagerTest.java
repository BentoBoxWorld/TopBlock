package world.bentobox.topblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.topblock.TopBlockManager.TopTenData;
import world.bentobox.topblock.config.ConfigSettings;
import world.bentobox.topblock.hooks.IslandBlockData;
import world.bentobox.topblock.hooks.TopBlockHook;

class TopBlockManagerTest extends CommonTestSetup {

    @Mock
    private TopBlock addon;
    @Mock
    private TopBlockHook hook;

    private TopBlockManager tbm;
    private ConfigSettings settings;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        settings = new ConfigSettings();
        when(addon.getPlugin()).thenReturn(plugin);
        when(addon.getSettings()).thenReturn(settings);
        when(addon.getHooks()).thenReturn(List.of(hook));
        when(addon.getIslands()).thenReturn(im);
        when(im.getIslandById(anyString())).thenReturn(Optional.of(island));
        when(island.getWorld()).thenReturn(world);
        when(iwm.getPermissionPrefix(any())).thenReturn("aoneblock.");

        tbm = new TopBlockManager(addon);
    }

    private static IslandBlockData ib(int blockNumber, long lifetime, String phase) {
        return new IslandBlockData(UUID.randomUUID().toString(), blockNumber, lifetime, phase);
    }

    @Test
    void testGetTopTenEmptyByDefault() {
        assertTrue(tbm.getTopTen(hook, 10).isEmpty());
    }

    @Test
    void testRefreshPopulatesTopTen() {
        when(hook.getAllIslandData()).thenReturn(List.of(
                ib(50, 100, "Plains"),
                ib(80, 250, "Underground")));

        tbm.refreshAll();

        List<TopTenData> top = tbm.getTopTen(hook, 10);
        assertEquals(2, top.size());
        // Sorted descending by lifetime
        assertEquals(250L, top.get(0).lifetime());
        assertEquals(100L, top.get(1).lifetime());
    }

    @Test
    void testRefreshFiltersZeroLifetime() {
        when(hook.getAllIslandData()).thenReturn(List.of(
                ib(0, 0, "Plains"),
                ib(80, 250, "Underground")));

        tbm.refreshAll();

        List<TopTenData> top = tbm.getTopTen(hook, 10);
        assertEquals(1, top.size());
        assertEquals(250L, top.get(0).lifetime());
    }

    @Test
    void testRefreshSkipsIslandsWithoutBentoBoxIsland() {
        when(im.getIslandById(anyString())).thenReturn(Optional.empty());
        when(hook.getAllIslandData()).thenReturn(List.of(ib(80, 250, "Underground")));

        tbm.refreshAll();

        assertTrue(tbm.getTopTen(hook, 10).isEmpty());
    }

    @Test
    void testRefreshReplacesPreviousResults() {
        when(hook.getAllIslandData()).thenReturn(List.of(ib(80, 250, "Underground")));
        tbm.refreshAll();
        assertEquals(1, tbm.getTopTen(hook, 10).size());

        when(hook.getAllIslandData()).thenReturn(List.of());
        tbm.refreshAll();
        assertTrue(tbm.getTopTen(hook, 10).isEmpty());
    }

    @Test
    void testTopTensAreSeparatePerHook() {
        TopBlockHook hook2 = mock(TopBlockHook.class);
        when(addon.getHooks()).thenReturn(List.of(hook, hook2));
        when(hook.getAllIslandData()).thenReturn(List.of(ib(80, 250, "Underground")));
        when(hook2.getAllIslandData()).thenReturn(List.of(
                ib(10, 30, "Plains"),
                ib(20, 40, "Plains")));

        tbm.refreshAll();

        assertEquals(1, tbm.getTopTen(hook, 10).size());
        assertEquals(250L, tbm.getTopTen(hook, 10).get(0).lifetime());
        assertEquals(2, tbm.getTopTen(hook2, 10).size());
        assertEquals(40L, tbm.getTopTen(hook2, 10).get(0).lifetime());
    }

    @Test
    void testGetTopTenByWorld() {
        when(addon.getHook(world)).thenReturn(Optional.of(hook));
        when(hook.getAllIslandData()).thenReturn(List.of(ib(80, 250, "Underground")));
        tbm.refreshAll();

        assertEquals(1, tbm.getTopTen(world, 10).size());
    }

    @Test
    void testGetTopTenByWorldWithoutHookIsEmpty() {
        when(addon.getHook(world)).thenReturn(Optional.empty());
        when(hook.getAllIslandData()).thenReturn(List.of(ib(80, 250, "Underground")));
        tbm.refreshAll();

        assertTrue(tbm.getTopTen(world, 10).isEmpty());
    }

    @Test
    void testGetTopTenLimitsSize() {
        when(hook.getAllIslandData()).thenReturn(List.of(
                ib(10, 10, "a"),
                ib(20, 20, "b"),
                ib(30, 30, "c")));
        tbm.refreshAll();

        assertEquals(2, tbm.getTopTen(hook, 2).size());
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
    void testRefreshExcludesOnlineOwnerWithoutIntoptenPerm() {
        Player p = mock(Player.class);
        when(p.hasPermission("aoneblock.intopten")).thenReturn(false);
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(p);

        when(hook.getAllIslandData()).thenReturn(List.of(ib(80, 250, "Underground")));
        tbm.refreshAll();

        assertTrue(tbm.getTopTen(hook, 10).isEmpty());
    }

    @Test
    void testRefreshIncludesOnlineOwnerWithIntoptenPerm() {
        Player p = mock(Player.class);
        when(p.hasPermission("aoneblock.intopten")).thenReturn(true);
        mockedBukkit.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(p);

        when(hook.getAllIslandData()).thenReturn(List.of(ib(80, 250, "Underground")));
        tbm.refreshAll();

        assertEquals(1, tbm.getTopTen(hook, 10).size());
    }

    @Test
    void testRefreshIncludesOfflineOwner() {
        // CommonTestSetup already stubs Bukkit.getPlayer -> null
        when(hook.getAllIslandData()).thenReturn(List.of(ib(80, 250, "Underground")));
        tbm.refreshAll();

        assertEquals(1, tbm.getTopTen(hook, 10).size());
    }

    @Test
    void testRefreshExcludesIslandWithoutOwner() {
        when(island.getOwner()).thenReturn(null);
        when(hook.getAllIslandData()).thenReturn(List.of(ib(80, 250, "Underground")));
        tbm.refreshAll();

        assertTrue(tbm.getTopTen(hook, 10).isEmpty());
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
