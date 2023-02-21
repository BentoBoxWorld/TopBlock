package world.bentobox.topblock.config;

import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.configuration.StoreAt;

@StoreAt(filename="config.yml", path="addons/TopBlock")
@ConfigComment("TopBlock Configuration [version]")
@ConfigComment("")
public class ConfigSettings implements ConfigObject {
    @ConfigComment("How often the Top Ten should be refreshed in minutes. Minimum is 1 minute, default is 5.")
    @ConfigComment("Each refresh requires reading every island from the database, so this should not be done too often.")
    @ConfigEntry(path = "refresh-time")
    private int refreshTime = 5;
    
    @ConfigComment("Shorthand island counts")
    @ConfigComment("Shows large values using units, e.g., 10,345 -> 10k")
    @ConfigEntry(path = "shorthand")
    private boolean shorthand = false;

    public boolean isShorthand() {
        return shorthand;
    }

    public void setShorthand(boolean shorthand) {
        this.shorthand = shorthand;
    }

    public int getRefreshTime() {
        if (refreshTime <= 0) {
            refreshTime = 1;
        }
        return refreshTime;
    }

    public void setRefreshTime(int refreshTime) {
        if (refreshTime <= 0) {
            refreshTime = 1;
        }
        this.refreshTime = refreshTime;
    }

    
    
}
