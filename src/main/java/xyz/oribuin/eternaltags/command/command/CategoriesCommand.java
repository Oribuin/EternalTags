package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.gui.menu.CategoryGUI;

public class CategoriesCommand extends RoseCommand {

    public CategoriesCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        MenuProvider.get(CategoryGUI.class).open((Player) context.getSender());
    }

    @Override
    protected String getDefaultName() {
        return "categories";
    }

    @Override
    public String getDescriptionKey() {
        return "command-categories-description";
    }

    @Override
    public String getRequiredPermission() {
        return "eternaltags.categories";
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

}
