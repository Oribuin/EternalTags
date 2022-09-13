package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.command.framework.types.GreedyString;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.gui.TagsGUI;
import xyz.oribuin.eternaltags.manager.MenuManager;

public class SearchCommand extends RoseCommand {

    public SearchCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context, GreedyString keyword) {
        this.rosePlugin.getManager(MenuManager.class).get(TagsGUI.class).open((Player) context.getSender(), keyword.get());
    }

    @Override
    protected String getDefaultName() {
        return "search";
    }

    @Override
    public String getDescriptionKey() {
        return "command-search-description";
    }

    @Override
    public String getRequiredPermission() {
        return "eternaltags.search";
    }

    @Override
    public boolean isPlayerOnly() {
        return true;
    }

}
