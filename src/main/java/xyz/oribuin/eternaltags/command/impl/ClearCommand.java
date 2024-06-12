package xyz.oribuin.eternaltags.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.argument.ArgumentHandlers;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;

public class ClearCommand extends BaseRoseCommand {

    public ClearCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, Player target, String silent) {
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        CommandSender sender = context.getSender();

        // Check if the player arg was provided.
        if (target != null) {
            if (!sender.hasPermission("eternaltags.clear.other")) {
                locale.sendMessage(sender, "no-permission");
                return;
            }

            manager.clearTag(target.getUniqueId());

            if (silent != null)
                locale.sendMessage(target, "command-clear-cleared");

            locale.sendMessage(sender, "command-clear-cleared-other", StringPlaceholders.of("player", target.getName()));
            return;
        }

        if (!(sender instanceof Player playerSender)) {
            locale.sendMessage(sender, "only-player");
            return;
        }

        manager.clearTag(playerSender.getUniqueId());
        locale.sendMessage(sender, "command-clear-cleared");
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("clear")
                .descriptionKey("command-clear-description")
                .permission("eternaltags.clear")
                .arguments(this.createArguments())
                .build();
    }

    private ArgumentsDefinition createArguments() {
        return ArgumentsDefinition.builder()
                .optional("target", ArgumentHandlers.PLAYER)
                .optional("silent", ArgumentHandlers.BOOLEAN)
                .build();
    }

}
