package dev.oribuin.eternaltags.command.impl.def;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandInfo;

public class HelpCommand extends dev.rosewood.rosegarden.command.HelpCommand {

    public HelpCommand(RosePlugin rosePlugin, BaseRoseCommand parent, CommandInfo commandInfo) {
        super(rosePlugin, parent, commandInfo);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("help")
                .descriptionKey("command-help-description")
                .permission("eternaltags.help")
                .build();
    }

}
