package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.Optional;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

public class SetAllCommand extends RoseCommand {

    public SetAllCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context, Tag tag, @Optional Boolean silent) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        CommandSender sender = context.getSender();

        // may need to check if tag == null?
        if (tag == null) {
            locale.sendMessage(sender, "tag-doesnt-exist");
            return;
        }

        manager.setEveryone(tag);
        if (silent == null || !silent) {
            Bukkit.getOnlinePlayers().forEach(player -> locale.sendMessage(player, "command-set-changed", StringPlaceholders.of("tag", manager.getDisplayTag(tag, player))));
        }

        locale.sendMessage(sender, "command-setall-changed", StringPlaceholders.of("tag", manager.getDisplayTag(tag, null)));
    }


    @Override
    protected String getDefaultName() {
        return "setall";
    }

    @Override
    public String getDescriptionKey() {
        return "command-setall-description";
    }

    @Override
    public String getRequiredPermission() {
        return "eternaltags.setall";
    }

}
