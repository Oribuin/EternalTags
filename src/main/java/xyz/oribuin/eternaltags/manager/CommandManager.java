package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.manager.AbstractCommandManager;
import xyz.oribuin.eternaltags.command.impl.CategoriesCommand;
import xyz.oribuin.eternaltags.command.impl.ClearCommand;
import xyz.oribuin.eternaltags.command.impl.ConvertCommand;
import xyz.oribuin.eternaltags.command.impl.CreateCommand;
import xyz.oribuin.eternaltags.command.impl.DeleteCommand;
import xyz.oribuin.eternaltags.command.impl.EditCommand;
import xyz.oribuin.eternaltags.command.impl.FavoriteCommand;
import xyz.oribuin.eternaltags.command.impl.RandomCommand;
import xyz.oribuin.eternaltags.command.impl.ReloadCommand;
import xyz.oribuin.eternaltags.command.impl.SearchCommand;
import xyz.oribuin.eternaltags.command.impl.SetAllCommand;
import xyz.oribuin.eternaltags.command.impl.SetCommand;
import xyz.oribuin.eternaltags.command.impl.TagsCommand;

import java.util.List;
import java.util.function.Function;

public class CommandManager extends AbstractCommandManager {

    public CommandManager(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public List<Function<RosePlugin, BaseRoseCommand>> getRootCommands() {
        return List.of(TagsCommand::new);
    }

}
