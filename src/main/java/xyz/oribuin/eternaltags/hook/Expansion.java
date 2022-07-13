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
            // Can't use the switch statement here
            if (args[0].equalsIgnoreCase("get") && tag != null)
                return this.manager.getDisplayTag(tag, player, this.formattedPlaceholder);

            else if (args[0].equalsIgnoreCase("has") && tag != null)
                return player.getPlayer() != null && player.getPlayer().hasPermission(tag.getPermission()) ? "true" : "false";
        }


        final Tag activeTag = this.manager.getPlayersTag(player);

        return switch (params.toLowerCase()) {
            // Set bracket placeholders to allow \o/ Placeholder Inception \o/
            case "tag" -> this.manager.getDisplayTag(activeTag, player, "");
            case "tag_formatted" -> this.manager.getDisplayTag(activeTag, player, this.formattedPlaceholder);

            // We're separating these tags from the other ones because of placeholder inception
            case "tag_stripped" -> activeTag != null ? activeTag.getTag() : "";
            case "tag_stripped_formatted" -> activeTag != null ? activeTag.getTag() : this.formattedPlaceholder;

            // general tag placeholders, unlikely to be used often
            case "tag_name" -> activeTag != null ? activeTag.getName() : this.formattedPlaceholder;
            case "tag_id" -> activeTag != null ? activeTag.getId() : this.formattedPlaceholder;
            case "tag_permission" -> activeTag != null ? activeTag.getPermission() : this.formattedPlaceholder;
            case "tag_description" -> activeTag != null ? TagsUtils.formatList(activeTag.getDescription()) : this.formattedPlaceholder;
            case "tag_order" -> activeTag != null ? String.valueOf(activeTag.getOrder()) : this.formattedPlaceholder;
            case "tag_icon" -> activeTag != null ? activeTag.getIcon().toString() : this.formattedPlaceholder;

            // These are the tags that return a number.
            case "total" -> String.valueOf(this.manager.getCachedTags().size());
            case "joined" -> this.joinTags(this.manager.getPlayerTags(player.getPlayer()));
            case "unlocked" -> player.getPlayer() != null ? String.valueOf(this.manager.getPlayerTags(player.getPlayer()).size()) : "0";
            case "favorites" -> player.getPlayer() != null ? String.valueOf(this.manager.getUsersFavourites(player.getUniqueId()).size()) : "0";
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
