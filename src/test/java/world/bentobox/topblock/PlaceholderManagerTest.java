package world.bentobox.topblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.google.common.collect.ImmutableSet;

import world.bentobox.aoneblock.AOneBlock;
import world.bentobox.aoneblock.dataobjects.OneBlockIslands;
import world.bentobox.aoneblock.listeners.BlockListener;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.topblock.config.ConfigSettings;

class PlaceholderManagerTest extends CommonTestSetup {

    @Mock
    private TopBlock addon;
    @Mock
    private AOneBlock aob;
    @Mock
    private BlockListener bl;
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
        when(addon.getaOneBlock()).thenReturn(aob);
        when(addon.getIslands()).thenReturn(im);
        when(addon.getPlayers()).thenReturn(playersMgr);
        when(aob.getBlockListener()).thenReturn(bl);
        when(im.getIslandById(anyString())).thenReturn(Optional.of(island));

        // Single island in top ten
        OneBlockIslands ob = new OneBlockIslands(UUID.randomUUID().toString());
        ob.setBlockNumber(80);
        ob.setLifetime(250);
        ob.setPhaseName("Underground");
        when(bl.getAllIslands()).thenReturn(List.of(ob));

        tbm = new TopBlockManager(addon);
        when(addon.getManager()).thenReturn(tbm);
        tbm.getOneBlockData();

        phMgr = new PlaceholderManager(addon);
        phMgr.updateTopTen();
    }

    @Test
    void testGetMemberNamesEmptyForSingleMemberIsland() {
        when(island.getMembers()).thenReturn(java.util.Collections.emptyMap());

        assertEquals("", phMgr.getMemberNames(1));
    }

    @Test
    void testGetMemberNamesPastEndReturnsEmpty() {
        // Rank 5 with only 1 island in the list → empty
        assertEquals("", phMgr.getMemberNames(5));
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

        String names = phMgr.getMemberNames(1);
        // SUB_OWNER_RANK > MEMBER_RANK, so Bob comes first
        assertEquals("Bob,Alice", names);
    }
}
