package xyz.oribuin.eternaltags.command.command.edit;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.RoseSubCommand;
import dev.rosewood.rosegarden.command.framework.annotation.Inject;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Category;
import xyz.oribuin.eternaltags.obj.Tag;

public class EditCategoryCommand extends RoseSubCommand {

    public EditCategoryCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(@Inject CommandContext context, Tag tag, Category newCategory) {
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

        tag.setCategory(newCategory.getId());
        manager.saveTag(tag);
        manager.updateActiveTag(tag);

        final StringPlaceholders placeholders = StringPlaceholders.builder()
                .addPlaceholder("tag", manager.getDisplayTag(tag, context.getSender() instanceof Player ? (Player) context.getSender() : null))
                .addPlaceholder("option", "category")
                .addPlaceholder("id", tag.getId())
                .addPlaceholder("name", tag.getName())
                .addPlaceholder("value", newCategory.getId())
                .build();

        locale.sendMessage(context.getSender(), "command-edit-edited", placeholders);
    }

    @Override
    protected String getDefaultName() {
        return "category";
    }

    @Override
    public boolean isPlayerOnly() {
        return false;
    }

}
