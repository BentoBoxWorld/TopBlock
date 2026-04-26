package world.bentobox.topblock;


import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.Pladdon;


/**
 * @author tastybento
 *
 */
public class TopBlockPladdon extends Pladdon {

    private Addon addon;

    @Override
    public Addon getAddon() {
        if (addon == null) {
            addon = new TopBlock();
        }
        return addon;
    }
}
