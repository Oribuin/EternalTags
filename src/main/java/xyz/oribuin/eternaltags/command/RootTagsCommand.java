package xyz.oribuin.eternaltags.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.HelpCommand;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import xyz.oribuin.eternaltags.command.command.*;

public class RootTagsCommand extends BaseRoseCommand {

    public RootTagsCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("tags")
                .aliases("eternaltags")
                .build();
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder().requiredSub(
                new HelpCommand(this.rosePlugin, this) {
                    @Override
                    protected CommandInfo createCommandInfo() {
                        return CommandInfo.builder("help").build();
                    }
                },
                new ReloadCommand(this.rosePlugin),
                new TagsCommand(this.rosePlugin),
                new SetCommand(this.rosePlugin),
                new SetAllCommand(this.rosePlugin),
                new SearchCommand(this.rosePlugin),
                new RandomCommand(this.rosePlugin),
                new FavoriteCommand(this.rosePlugin),
                new EditCommand(this.rosePlugin),
                new DeleteCommand(this.rosePlugin),
                new CreateCommand(this.rosePlugin),
                new ConvertCommand(this.rosePlugin),
                new ClearCommand(this.rosePlugin),
                new CategoriesCommand(this.rosePlugin)
        );
    }
}
