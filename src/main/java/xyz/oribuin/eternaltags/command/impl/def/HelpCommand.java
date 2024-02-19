package xyz.oribuin.eternaltags.command.impl.def;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandInfo;

public class HelpCommand extends dev.rosewood.rosegarden.command.HelpCommand {

    public HelpCommand(RosePlugin rosePlugin, BaseRoseCommand parent) {
        super(rosePlugin, parent);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("help")
                .descriptionKey("command-help-description")
                .permission("eternaltags.help")
                .build();
    }

}
