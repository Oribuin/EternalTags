package dev.oribuin.eternaltags.command.impl;

import dev.oribuin.eternaltags.gui.MenuProvider;
import dev.oribuin.eternaltags.gui.menu.TagsGUI;
import dev.oribuin.eternaltags.manager.LocaleManager;
import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.HelpCommand;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import org.bukkit.entity.Player;

public class TagsCommand extends BaseRoseCommand {

    public TagsCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

        // Make sure the sender is a player.
        if (!(context.getSender() instanceof Player player)) {
            locale.sendMessage(context.getSender(), "only-player");
            return;
        }

        MenuProvider.get(TagsGUI.class).open(player, null);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("tags")
                .descriptionKey("command-tags-description")
                .permission("eternaltags.use")
                .arguments(this.createArguments())
                .build();
    }

    private ArgumentsDefinition createArguments() {
        return ArgumentsDefinition.builder().optionalSub(
                new HelpCommand(this.rosePlugin, this),
                new ClearCommand(this.rosePlugin),
                new ConvertCommand(this.rosePlugin),
                new CreateCommand(this.rosePlugin),
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
}
