package dev.oribuin.eternaltags.command.impl;

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
import dev.oribuin.eternaltags.command.argument.TagsArgumentHandler;
import dev.oribuin.eternaltags.manager.LocaleManager;
import dev.oribuin.eternaltags.manager.TagsManager;
import dev.oribuin.eternaltags.obj.Tag;

public class SetCommand extends BaseRoseCommand {

    public SetCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, Tag tag, Player target, String silent) {
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        CommandSender sender = context.getSender();

        // may need to check if tag == null?
        if (tag == null) {
            locale.sendMessage(sender, "tag-doesnt-exist");
            return;
        }

        // Setting another player's tag
        if (target != null) {
            if (!sender.hasPermission("eternaltags.set.other")) {
                locale.sendMessage(sender, "no-permission");
                return;
            }

            tag.equip(target);

            if (silent == null) {
                locale.sendMessage(target, "command-set-changed", StringPlaceholders.of("tag", manager.getDisplayTag(tag, target)));
            }

            locale.sendMessage(sender, "command-set-changed-other", StringPlaceholders.builder("tag", manager.getDisplayTag(tag, target))
                    .add("player", target.getName())
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

        tag.equip(pl);
        locale.sendMessage(sender, "command-set-changed", StringPlaceholders.of("tag", manager.getDisplayTag(tag, pl)));
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("set")
                .descriptionKey("command-set-description")
                .permission("eternaltags.set")
                .arguments(this.createArguments())
                .build();
    }

    private ArgumentsDefinition createArguments() {
        return ArgumentsDefinition.builder()
                .required("tag", new TagsArgumentHandler())
                .optional("target", ArgumentHandlers.PLAYER)
                .optional("silent", ArgumentHandlers.STRING)
                .build();
    }

}
