package xyz.oribuin.eternaltags.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import xyz.oribuin.eternaltags.config.Setting;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.listener.BungeeListener;
import xyz.oribuin.eternaltags.manager.LocaleManager;

public class ReloadCommand extends BaseRoseCommand {

    public ReloadCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        if (Setting.PLUGIN_MESSAGING_RELOAD.get()) {
            BungeeListener.sendReload(); // Send reload message to BungeeCord
        }

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
