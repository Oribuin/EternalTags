package xyz.oribuin.eternaltags.hook;

import dev.rosewood.rosegarden.utils.HexUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.ConfigurationManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.util.List;

public class Expansion extends PlaceholderExpansion {

    private final EternalTags plugin;
    private final TagsManager manager;
    private final String formattedPlaceholder;

    public Expansion(final EternalTags plugin) {
        this.plugin = plugin;
        this.manager = plugin.getManager(TagsManager.class);
        this.formattedPlaceholder = ConfigurationManager.Setting.FORMATTED_PLACEHOLDER.getString();
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        // Allow the ability to get any tag from the id
        final String[] args = params.split("_");

        // Add new specific tags here
        if (args.length >= 2) {
            final String tagId = String.join(" ", args).substring(args[0].length() + 1);
            final Tag tag = this.manager.getTagFromId(tagId);
            if (tag == null)
                return this.formattedPlaceholder;

            // Can't use the switch statement here
            if (args[0].equalsIgnoreCase("get"))
                return this.formatString(tag.getTag(), true);

            else if (args[0].equalsIgnoreCase("has"))
                return player.getPlayer() != null && player.getPlayer().hasPermission(tag.getPermission()) ? "true" : "false";
        }


        final Tag activeTag = this.manager.getPlayersTag(player);
        return switch (params.toLowerCase()) {
            // Set bracket placeholders to allow \o/ Placeholder Inception \o/
            case "tag" -> this.manager.getDisplayTag(activeTag, player, "");
            case "tag_formatted" -> this.manager.getDisplayTag(activeTag, player, this.formattedPlaceholder);

            // We're separating these tags from the other ones because of placeholder inception
            case "tag_stripped" -> this.formatString(activeTag.getTag(), false);
            case "tag_stripped_formatted" -> this.formatString(activeTag.getTag(), false);

            // Geneal tag placeholders, unlikely to be used often
            case "tag_name" -> this.formatString(activeTag.getName(), true);
            case "tag_id" -> this.formatString(activeTag.getId(), true);
            case "tag_permission" -> this.formatString(activeTag.getPermission(), true);
            case "tag_description" -> TagsUtils.formatList(this.formatList(activeTag.getDescription()));
            case "tag_order" -> this.formatString(String.valueOf(activeTag.getOrder()), true);
            case "tag_icon" -> this.formatString(activeTag.getIcon().toString(), true);

            // These are the tags that return a number.
            case "total" -> String.valueOf(this.manager.getCachedTags().size());
            case "joined" -> this.joinTags(this.manager.getPlayerTags(player.getPlayer()));
            case "unlocked" -> player.getPlayer() != null ? String.valueOf(this.manager.getPlayerTags(player.getPlayer()).size()) : "0";
            case "favorites" -> player.getPlayer() != null ? String.valueOf(this.manager.getUsersFavourites(player.getUniqueId()).size()) : "0";
            default -> null;
        };
    }

    /**
     * Format a string depending on if it is null or not
     *
     * @param text      The string to format
     * @param formatted If the string is formatted or not
     * @return The formatted string
     */
    private String formatString(@Nullable String text, boolean formatted) {
        if (text == null)
            return formatted ? this.formattedPlaceholder : "";

        return text;
    }

    /**
     * Format a string list depending on if it is null or not
     *
     * @param list The strings to format
     * @return The formatted string list
     */
    private List<String> formatList(@Nullable List<String> list) {
        if (list == null)
            return List.of(this.formattedPlaceholder);

        return list;
    }

    /**
     * Join all the tags in a single string
     *
     * @param tags The tags to join
     * @return The joined tags
     */
    public String joinTags(List<Tag> tags) {
        return tags.stream().map(Tag::getTag).map(HexUtils::colorify).reduce("", (a, b) -> a + b);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "eternaltags";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Oribuin";
    }

    @Override
    public @NotNull String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

}
