package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.*;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.gui.menu.CategoryGUI;

public class CategoriesCommand extends BaseRoseCommand {

    public CategoriesCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        MenuProvider.get(CategoryGUI.class).open((Player) context.getSender());
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("categories")
                .permission("eternaltags.categories")
                .descriptionKey("command-categories-description")
                .playerOnly(true)
                .build();
    }

}
