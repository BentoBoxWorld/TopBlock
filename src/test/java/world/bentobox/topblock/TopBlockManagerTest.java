package world.bentobox.topblock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.aoneblock.AOneBlock;
import world.bentobox.aoneblock.dataobjects.OneBlockIslands;
import world.bentobox.aoneblock.listeners.BlockListener;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.topblock.TopBlockManager.TopTenData;
import world.bentobox.topblock.config.ConfigSettings;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
public class TopBlockManagerTest {

    @Mock
    private TopBlock addon;
    @Mock
    private Island island;

    private TopBlockManager tbm;
    @Mock
    private AOneBlock aob;
    @Mock
    private BlockListener bl;
    @Mock
    private IslandsManager im;


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        List<OneBlockIslands> list = new ArrayList<>();
        OneBlockIslands i = new OneBlockIslands(UUID.randomUUID().toString());
        i.setLifetime(100);
        i.setBlockNumber(100);
        i.setPhaseName("phasy");
        list.add(i);

        // Island manager
        when(addon.getIslands()).thenReturn(im);
        when(im.getIslandById(anyString())).thenReturn(Optional.of(island));
        // AOneBlock
        when(bl.getAllIslands()).thenReturn(list);
        when(aob.getBlockListener()).thenReturn(bl);
        when(addon.getaOneBlock()).thenReturn(aob);
        tbm = new TopBlockManager(addon);
    }

    /**
     * Test method for {@link world.bentobox.topblock.TopBlockManager#TopBlockManager(world.bentobox.topblock.TopBlock)}.
     */
    @Test
    public void testTopBlockManager() {
        assertNotNull(tbm);
    }

    /**
     * Test method for {@link world.bentobox.topblock.TopBlockManager.TopTenData}.
     */
    @Test
    public void testTopTenDataSame() {
        TopTenData ttd = new TopTenData(island, 0, 0, "phase one");
        TopTenData ttd2 = new TopTenData(island, 0, 0, "phase one");
        assertEquals(ttd, ttd2);
    }

    /**
     * Test method for {@link world.bentobox.topblock.TopBlockManager.TopTenData}.
     */
    @Test
    public void testTopTenDataBlockDifferent() {
        TopTenData ttd = new TopTenData(island, 1000, 0, "phase one");
        TopTenData ttd2 = new TopTenData(island, 0, 0, "phase one");
        assertNotEquals(ttd, ttd2);
    }

    /**
     * Test method for {@link world.bentobox.topblock.TopBlockManager.TopTenData}.
     */
    @Test
    public void testTopTenDataLifetimeDifferent() {
        TopTenData ttd = new TopTenData(island, 0, 0, "phase one");
        TopTenData ttd2 = new TopTenData(island, 0, 10000, "phase one");
        assertNotEquals(ttd, ttd2);
    }

    /**
     * Test method for {@link world.bentobox.topblock.TopBlockManager.TopTenData}.
     */
    @Test
    public void testTopTenDataPhaseDifferent() {
        TopTenData ttd = new TopTenData(island, 0, 0, "phase one");
        TopTenData ttd2 = new TopTenData(island, 0, 0, "phase two");
        assertNotEquals(ttd, ttd2);
    }

    /**
     * Test method for {@link world.bentobox.topblock.TopBlockManager.TopTenData}.
     */
    @Test
    public void testTopTenDataGreater() {
        TopTenData ttd = new TopTenData(island, 10000, 0, "phase fifty");
        TopTenData ttd2 = new TopTenData(island, 0, 0, "phase two");
        List<TopTenData> list = new ArrayList<>();
        list.add(ttd);
        list.add(ttd2);
        list = list.stream().sorted(Collections.reverseOrder()).toList();
        assertEquals(ttd, list.get(0));
        assertEquals(ttd2, list.get(1));
    }

    /**
     * Test method for {@link world.bentobox.topblock.TopBlockManager.TopTenData}.
     */
    @Test
    public void testTopTenDataLess() {
        TopTenData ttd = new TopTenData(island, 0, 0, "phase one");
        TopTenData ttd2 = new TopTenData(island, 10000, 0, "phase fifty");
        List<TopTenData> list = new ArrayList<>();
        list.add(ttd);
        list.add(ttd2);
        list = list.stream().sorted(Collections.reverseOrder()).toList();
        assertEquals(ttd2, list.get(0));
        assertEquals(ttd, list.get(1));
    }

    /**
     * Test method for {@link world.bentobox.topblock.TopBlockManager.TopTenData}.
     */
    @Test
    public void testTopTenDataGreaterLifetime() {
        TopTenData ttd = new TopTenData(island, 100, 10100, "phase fifty");
        TopTenData ttd2 = new TopTenData(island, 1000, 0, "phase two");
        List<TopTenData> list = new ArrayList<>();
        list.add(ttd);
        list.add(ttd2);
        list = list.stream().sorted(Collections.reverseOrder()).toList();
        assertEquals(ttd, list.get(0));
        assertEquals(ttd2, list.get(1));
    }

    /**
     * Test method for {@link world.bentobox.topblock.TopBlockManager.TopTenData}.
     */
    @Test
    public void testTopTenDataGreaterLifetime2() {
        TopTenData ttd = new TopTenData(island, 100, 10100, "phase fifty");
        TopTenData ttd2 = new TopTenData(island, 100, 0, "phase two");
        List<TopTenData> list = new ArrayList<>();
        list.add(ttd2);
        list.add(ttd);
        list = list.stream().sorted(Collections.reverseOrder()).toList();
        assertEquals(ttd, list.get(0));
        assertEquals(ttd2, list.get(1));
    }


    /**
     * Test method for {@link world.bentobox.topblock.TopBlockManager#getOneBlockData()}.
     */
    @Test
    public void testGetOneBlockData() {
        this.tbm.getOneBlockData();
        @NonNull
        List<TopTenData> list = tbm.getTopTen(10);
        TopTenData t = list.get(0);
        assertEquals(100, t.lifetime());
        assertEquals(100, t.blockNumber());
        assertEquals("phasy", t.phaseName());

    }

    /**
     * Test method for {@link world.bentobox.topblock.TopBlockManager#formatLevel(java.lang.Long)}.
     */
    @Test
    public void testFormatLevel() {
        ConfigSettings settings = new ConfigSettings();
        settings.setShorthand(true);
        when(addon.getSettings()).thenReturn(settings);
        assertEquals("12.3G", tbm.formatLevel(12345678349L));
        settings.setShorthand(false);
        when(addon.getSettings()).thenReturn(settings);
        assertEquals("12345678349", tbm.formatLevel(12345678349L));
    }

    /**
     * Test method for {@link world.bentobox.topblock.TopBlockManager#getTopTen(int)}.
     */
    @Test
    public void testGetTopTen() {
        List<TopTenData> list = tbm.getTopTen(10);
        assertTrue(list.isEmpty());
    }

}
