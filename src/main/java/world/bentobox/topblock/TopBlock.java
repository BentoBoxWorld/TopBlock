package world.bentobox.topblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.bukkit.World;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.topblock.commands.TopBlockCommand;
import world.bentobox.topblock.config.ConfigSettings;
import world.bentobox.topblock.hooks.AOneBlockHook;
import world.bentobox.topblock.hooks.ChunkBlockHook;
import world.bentobox.topblock.hooks.TopBlockHook;


/**
 * @author tastybento
 *
 */
public class TopBlock extends Addon {

    // The 10 in top ten
    public static final int TEN = 10;

    // Settings
    private ConfigSettings settings;
    private Config<ConfigSettings> configObject = new Config<>(this, ConfigSettings.class);
    private TopBlockManager manager;
    // Hooks into the game modes this addon supports; each keeps its own top ten
    private final List<TopBlockHook> hooks = new ArrayList<>();

    @Override
    public void onLoad() {
        // Save the default config from config.yml
        saveDefaultConfig();
        if (loadSettings()) {
            // Disable
            logError("TopBlock settings could not load! Addon disabled.");
            setState(State.DISABLED);
            return;
        } else {
            configObject.saveConfigObject(settings);
        }

        // Save existing panels.
        this.saveResource("panels/top_panel.yml", false);
    }

    private boolean loadSettings() {
        // Load settings again to get worlds
        settings = configObject.loadConfigObject();

        return settings == null;
    }

    @Override
    public void onEnable() {
        // Start Manager
        manager = new TopBlockManager(this);
        this.registerListener(manager);

        // Hook into whichever supported game modes are present. Game-mode classes are
        // only referenced inside the hook constructors, which run after the presence
        // check, so a game mode that is not installed is never class-loaded.
        findGameMode("aoneblock").ifPresent(gm -> hook(gm, "AOneBlock", () -> new AOneBlockHook(gm)));
        findGameMode("chunkblock").ifPresent(gm -> hook(gm, "ChunkBlock", () -> new ChunkBlockHook(gm)));

        if (hooks.isEmpty()) {
            logError("Could not hook into AOneBlock or ChunkBlock. Is at least one loaded?");
            this.setState(State.DISABLED);
        }
    }

    /**
     * Add a hook for a game mode that the BentoBox API says is present and enabled.
     * The game mode's classes still have to be visible to this addon's class loader, which
     * is not guaranteed - e.g. if the game mode is installed as a BentoBox addon jar while
     * TopBlock is loaded as a Pladdon plugin. If they are not, skip that game mode rather
     * than letting the LinkageError mark the whole addon as incompatible.
     * @param gm game mode addon
     * @param name friendly name of the game mode, for logging
     * @param hookSupplier supplier of the hook. Must be a lambda so that the game mode's
     *        classes are only loaded when it is called.
     */
    private void hook(GameModeAddon gm, String name, Supplier<TopBlockHook> hookSupplier) {
        try {
            TopBlockHook hook = hookSupplier.get();
            registerCommands(gm);
            hooks.add(hook);
            log("TopBlock hooking into " + name);
        } catch (LinkageError e) {
            logError(name + " is loaded, but TopBlock cannot see its classes: " + e.getMessage());
            logError("Install " + name + " as a plugin (jar in the plugins folder) so that TopBlock can hook into it.");
        }
    }

    private Optional<GameModeAddon> findGameMode(String name) {
        return getPlugin().getAddonsManager().getAddonByName(name)
                .filter(Addon::isEnabled)
                .filter(GameModeAddon.class::isInstance)
                .map(GameModeAddon.class::cast);
    }

    private void registerCommands(GameModeAddon gm) {
        gm.getPlayerCommand().ifPresent(playerCmd -> new TopBlockCommand(this, playerCmd));
    }


    /**
     * @return the settings
     */
    public ConfigSettings getSettings() {
        return settings;
    }

    /**
     * @return the manager
     */
    public TopBlockManager getManager() {
        return manager;
    }

    /**
     * Set the config settings - used for tests only
     * @param configSettings - config settings
     */
    void setSettings(ConfigSettings configSettings) {
        this.settings = configSettings;

    }

    @Override
    public void onDisable() {
        // Do nothing

    }

    /**
     * @return the game mode hooks that are active
     */
    public List<TopBlockHook> getHooks() {
        return hooks;
    }

    /**
     * Get the hook whose game mode owns the given world, if any.
     * @param world world to look up
     * @return hook for that world's game mode
     */
    public Optional<TopBlockHook> getHook(World world) {
        return hooks.stream().filter(h -> h.getGameMode().inWorld(world)).findFirst();
    }


}
