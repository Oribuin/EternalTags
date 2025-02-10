package dev.oribuin.eternaltags.command.impl.edit;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.argument.ArgumentHandlers;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;
import dev.oribuin.eternaltags.command.argument.TagsArgumentHandler;
import dev.oribuin.eternaltags.manager.LocaleManager;
import dev.oribuin.eternaltags.manager.TagsManager;
import dev.oribuin.eternaltags.obj.Tag;

public class EditNameCommand extends BaseRoseCommand {

    public EditNameCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, Tag tag, String name) {
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

        tag.setName(name);
        tag.save();
        manager.updateActiveTag(tag);

        StringPlaceholders placeholders = StringPlaceholders.builder()
                .add("tag", manager.getDisplayTag(tag, context.getSender() instanceof Player ? (Player) context.getSender() : null))
                .add("option", "name")
                .add("id", tag.getId())
                .add("name", tag.getName())
                .add("value", name)
                .build();

        locale.sendMessage(context.getSender(), "command-edit-edited", placeholders);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("name")
                .permission("eternaltags.edit")
                .arguments(this.createArguments())
                .build();
    }

    private ArgumentsDefinition createArguments() {
        return ArgumentsDefinition.builder()
                .required("tag", new TagsArgumentHandler())
                .optional("name", ArgumentHandlers.GREEDY_STRING)
                .build();
    }

}
