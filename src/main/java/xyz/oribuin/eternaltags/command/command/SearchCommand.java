package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.command.framework.types.GreedyString;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.gui.menu.TagsGUI;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.function.Predicate;

public class SearchCommand extends RoseCommand {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    public SearchCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context, GreedyString keyword) {
        MenuProvider.get(TagsGUI.class).open((Player) context.getSender(), this.containsKeyword(keyword.get()));
    }

    /**
     * Predicate to check if a tag contains a keyword
     *
     * @param keyword The keyword to check
     * @return The predicate
     */
    private Predicate<Tag> containsKeyword(String keyword) {
        return tag -> tag.getId().contains(keyword)
                      || tag.getName().contains(keyword)
                      || tag.getDescription().contains(keyword)
                      || this.strip(tag.getTag()).contains(keyword);
    }

    /**
     * Strip the color codes from a string
     *
     * @param text The string to strip
     * @return The stripped string
     */
    @SuppressWarnings("deprecation")
    private String strip(String text) {
        if (text == null) return null;

        if (Setting.TAG_FORMATTING.getString().equalsIgnoreCase("mini_message"))
            return PLAIN.deserialize(text).content();

        return ChatColor.stripColor(text);
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
