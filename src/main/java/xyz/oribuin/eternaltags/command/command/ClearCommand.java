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
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.event.TagUnequipEvent;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

public class ClearCommand extends RoseCommand {

    public ClearCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context, @Optional Player player, @Optional Boolean silent) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        CommandSender sender = context.getSender();

        // Check if the player arg was provided.
        if (player != null) {
            if (!sender.hasPermission("eternaltags.clear.other")) {
                locale.sendMessage(sender, "no-permission");
                return;
            }

            Tag activeTag = manager.getUserTag(player);
            final TagUnequipEvent event = new TagUnequipEvent(player, activeTag);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled())
                return;

            manager.clearTag(player.getUniqueId());
            if (silent != null && !silent) {
                locale.sendMessage(player, "command-clear-cleared");
            }

            locale.sendMessage(sender, "command-clear-cleared-other", StringPlaceholders.single("player", player.getName()));
            return;
        }

        if (!(sender instanceof Player playerSender)) {
            locale.sendMessage(sender, "only-player");
            return;
        }

        Tag activeTag = manager.getUserTag(playerSender);
        final TagUnequipEvent event = new TagUnequipEvent(playerSender, activeTag);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        manager.clearTag(playerSender.getUniqueId());
        locale.sendMessage(sender, "command-clear-cleared");
    }


    @Override
    protected String getDefaultName() {
        return "clear";
    }

    @Override
    public String getDescriptionKey() {
        return "command-clear-description";
    }

    @Override
    public String getRequiredPermission() {
        return "eternaltags.clear";
    }

}
