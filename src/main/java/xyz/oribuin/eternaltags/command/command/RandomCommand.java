package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.event.TagEquipEvent;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.Optional;

public class RandomCommand extends RoseCommand {

    public RandomCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        Player sender = (Player) context.getSender();

        final Optional<Tag> tagOptional = manager.getRandomTag(sender);
        if (!tagOptional.isPresent()) {
            locale.sendMessage(sender, "no-tags");
            return;
        }

        final TagEquipEvent event = new TagEquipEvent(sender, tagOptional.get());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        manager.setTag(sender.getUniqueId(), tagOptional.get());
        locale.sendMessage(sender, "command-set-changed", StringPlaceholders.single("tag", tagOptional.get()));
    }


    @Override
    protected String getDefaultName() {
        return "random";
    }

    @Override
    public String getDescriptionKey() {
        return "command-random-description";
    }

    @Override
    public String getRequiredPermission() {
        return "eternaltags.random";
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

}
