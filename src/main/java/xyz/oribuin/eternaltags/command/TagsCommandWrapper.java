package xyz.oribuin.eternaltags.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;

import java.util.Collections;
import java.util.List;

public class TagsCommandWrapper extends RoseCommandWrapper {

    public TagsCommandWrapper(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public String getDefaultName() {
        return "tags";
    }

    @Override
    public List<String> getDefaultAliases() {
        return Collections.singletonList("eternaltags");
    }

    @Override
    public List<String> getCommandPackages() {
        return Collections.singletonList("xyz.oribuin.eternaltags.command.command");
    }

    @Override
    public boolean includeBaseCommand() {
        return false;
    }

    @Override
    public boolean includeHelpCommand() {
        return true;
    }

    @Override
    public boolean includeReloadCommand() {
        return true;
    }
}
