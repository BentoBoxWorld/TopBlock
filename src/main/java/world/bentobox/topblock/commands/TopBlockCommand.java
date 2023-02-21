package world.bentobox.topblock.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.topblock.TopBlock;
import world.bentobox.topblock.panels.TopLevelPanel;


public class TopBlockCommand extends CompositeCommand {

    private final TopBlock addon;

    public TopBlockCommand(TopBlock addon, CompositeCommand parent) {
        super(parent, "topblock");
        this.addon = addon;
    }

    @Override
    public void setup() {
        setPermission("island.topblock");
        setDescription("island.topblock.description");
        setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, String label, List<String> list) {
        TopLevelPanel.openPanel(this.addon, user, this.getWorld(), this.getPermissionPrefix());
        return true;
    }
}
