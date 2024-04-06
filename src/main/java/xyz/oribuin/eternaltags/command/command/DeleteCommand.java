package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.*;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import xyz.oribuin.eternaltags.command.argument.TagsArgumentHandler;
import xyz.oribuin.eternaltags.event.TagDeleteEvent;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

public class DeleteCommand extends BaseRoseCommand {

    public DeleteCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, Tag tag) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        CommandSender sender = context.getSender();

        if (tag == null) {
            locale.sendMessage(sender, "tag-doesnt-exist");
            return;
        }

        final TagDeleteEvent event = new TagDeleteEvent(tag);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        manager.deleteTag(tag);
        locale.sendMessage(sender, "command-delete-deleted", StringPlaceholders.of("tag", manager.getDisplayTag(tag, null)));
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("delete")
                .permission("eternaltags.delete")
                .descriptionKey("command-delete-description")
                .build();
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder()
                .required("tag", new TagsArgumentHandler(this.rosePlugin))
                .build();
    }
}
