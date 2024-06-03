package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.HelpCommand;
import dev.rosewood.rosegarden.command.PrimaryCommand;
import dev.rosewood.rosegarden.command.framework.*;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.gui.menu.CategoryGUI;
import xyz.oribuin.eternaltags.gui.menu.TagsGUI;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.manager.LocaleManager;

public class TagsCommand extends PrimaryCommand {

    public TagsCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("tags")
                .permission("eternaltags.use")
                .playerOnly(true)
                .build();
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder().optionalSub(
                new HelpCommand(this.rosePlugin, this, CommandInfo.builder("help").descriptionKey("command-help-description").build()),
                new CategoriesCommand(this.rosePlugin),
                new ClearCommand(this.rosePlugin),
                new ConvertCommand(this.rosePlugin),
                new DeleteCommand(this.rosePlugin),
                new EditCommand(this.rosePlugin),
                new FavoriteCommand(this.rosePlugin),
                new RandomCommand(this.rosePlugin),
                new ReloadCommand(this.rosePlugin),
                new SearchCommand(this.rosePlugin),
                new SetAllCommand(this.rosePlugin),
                new SetCommand(this.rosePlugin)
        );
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

        // Make sure the sender is a player.
        if (!(context.getSender() instanceof Player player)) {
            locale.sendMessage(context.getSender(), "only-player");
            return;
        }

        if (Setting.OPEN_CATEGORY_GUI_FIRST.getBoolean())
            MenuProvider.get(CategoryGUI.class).open(player);
        else
            MenuProvider.get(TagsGUI.class).open(player, null);
    }

}
