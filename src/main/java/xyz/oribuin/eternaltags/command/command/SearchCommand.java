package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.argument.ArgumentHandlers;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.gui.menu.TagsGUI;

public class SearchCommand extends BaseRoseCommand {

    public SearchCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @SuppressWarnings("deprecation")
    @RoseExecutable
    public void execute(CommandContext context, String keyword) {
        MenuProvider.get(TagsGUI.class).open((Player) context.getSender(), tag -> tag.getId().contains(keyword)
                                                                                  || tag.getName().contains(keyword)
                                                                                  || tag.getDescription().contains(keyword)
                                                                                  || ChatColor.stripColor(tag.getTag()).contains(keyword));
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("search")
                .permission("eternaltags.search")
                .descriptionKey("command-search-description")
                .playerOnly(true)
                .build();
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder()
                .required("keyword", ArgumentHandlers.GREEDY_STRING)
                .build();
    }
}
