package xyz.oribuin.eternaltags.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.command.argument.CategoryArgumentHandler;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.gui.menu.CategoryGUI;
import xyz.oribuin.eternaltags.gui.menu.TagsGUI;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.obj.Category;

public class CategoriesCommand extends BaseRoseCommand {

    public CategoriesCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, Category category) {
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

        if (!(context.getSender() instanceof Player player)) {
            locale.sendMessage(context.getSender(), "only-player");
            return;
        }

        if (category == null) {
            // Open the main categories GUI if no specific category is provided
            MenuProvider.get(CategoryGUI.class).open(player);
        } else {
            // Open the tags GUI for the specific category
            MenuProvider.get(TagsGUI.class).open(player, tag -> tag.getCategory() != null && tag.getCategory().equalsIgnoreCase(category.getId()));
        }
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("categories")
                .descriptionKey("command-categories-description")
                .permission("eternaltags.categories")
                .playerOnly(true)
                .arguments(this.createArguments())
                .build();
    }

    private ArgumentsDefinition createArguments() {
        return ArgumentsDefinition.builder()
                .optional("category", new CategoryArgumentHandler())
                .build();
    }
}