package world.bentobox.topblock.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import world.bentobox.chunkblock.ChunkBlock;
import world.bentobox.chunkblock.dataobjects.OneBlockIslands;
import world.bentobox.chunkblock.listeners.BlockListener;
import world.bentobox.chunkblock.oneblocks.OneBlockPhase;
import world.bentobox.chunkblock.oneblocks.OneBlocksManager;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChunkBlockHookTest {

    @Mock
    private ChunkBlock cb;
    @Mock
    private BlockListener bl;
    @Mock
    private OneBlocksManager obm;

    private ChunkBlockHook hook;

    @BeforeEach
    void setUp() {
        when(cb.getBlockListener()).thenReturn(bl);
        when(cb.getOneBlockManager()).thenReturn(obm);
        hook = new ChunkBlockHook(cb);
    }

    @Test
    void testGetGameMode() {
        assertEquals(cb, hook.getGameMode());
    }

    @Test
    void testGetAllIslandDataMapsFields() {
        OneBlockIslands i = new OneBlockIslands("island-id");
        i.setBlockNumber(80);
        i.setLifetime(250L);
        i.setPhaseName("Underground");
        when(bl.getAllIslands()).thenReturn(List.of(i));

        List<IslandBlockData> data = hook.getAllIslandData();
        assertEquals(1, data.size());
        assertEquals("island-id", data.get(0).uniqueId());
        assertEquals(80, data.get(0).blockNumber());
        assertEquals(250L, data.get(0).lifetime());
        assertEquals("Underground", data.get(0).phaseName());
    }

    @Test
    void testGetPhaseCountCountsPhasesStrictlyBelow() {
        TreeMap<Integer, OneBlockPhase> probs = new TreeMap<>();
        probs.put(0, mock(OneBlockPhase.class));
        probs.put(100, mock(OneBlockPhase.class));
        probs.put(1000, mock(OneBlockPhase.class));
        when(obm.getBlockProbs()).thenReturn(probs);

        assertEquals(2, hook.getPhaseCount(500));
        // A phase starting exactly at the block number is not counted
        assertEquals(2, hook.getPhaseCount(1000));
        assertEquals(3, hook.getPhaseCount(1001));
        assertEquals(0, hook.getPhaseCount(0));
    }
}
