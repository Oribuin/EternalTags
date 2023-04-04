package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.Optional;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.gui.menu.FavouritesGUI;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.Collections;
import java.util.List;

public class FavoriteCommand extends RoseCommand {

    public FavoriteCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context, @Optional Tag tag) {
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
                .addPlaceholder("toggled", !isFavourite ? on : off)
                .build());
    }


    @Override
    protected String getDefaultName() {
        return "favorite";
    }

    @Override
    protected List<String> getDefaultAliases() {
        return Collections.singletonList("favourite");
    }

    @Override
    public String getDescriptionKey() {
        return "command-favorite-description";
    }

    @Override
    public String getRequiredPermission() {
        return "eternaltags.favorite";
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

}
