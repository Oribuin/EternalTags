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
    public String onRequest(@Nullable OfflinePlayer offlineUser, @NotNull String params) {

        // Require a player for these placeholders
        if (offlineUser == null)
            return "Error: Player is null";

        // Allow the ability to get any tag from the id
        final String[] args = params.split("_");

        // Add new specific tags here
        if (args.length >= 2) {
            final String tagId = params.substring(args[0].length() + 1);
            final Tag tag = this.manager.getTagFromId(tagId);
            // Can't use the switch statement here
            if (tag != null) {
                return switch (args[0].toLowerCase()) {
                    case "get" -> this.manager.getDisplayTag(tag, offlineUser, "");
                    case "get-formatted" -> this.manager.getDisplayTag(tag, offlineUser, this.formattedPlaceholder);
                    case "has" -> (offlineUser.getPlayer() == null ? "false" : this.manager.canUseTag(offlineUser.getPlayer(), tag) ? "true" : "false");
                    case "has-unlocked" -> (offlineUser.getPlayer() == null ? "false" : this.manager.canUseTag(offlineUser.getPlayer(), tag) ? Setting.TAG_UNLOCKED_FORMAT.getString() : Setting.TAG_LOCKED_FORMAT.getString());
                    case "active" -> String.valueOf(this.manager.getOfflineUserTag(offlineUser) == tag);
                    default -> "Unknown Placeholder";
                };
            }
        }

        // This is the only tag that doesn't require a player
        if (params.equalsIgnoreCase("total"))
            return String.valueOf(this.manager.getCachedTags().size());

        Player player = offlineUser.getPlayer();
        if (player == null)
            return "Error: Player is null";

        final Tag activeTag = this.manager.getUserTag(player);
        return switch (params.toLowerCase()) {
            // Set bracket placeholders to allow \o/ Placeholder Inception \o/
            case "tag" -> this.manager.getDisplayTag(activeTag, offlineUser, "");
            case "tag_formatted" -> this.manager.getDisplayTag(activeTag, offlineUser, this.formattedPlaceholder);

            // We're separating these tags from the other ones because of placeholder inception
            case "tag_stripped" -> activeTag != null ? activeTag.getTag() : "";
            case "tag_stripped_formatted" -> activeTag != null ? activeTag.getTag() : this.formattedPlaceholder;

            // general tag placeholders, unlikely to be used often
            case "tag_name" -> activeTag != null ? activeTag.getName() : this.formattedPlaceholder;
            case "tag_id" -> activeTag != null ? activeTag.getId() : this.formattedPlaceholder;
            case "tag_permission" -> activeTag != null ? activeTag.getPermission() : this.formattedPlaceholder;
            case "tag_description" -> activeTag != null ? TagsUtils.formatList(activeTag.getDescription(), Setting.DESCRIPTION_DELIMITER.getString()) : this.formattedPlaceholder;
            case "tag_order" -> activeTag != null ? String.valueOf(activeTag.getOrder()) : this.formattedPlaceholder;
            case "active" -> String.valueOf(activeTag != null);

            // These are the tags that return a number.
            case "joined" -> this.joinTags(this.manager.getPlayerTags(offlineUser.getPlayer()));
            case "unlocked" -> offlineUser.getPlayer() != null ? String.valueOf(this.manager.getPlayerTags(offlineUser.getPlayer()).size()) : "0";
            case "favorites" -> offlineUser.getPlayer() != null ? String.valueOf(this.manager.getUsersFavourites(offlineUser.getUniqueId()).size()) : "0";
            default -> null;
        };
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
