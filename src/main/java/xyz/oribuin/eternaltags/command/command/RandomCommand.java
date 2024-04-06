package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.*;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.event.TagEquipEvent;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

public class RandomCommand extends BaseRoseCommand {

    public RandomCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        Player sender = (Player) context.getSender();

        final Tag randomTag = manager.getRandomTag(sender);
        if (randomTag == null) {
            locale.sendMessage(sender, "no-tags");
            return;
        }

        final TagEquipEvent event = new TagEquipEvent(sender, randomTag);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        manager.setTag(sender.getUniqueId(), randomTag);
        locale.sendMessage(sender, "command-set-changed", StringPlaceholders.of("tag", manager.getDisplayTag(randomTag, sender)));
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("random")
                .permission("eternaltags.random")
                .descriptionKey("command-random-description")
                .playerOnly(true)
                .build();
    }

}
