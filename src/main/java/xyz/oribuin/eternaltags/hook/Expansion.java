package xyz.oribuin.eternaltags.hook;

import dev.rosewood.rosegarden.utils.HexUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
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
        this.formattedPlaceholder = Setting.FORMATTED_PLACEHOLDER.getString();
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {

        // Regular placeholders :)
        if (params.equalsIgnoreCase("total")) return String.valueOf(this.manager.getCachedTags().size());
        if (params.equalsIgnoreCase("joined")) return this.joinTags(this.manager.getPlayerTags(player));
        if (params.equalsIgnoreCase("unlocked")) return String.valueOf(this.manager.getPlayerTags(player).size());
        if (params.equalsIgnoreCase("favorites")) return String.valueOf(this.manager.getUsersFavourites(player.getUniqueId()).size());

        // Allow the ability to get any tag from the id
        String[] args = params.split("_");
        Tag activeTag = this.manager.getUserTag(player);

        // Add new specific tags here
        if (args.length >= 2) {
            String tagId = params.substring(args[0].length() + 1);
            Tag tag = this.manager.getTagFromId(tagId); // args[1]

            if (tag != null) {
                return switch (args[0].toLowerCase()) {
                    case "get" -> this.manager.getDisplayTag(tag, player, "");
                    case "get-formatted" -> this.manager.getDisplayTag(tag, player, this.formattedPlaceholder);
                    case "has" -> String.valueOf(this.manager.canUseTag(player, tag));
                    case "has-unlocked" -> this.manager.canUseTag(player, tag) ? Setting.TAG_UNLOCKED_FORMAT.getString() : Setting.TAG_LOCKED_FORMAT.getString();
                    case "active" -> String.valueOf(activeTag != null && activeTag.getId().equalsIgnoreCase(tag.getId()));
                    case "description" -> TagsUtils.formatList(tag.getDescription(), Setting.DESCRIPTION_DELIMITER.getString());
                    default -> null;
                };
            }
        }

        // Return the result of the placeholder
        return this.result(params, player, activeTag);
    }

    /**
     * Parse all the placeholders and return the result
     *
     * @param param         The placeholder to parse
     * @param offlinePlayer The player to parse the placeholder for
     * @param tag           The tag to parse the placeholder for
     *
     * @return The result of the placeholder
     */
    public String result(String param, OfflinePlayer offlinePlayer, Tag tag) {
        if (offlinePlayer == null) return ""; // Require a player for these placeholders
        Player player = offlinePlayer.getPlayer();

        if (player == null) return ""; // Require a player for these placeholders

        // These tags are different and dont always want formattedPlaceholder
        if (param.equalsIgnoreCase("active")) return String.valueOf(tag != null); // Return true if the tag is not null
        if (param.equalsIgnoreCase("tag")) return this.manager.getDisplayTag(tag, player, ""); // Return the tag with the player's tag
        if (param.equalsIgnoreCase("tag_stripped")) return tag != null ? tag.getTag() : ""; // Return nothing when the tag is null

        // Has unlocked is a double special case
        if (param.equalsIgnoreCase("has-unlocked")) {
            if (tag == null) return Setting.TAG_LOCKED_FORMAT.getString();

            return this.manager.canUseTag(player, tag) ? Setting.TAG_UNLOCKED_FORMAT.getString() : Setting.TAG_LOCKED_FORMAT.getString();
        }

        if (tag == null) return this.formattedPlaceholder; // Return the formatted placeholder if the tag is null

        // Regular tag placeholders
        return switch (param) {
            case "tag_formatted" -> this.manager.getDisplayTag(tag, player, this.formattedPlaceholder);
            case "tag_stripped_formatted" -> tag.getTag();
            case "tag_name" -> tag.getName();
            case "tag_id" -> tag.getId();
            case "tag_permission" -> tag.getPermission();
            case "tag_description" -> TagsUtils.formatList(tag.getDescription(), Setting.DESCRIPTION_DELIMITER.getString());
            case "tag_order" -> String.valueOf(tag.getOrder());
            default -> null;
        };

    }


    /**
     * Join all the tags in a single string
     *
     * @param tags The tags to join
     *
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
