package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.command.framework.types.GreedyString;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.command.CommandSender;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.Collections;

public class CreateCommand extends RoseCommand {

    public CreateCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context, String name, GreedyString tag) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        CommandSender sender = context.getSender();

        if (manager.checkTagExists(name)) {
            locale.sendMessage(sender, "command-create-tag-exists");
            return;
        }

        final String id = name.toLowerCase().replace(".", "_");

        Tag newTag = new Tag(id, name, tag.get());
        newTag.setDescription(Collections.singletonList("None"));

        if (manager.saveTag(newTag)) {
            locale.sendMessage(sender, "command-create-created", StringPlaceholders.of("tag", manager.getDisplayTag(newTag, null)));
        }
    }


    @Override
    protected String getDefaultName() {
        return "create";
    }

    @Override
    public String getDescriptionKey() {
        return "command-create-description";
    }

    @Override
    public String getRequiredPermission() {
        return "eternaltags.create";
    }

}
