package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.command.argument.TagsArgumentHandlers;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.gui.menu.FavouritesGUI;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.Collections;
import java.util.List;

public class FavoriteCommand extends BaseRoseCommand {

    public FavoriteCommand(RosePlugin rosePlugin) {
        super(rosePlugin);

    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("favorite")
                .aliases("favourite")
                .descriptionKey("command-favorite-description")
                .permission("eternaltags.favorite")
                .playerOnly(true)
                .build();
    }

    @RoseExecutable
    public void execute(CommandContext context, Tag tag) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        Player sender = (Player) context.getSender();

        if (tag == null) {
            MenuProvider.get(FavouritesGUI.class).open(sender);
            return;
        }

        if (!manager.canUseTag(sender, tag)) {
            locale.sendMessage(sender, "command-favorite-no-permission");
            return;
        }

        boolean isFavourite = manager.isFavourite(sender.getUniqueId(), tag);

        if (isFavourite)
            manager.removeFavourite(sender.getUniqueId(), tag);
        else
            manager.addFavourite(sender.getUniqueId(), tag);

        String on = locale.getLocaleMessage("command-favorite-on");
        String off = locale.getLocaleMessage("command-favorite-off");

        locale.sendMessage(sender, "command-favorite-toggled", StringPlaceholders.builder("tag", manager.getDisplayTag(tag, sender))
                .add("toggled", !isFavourite ? on : off)
                .build());
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder()
                .optional("tag", TagsArgumentHandlers.TAG)
                .build();
    }
}
