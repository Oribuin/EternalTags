package dev.oribuin.eternaltags.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;
import dev.oribuin.eternaltags.command.argument.TagsArgumentHandler;
import dev.oribuin.eternaltags.gui.MenuProvider;
import dev.oribuin.eternaltags.gui.menu.FavouritesGUI;
import dev.oribuin.eternaltags.manager.LocaleManager;
import dev.oribuin.eternaltags.manager.TagsManager;
import dev.oribuin.eternaltags.obj.Tag;

public class FavoriteCommand extends BaseRoseCommand {

    public FavoriteCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, Tag tag) {
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
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
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("favorite")
                .descriptionKey("command-favorite-description")
                .permission("eternaltags.favorite")
                .aliases("favourite")
                .playerOnly(true)
                .arguments(this.createArguments())
                .build();
    }

    private ArgumentsDefinition createArguments() {
        return ArgumentsDefinition.builder()
                .optional("tag", new TagsArgumentHandler())
                .build();
    }

}
