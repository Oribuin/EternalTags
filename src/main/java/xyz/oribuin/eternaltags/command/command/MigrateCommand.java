package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import xyz.oribuin.eternaltags.command.model.DataStorageType;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.HashMap;
import java.util.Map;

public class MigrateCommand extends RoseCommand {

    public MigrateCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context, DataStorageType from, DataStorageType to) {

        if (from == to) {
            this.rosePlugin.getManager(LocaleManager.class).sendMessage(context.getSender(), "command-migrate-matching");
            return;
        }

        // Get all tags from the current storage type
        this.rosePlugin.getManager(TagsManager.class).getCachedTags().clear();

        Map<String, Tag> result = new HashMap<>(this.rosePlugin.getManager(TagsManager.class).loadTags(from));
        this.rosePlugin.getManager(TagsManager.class).saveTags(to, result);
        this.rosePlugin.getManager(LocaleManager.class).sendMessage(context.getSender(), "command-migrate-success", StringPlaceholders.of(
                "from", from.name(),
                "to", to.name()
        ));

    }

    @Override
    protected String getDefaultName() {
        return "migrate";
    }

    @Override
    public String getDescriptionKey() {
        return "command-migrate-description";
    }

    @Override
    public String getRequiredPermission() {
        return "eternaltags.command.migrate";
    }

}
