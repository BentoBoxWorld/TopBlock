package world.bentobox.topblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import world.bentobox.aoneblock.AOneBlock;
import world.bentobox.bentobox.api.addons.Addon.State;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.chunkblock.ChunkBlock;
import world.bentobox.topblock.config.ConfigSettings;
import world.bentobox.topblock.hooks.AOneBlockHook;
import world.bentobox.topblock.hooks.ChunkBlockHook;

class TopBlockTest extends CommonTestSetup {

    private static final String CONFIG_YML =
            """
                    refresh-time: 5
                    shorthand: false
                    """;

    private static final String TOP_PANEL_YML = "top_panel:\n  type: INVENTORY\n";

    @Mock
    private AddonsManager am;

    private TopBlock addon;
    private MockedStatic<DatabaseSetup> mockDb;

    @SuppressWarnings("unchecked")
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Database mock
        AbstractDatabaseHandler<Object> h = mock(AbstractDatabaseHandler.class);
        mockDb = Mockito.mockStatic(DatabaseSetup.class);
        DatabaseSetup dbSetup = mock(DatabaseSetup.class);
        mockDb.when(DatabaseSetup::getDatabase).thenReturn(dbSetup);
        when(dbSetup.getHandler(any())).thenReturn(h);
        when(h.saveObject(any())).thenReturn(CompletableFuture.completedFuture(true));

        // CommandsManager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // AddonsManager — no game modes present by default
        when(plugin.getAddonsManager()).thenReturn(am);
        when(am.getGameModeAddons()).thenReturn(Collections.emptyList());
        when(am.getAddonByName("aoneblock")).thenReturn(Optional.empty());
        when(am.getAddonByName("chunkblock")).thenReturn(Optional.empty());

        // FlagsManager
        when(plugin.getFlagsManager()).thenReturn(fm);
        when(fm.getFlags()).thenReturn(Collections.emptyList());

        // Build a JAR with config.yml + the panel resource that onLoad copies
        addon = new TopBlock();
        File jFile = new File("addon.jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jFile))) {
            addJarEntry(jos, "config.yml", CONFIG_YML);
            addJarEntry(jos, "panels/top_panel.yml", TOP_PANEL_YML);
        }
        addon.setDataFolder(new File("addons/TopBlock"));
        addon.setFile(jFile);
        addon.setDescription(new AddonDescription.Builder("bentobox", "TopBlock", "1.0.0")
                .description("test").authors("tastybento").build());
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        if (mockDb != null) {
            mockDb.closeOnDemand();
        }
        super.tearDown();
        new File("addon.jar").delete();
        deleteAll(new File("addons"));
    }

    private static void addJarEntry(JarOutputStream jos, String name, String content) throws Exception {
        JarEntry entry = new JarEntry(name);
        jos.putNextEntry(entry);
        jos.write(content.getBytes(StandardCharsets.UTF_8));
        jos.closeEntry();
    }

    private AOneBlock mockAOneBlock() {
        AOneBlock aob = mock(AOneBlock.class);
        when(aob.isEnabled()).thenReturn(true);
        when(aob.getPlayerCommand()).thenReturn(Optional.empty());
        when(am.getAddonByName("aoneblock")).thenReturn(Optional.of(aob));
        return aob;
    }

    private ChunkBlock mockChunkBlock() {
        ChunkBlock cb = mock(ChunkBlock.class);
        when(cb.isEnabled()).thenReturn(true);
        when(cb.getPlayerCommand()).thenReturn(Optional.empty());
        when(am.getAddonByName("chunkblock")).thenReturn(Optional.of(cb));
        return cb;
    }

    @Test
    void testGetSettingsNullBeforeLoad() {
        assertNull(addon.getSettings());
    }

    @Test
    void testOnLoad() {
        addon.onLoad();
        assertNotNull(addon.getSettings());
    }

    @Test
    void testOnLoadSettingsDefaults() {
        addon.onLoad();
        ConfigSettings s = addon.getSettings();
        assertNotNull(s);
        // refresh-time clamps to >=1; default in YAML is 5
        org.junit.jupiter.api.Assertions.assertEquals(5, s.getRefreshTime());
        org.junit.jupiter.api.Assertions.assertFalse(s.isShorthand());
    }

    @Test
    void testOnEnableWithoutGameModesDisables() {
        loadAndEnable();
        // Neither AOneBlock nor ChunkBlock present → addon disables itself
        assertTrue(addon.getState() == State.DISABLED);
        assertTrue(addon.getHooks().isEmpty());
        // Manager is still constructed before the game mode lookup
        assertNotNull(addon.getManager());
    }

    /**
     * A freshly constructed Addon starts in DISABLED state and only AddonsManager
     * flips it to ENABLED, so mark it LOADED first — then a DISABLED state after
     * onEnable can only mean the addon disabled itself.
     */
    private void loadAndEnable() {
        addon.onLoad();
        addon.setState(State.LOADED);
        addon.onEnable();
    }

    @Test
    void testOnEnableWithAOneBlockOnly() {
        AOneBlock aob = mockAOneBlock();
        loadAndEnable();
        assertNotEquals(State.DISABLED, addon.getState());
        assertEquals(1, addon.getHooks().size());
        assertInstanceOf(AOneBlockHook.class, addon.getHooks().get(0));
        assertEquals(aob, addon.getHooks().get(0).getGameMode());
    }

    @Test
    void testOnEnableWithChunkBlockOnly() {
        ChunkBlock cb = mockChunkBlock();
        loadAndEnable();
        assertNotEquals(State.DISABLED, addon.getState());
        assertEquals(1, addon.getHooks().size());
        assertInstanceOf(ChunkBlockHook.class, addon.getHooks().get(0));
        assertEquals(cb, addon.getHooks().get(0).getGameMode());
    }

    @Test
    void testOnEnableWithBothGameModes() {
        mockAOneBlock();
        mockChunkBlock();
        loadAndEnable();
        assertNotEquals(State.DISABLED, addon.getState());
        assertEquals(2, addon.getHooks().size());
        assertInstanceOf(AOneBlockHook.class, addon.getHooks().get(0));
        assertInstanceOf(ChunkBlockHook.class, addon.getHooks().get(1));
    }

    @Test
    void testOnEnableSkipsDisabledGameMode() {
        AOneBlock aob = mockAOneBlock();
        when(aob.isEnabled()).thenReturn(false);
        loadAndEnable();
        assertTrue(addon.getState() == State.DISABLED);
        assertTrue(addon.getHooks().isEmpty());
    }

    @Test
    void testGetHookByWorld() {
        AOneBlock aob = mockAOneBlock();
        when(aob.inWorld(world)).thenReturn(true);
        loadAndEnable();
        assertTrue(addon.getHook(world).isPresent());
        assertEquals(aob, addon.getHook(world).get().getGameMode());
    }

    @Test
    void testGetHookByWorldNoMatch() {
        AOneBlock aob = mockAOneBlock();
        when(aob.inWorld(world)).thenReturn(false);
        loadAndEnable();
        assertTrue(addon.getHook(world).isEmpty());
    }

    @Test
    void testOnDisable() {
        addon.onDisable();
        assertNotNull(addon);
    }

    @Test
    void testOnReload() {
        addon.onLoad();
        addon.onReload();
        assertNotNull(addon.getSettings());
    }
}
