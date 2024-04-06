package xyz.oribuin.eternaltags.command.command.edit;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.argument.ArgumentHandlers;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.command.argument.TagsArgumentHandler;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.ArrayList;
import java.util.List;

public class EditDescriptionCommand extends BaseRoseCommand {

    public EditDescriptionCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, Tag tag, int order, String line) {
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);


        List<String> description = new ArrayList<>(tag.getDescription());


        if (line.equalsIgnoreCase("remove") && description.remove(order) != null) {
            StringPlaceholders placeholders = StringPlaceholders.builder("tag", manager.getDisplayTag(tag, null))
                    .add("id", tag.getId())
                    .add("name", tag.getName())
                    .add("value", order)
                    .build();

            locale.sendMessage(context.getSender(), "command-edit-description-removed", placeholders);
            tag.setDescription(description);
            manager.saveTag(tag);
            manager.updateActiveTag(tag);
            return;
        }


        description.set(order, line);
        tag.setDescription(description);
        manager.saveTag(tag);
        manager.updateActiveTag(tag);

        final StringPlaceholders placeholders = StringPlaceholders.builder()
                .add("tag", manager.getDisplayTag(tag, context.getSender() instanceof Player ? (Player) context.getSender() : null))
                .add("option", "description")
                .add("id", tag.getId())
                .add("name", tag.getName())
                .add("value", "line " + order + " set to " + line)
                .build();

        locale.sendMessage(context.getSender(), "command-edit-edited", placeholders);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("description")
                .playerOnly(false)
                .build();
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder()
                .required("tag", new TagsArgumentHandler(this.rosePlugin))
                .required("order", ArgumentHandlers.INTEGER)
                .required("line", ArgumentHandlers.GREEDY_STRING)
                .build();
    }
}
