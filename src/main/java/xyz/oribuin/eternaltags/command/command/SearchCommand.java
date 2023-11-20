package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.command.framework.types.GreedyString;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.gui.menu.TagsGUI;

public class SearchCommand extends RoseCommand {

    public SearchCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @SuppressWarnings("deprecation")
    @RoseExecutable
    public void execute(CommandContext context, GreedyString keyword) {
        MenuProvider.get(TagsGUI.class).open((Player) context.getSender(), tag -> tag.getId().contains(keyword.get())
                                                                                  || tag.getName().contains(keyword.get())
                                                                                  || tag.getDescription().contains(keyword.get())
                                                                                  || ChatColor.stripColor(tag.getTag()).contains(keyword.get()));
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
