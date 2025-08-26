package dev.oribuin.eternaltags.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.argument.ArgumentHandlers;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import org.bukkit.entity.Player;
import dev.oribuin.eternaltags.gui.MenuProvider;
import dev.oribuin.eternaltags.gui.menu.TagsGUI;
import dev.oribuin.eternaltags.obj.Tag;

import java.util.function.Predicate;

public class SearchCommand extends BaseRoseCommand {

    public SearchCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, String keyword) {
        MenuProvider.get(TagsGUI.class).open((Player) context.getSender(), this.containsKeyword(keyword));
    }

    /**
     * Predicate to check if a tag contains a keyword
     *
     * @param keyword The keyword to check
     * @return The predicate
     */
    private Predicate<Tag> containsKeyword(String keyword) {
        return tag -> tag.getId().contains(keyword)
                      || tag.getName().contains(keyword)
                      || tag.getDescription().contains(keyword);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("search")
                .descriptionKey("command-search-description")
                .permission("eternaltags.search")
                .playerOnly(true)
                .arguments(this.createArguments())
                .build();
    }

    private ArgumentsDefinition createArguments() {
        return ArgumentsDefinition.builder()
                .required("keyword", ArgumentHandlers.GREEDY_STRING)
                .build();
    }

}
