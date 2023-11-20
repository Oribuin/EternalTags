package xyz.oribuin.eternaltags.command.command.edit;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.RoseSubCommand;
import dev.rosewood.rosegarden.command.framework.annotation.Inject;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.command.framework.types.GreedyString;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.ArrayList;
import java.util.List;

public class EditDescriptionCommand extends RoseSubCommand {

    public EditDescriptionCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(@Inject CommandContext context, Tag tag, int order, GreedyString line) {
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);


        List<String> description = new ArrayList<>(tag.getDescription());


        if (line.get().equalsIgnoreCase("remove") && description.remove(order) != null) {
            StringPlaceholders placeholders = StringPlaceholders.builder("tag", manager.getDisplayTag(tag, null))
                    .addPlaceholder("id", tag.getId())
                    .addPlaceholder("name", tag.getName())
                    .addPlaceholder("value", order)
                    .build();

            locale.sendMessage(context.getSender(), "command-edit-description-removed", placeholders);
            tag.setDescription(description);
            manager.saveTag(tag);
            manager.updateActiveTag(tag);
            return;
        }


        description.set(order, line.get());
        tag.setDescription(description);
        manager.saveTag(tag);
        manager.updateActiveTag(tag);

        final StringPlaceholders placeholders = StringPlaceholders.builder()
                .addPlaceholder("tag", manager.getDisplayTag(tag, context.getSender() instanceof Player ? (Player) context.getSender() : null))
                .addPlaceholder("option", "description")
                .addPlaceholder("id", tag.getId())
                .addPlaceholder("name", tag.getName())
                .addPlaceholder("value", "line " + order + " set to " + line.get())
                .build();

        locale.sendMessage(context.getSender(), "command-edit-edited", placeholders);
    }

    @Override
    protected String getDefaultName() {
        return "description";
    }

    @Override
    public boolean isPlayerOnly() {
        return false;
    }

}
