package xyz.oribuin.eternaltags.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
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
                .descriptionKey("command-categories-description")
                .permission("eternaltags.categories")
                .playerOnly(true)
                .build();
    }

}
