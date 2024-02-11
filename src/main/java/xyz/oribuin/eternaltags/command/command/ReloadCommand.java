package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.listener.BungeeListener;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.manager.LocaleManager;

public class ReloadCommand extends RoseCommand {

    public ReloadCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        if (Setting.PLUGIN_MESSAGING_RELOAD.getBoolean()) {
            BungeeListener.sendReload(); // Send reload message to BungeeCord
        }

        MenuProvider.reload(); // Reload all menus
        this.rosePlugin.reload();
        this.rosePlugin.getManager(LocaleManager.class).sendCommandMessage(context.getSender(), "command-reload-reloaded");
    }

    @Override
    protected String getDefaultName() {
        return "reload";
    }

    @Override
    public String getDescriptionKey() {
        return "command-reload-description";
    }

    @Override
    public String getRequiredPermission() {
        return "eternaltags.reload";
    }

}
