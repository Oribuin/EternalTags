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
import xyz.oribuin.eternaltags.command.argument.TagsArgumentHandlers;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

public class EditPermissionCommand extends BaseRoseCommand {

    public EditPermissionCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, Tag tag, String permission) {
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

        tag.setPermission(permission);
        manager.saveTag(tag);
        manager.updateActiveTag(tag);

        final StringPlaceholders placeholders = StringPlaceholders.builder()
                .add("tag", manager.getDisplayTag(tag, context.getSender() instanceof Player ? (Player) context.getSender() : null))
                .add("option", "permission")
                .add("id", tag.getId())
                .add("name", tag.getName())
                .add("value", permission)
                .build();

        locale.sendMessage(context.getSender(), "command-edit-edited", placeholders);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("permission")
                .playerOnly(false)
                .build();
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder()
                .required("tag", TagsArgumentHandlers.TAG)
                .required("permission", ArgumentHandlers.GREEDY_STRING)
                .build();
    }
}
