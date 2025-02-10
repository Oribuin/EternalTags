package dev.oribuin.eternaltags.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.oribuin.eternaltags.gui.MenuProvider;
import dev.oribuin.eternaltags.manager.LocaleManager;

public class ReloadCommand extends BaseRoseCommand {

    public ReloadCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        MenuProvider.reload(); // Reload all menus
        this.rosePlugin.reload();
        this.rosePlugin.getManager(LocaleManager.class).sendCommandMessage(context.getSender(), "command-reload-reloaded");
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("reload")
                .descriptionKey("command-reload-description")
                .permission("eternaltags.reload")
                .build();
    }

}
