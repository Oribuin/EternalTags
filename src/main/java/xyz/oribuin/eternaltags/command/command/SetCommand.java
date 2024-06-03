package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.argument.ArgumentHandlers;
import dev.rosewood.rosegarden.command.framework.*;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.command.argument.TagsArgumentHandlers;
import xyz.oribuin.eternaltags.event.TagEquipEvent;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

public class SetCommand extends BaseRoseCommand {

    public SetCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, Tag tag, Player player, Boolean silent) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        CommandSender sender = context.getSender();

        // may need to check if tag == null?
        if (tag == null) {
            locale.sendMessage(sender, "tag-doesnt-exist");
            return;
        }

        // Setting another player's tag
        if (player != null) {
            if (!sender.hasPermission("eternaltags.set.other")) {
                locale.sendMessage(sender, "no-permission");
                return;
            }

            final TagEquipEvent event = new TagEquipEvent(player, tag);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled())
                return;

            manager.setTag(player.getUniqueId(), tag);
            if (silent == null || !silent) {
                locale.sendMessage(player, "command-set-changed", StringPlaceholders.of("tag", manager.getDisplayTag(tag, player)));
            }

            locale.sendMessage(sender, "command-set-changed-other", StringPlaceholders.builder("tag", manager.getDisplayTag(tag, player))
                    .add("player", player.getName())
                    .build());

            return;
        }

        // Setting own tag
        if (!(sender instanceof final Player pl)) {
            locale.sendMessage(sender, "only-player");
            return;
        }

        if (!manager.canUseTag(pl, tag)) {
            locale.sendMessage(pl, "command-set-no-permission");
            return;
        }

        final TagEquipEvent event = new TagEquipEvent(pl, tag);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        manager.setTag(pl.getUniqueId(), tag);
        locale.sendMessage(sender, "command-set-changed", StringPlaceholders.of("tag", manager.getDisplayTag(tag, pl)));
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("set")
                .descriptionKey("command-set-description")
                .permission("eternaltags.set")
                .build();
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder()
                .required("tag", TagsArgumentHandlers.TAG)
                .optional("player", ArgumentHandlers.PLAYER)
                .optional("silent", ArgumentHandlers.BOOLEAN)
                .build();
    }
}
