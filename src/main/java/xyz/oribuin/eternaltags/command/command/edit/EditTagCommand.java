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

public class EditTagCommand extends RoseSubCommand {

    public EditTagCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(@Inject CommandContext context, Tag tagId, GreedyString newTag) {
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

        tagId.setTag(newTag.get());
        manager.saveTag(tagId);
        manager.updateActiveTag(tagId);

        final StringPlaceholders placeholders = StringPlaceholders.builder()
                .addPlaceholder("tag", manager.getDisplayTag(tagId, context.getSender() instanceof Player ? (Player) context.getSender() : null))
                .addPlaceholder("option", "tag")
                .addPlaceholder("id", tagId.getId())
                .addPlaceholder("name", tagId.getName())
                .addPlaceholder("value", newTag.get())
                .build();

        locale.sendMessage(context.getSender(), "command-edit-edited", placeholders);
    }

    @Override
    protected String getDefaultName() {
        return "tag";
    }

    @Override
    public boolean isPlayerOnly() {
        return false;
    }

}
