package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.manager.AbstractCommandManager;
import xyz.oribuin.eternaltags.command.TagsCommandWrapper;

import java.util.Collections;
import java.util.List;

public class CommandManager extends AbstractCommandManager {

    public CommandManager(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public List<Class<? extends RoseCommandWrapper>> getRootCommands() {
        return Collections.singletonList(TagsCommandWrapper.class);
    }

    @Override
    public List<String> getArgumentHandlerPackages() {
        return Collections.singletonList("xyz.oribuin.eternaltags.command.argument");
    }
}
