package world.bentobox.topblock;


import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.Pladdon;


/**
 * @author tastybento
 *
 */
public class TopBlockPladdon extends Pladdon {
    @Override
    public Addon getAddon() {
        return new TopBlock();
    }
}
