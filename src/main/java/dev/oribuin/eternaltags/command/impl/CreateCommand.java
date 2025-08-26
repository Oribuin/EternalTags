package dev.oribuin.eternaltags.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.argument.ArgumentHandlers;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.command.CommandSender;
import dev.oribuin.eternaltags.manager.LocaleManager;
import dev.oribuin.eternaltags.manager.TagsManager;
import dev.oribuin.eternaltags.obj.Tag;

import java.util.Collections;

public class CreateCommand extends BaseRoseCommand {

    public CreateCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, String name, String tag) {
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        CommandSender sender = context.getSender();

        if (manager.checkTagExists(name)) {
            locale.sendMessage(sender, "command-create-tag-exists");
            return;
        }

        String id = name.toLowerCase().replace(".", "_");

        Tag newTag = new Tag(id, name, tag);
        newTag.setDescription(Collections.singletonList("None"));
        newTag.save();

        locale.sendMessage(sender, "command-create-created", StringPlaceholders.of("tag", manager.getDisplayTag(newTag, null)));
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("create")
                .descriptionKey("command-create-description")
                .permission("eternaltags.create")
                .arguments(this.createArguments())
                .build();
    }

    private ArgumentsDefinition createArguments() {
        return ArgumentsDefinition.builder()
                .required("name", ArgumentHandlers.STRING)
                .required("tag", ArgumentHandlers.GREEDY_STRING)
                .build();
    }

}
