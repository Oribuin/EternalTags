package xyz.oribuin.eternaltags.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

public class RandomCommand extends BaseRoseCommand {

    public RandomCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        Player sender = (Player) context.getSender();

        Tag randomTag = manager.getRandomTag(sender);
        if (randomTag == null) {
            locale.sendMessage(sender, "no-tags");
            return;
        }

        randomTag.equip(sender);
        locale.sendMessage(sender, "command-set-changed", StringPlaceholders.of("tag", manager.getDisplayTag(randomTag, sender)));
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("random")
                .descriptionKey("command-random-description")
                .permission("eternaltags.random")
                .playerOnly(true)
                .build();
    }

}
