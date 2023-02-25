package world.bentobox.topblock;

import world.bentobox.aoneblock.AOneBlock;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.Config;
import world.bentobox.topblock.commands.TopBlockCommand;
import world.bentobox.topblock.config.ConfigSettings;


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
    private Addon aOneBlock;

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

        // Find AOneBlock
        getPlugin().getAddonsManager().getAddonByName("aoneblock")
        .filter(gm -> gm.isEnabled())
        .filter(GameModeAddon.class::isInstance)
        .map(GameModeAddon.class::cast).ifPresentOrElse(gm -> {
            log("TopBlock hooking into AOneBlock");
            registerCommands(gm);
            aOneBlock = gm;
        }, () -> {
            logError("Could not hook into AOneBlock. Is it loaded?");
            this.setState(State.DISABLED);
        });
    }

    private void registerCommands(GameModeAddon gm) {
        gm.getPlayerCommand().ifPresent(playerCmd -> {
            new TopBlockCommand(this, playerCmd);
        });
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

    public AOneBlock getaOneBlock() {
        return (AOneBlock) aOneBlock;
    }


}
