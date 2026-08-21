package world.bentobox.topblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.topblock.config.ConfigSettings;
import world.bentobox.topblock.hooks.IslandBlockData;
import world.bentobox.topblock.hooks.TopBlockHook;

class PlaceholderManagerTest extends CommonTestSetup {

    @Mock
    private TopBlock addon;
    @Mock
    private TopBlockHook hook;
    @Mock
    private GameModeAddon gameMode;
    @Mock
    private PlayersManager playersMgr;

    private TopBlockManager tbm;
    private PlaceholderManager phMgr;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        ConfigSettings settings = new ConfigSettings();
        when(addon.getPlugin()).thenReturn(plugin);
        when(addon.getSettings()).thenReturn(settings);
        when(addon.getHooks()).thenReturn(List.of(hook));
        when(addon.getIslands()).thenReturn(im);
        when(addon.getPlayers()).thenReturn(playersMgr);
        when(hook.getGameMode()).thenReturn(gameMode);
        when(gameMode.inWorld(any(org.bukkit.World.class))).thenReturn(true);
        when(island.getWorld()).thenReturn(world);
        when(im.getIslandById(anyString())).thenReturn(Optional.of(island));

        // Single island in top ten
        when(hook.getAllIslandData()).thenReturn(List.of(
                new IslandBlockData(UUID.randomUUID().toString(), 80, 250, "Underground")));

        tbm = new TopBlockManager(addon);
        when(addon.getManager()).thenReturn(tbm);
        tbm.refreshAll();

        phMgr = new PlaceholderManager(addon);
        phMgr.registerPlaceholders(hook);
        phMgr.updateTopTen();
    }

    @Test
    void testRegisterPlaceholdersRegistersTenOfEach() {
        // 6 placeholders per rank, ranks 1..10, all scoped to the hook's game mode
        verify(phm, times(60)).registerPlaceholder(ArgumentMatchers.eq(gameMode), anyString(),
                ArgumentMatchers.any());
    }

    @Test
    void testGetMemberNamesEmptyForSingleMemberIsland() {
        when(island.getMembers()).thenReturn(java.util.Collections.emptyMap());

        assertEquals("", phMgr.getMemberNames(hook, 1));
    }

    @Test
    void testGetMemberNamesPastEndReturnsEmpty() {
        // Rank 5 with only 1 island in the list → empty
        assertEquals("", phMgr.getMemberNames(hook, 5));
    }

    @Test
    void testGetMemberNamesUnregisteredHookReturnsEmpty() {
        TopBlockHook other = mock(TopBlockHook.class);
        assertEquals("", phMgr.getMemberNames(other, 1));
    }

    @Test
    void testGetMemberNamesJoinsMembers() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        when(island.getMembers()).thenReturn(java.util.Map.of(
                a, RanksManager.MEMBER_RANK,
                b, RanksManager.SUB_OWNER_RANK));
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(a, b));
        when(playersMgr.getName(a)).thenReturn("Alice");
        when(playersMgr.getName(b)).thenReturn("Bob");

        String names = phMgr.getMemberNames(hook, 1);
        // SUB_OWNER_RANK > MEMBER_RANK, so Bob comes first
        assertEquals("Bob,Alice", names);
    }
}
