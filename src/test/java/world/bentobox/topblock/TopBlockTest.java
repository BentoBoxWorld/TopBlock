package world.bentobox.topblock;

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

import world.bentobox.bentobox.api.addons.Addon.State;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.topblock.config.ConfigSettings;

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

        // AddonsManager — no aoneblock present
        when(plugin.getAddonsManager()).thenReturn(am);
        when(am.getGameModeAddons()).thenReturn(Collections.emptyList());
        when(am.getAddonByName("aoneblock")).thenReturn(Optional.empty());

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
    void testOnEnableWithoutAOneBlockDisables() {
        addon.onLoad();
        addon.onEnable();
        // AOneBlock not present → addon disables itself
        assertTrue(addon.getState() == State.DISABLED);
        // Manager is still constructed before the AOneBlock lookup
        assertNotNull(addon.getManager());
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
