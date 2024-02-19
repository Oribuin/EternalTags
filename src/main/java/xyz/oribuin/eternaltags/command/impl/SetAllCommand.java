package xyz.oribuin.eternaltags.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.argument.ArgumentHandlers;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import xyz.oribuin.eternaltags.command.argument.TagsArgumentHandler;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

public class SetAllCommand extends BaseRoseCommand {

    public SetAllCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        CommandSender sender = context.getSender();
        Tag tag = context.get("tag");
        String silent = context.get("silent");
        manager.setEveryone(tag);

        if (silent == null) {
            Bukkit.getOnlinePlayers().forEach(player -> locale.sendMessage(player, "command-set-changed", StringPlaceholders.of("tag", manager.getDisplayTag(tag, player))));
        }

        locale.sendMessage(sender, "command-setall-changed", StringPlaceholders.of("tag", manager.getDisplayTag(tag, null)));
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("setall")
                .descriptionKey("command-setall-description")
                .permission("eternaltags.setall")
                .build();
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder()
                .required("tag", new TagsArgumentHandler())
                .optional("silent", ArgumentHandlers.STRING)
                .build();
    }

}
