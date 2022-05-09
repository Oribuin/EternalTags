package xyz.oribuin.eternaltags.hook;

import dev.rosewood.rosegarden.utils.HexUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.ConfigurationManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.util.TagsUtil;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

        if (args.length == 2 && args[0].equalsIgnoreCase("get")) {
            final String tagId = String.join(" ", args).substring(args[0].length() + 1);
            return this.manager.matchTagFromID(tagId).map(Tag::getTag).orElse("");
        }

        final Optional<Tag> activeTag = this.manager.getUsersTag(player.getUniqueId());

        return switch (params.toLowerCase()) {
            case "tag" -> HexUtils.colorify(activeTag.map(Tag::getTag).orElse(""));
            case "tag_formatted" -> HexUtils.colorify(activeTag.map(Tag::getTag).orElse(formattedPlaceholder));
            case "tag_stripped" -> activeTag.map(Tag::getTag).orElse("");
            case "tag_stripped_formatted" -> activeTag.map(Tag::getTag).orElse(formattedPlaceholder);
            case "tag_name" -> activeTag.map(Tag::getName).orElse(formattedPlaceholder);
            case "tag_id" -> activeTag.map(Tag::getId).orElse(formattedPlaceholder);
            case "tag_permission" -> activeTag.map(Tag::getPermission).orElse(formattedPlaceholder);
            case "tag_description" -> TagsUtil.formatList(activeTag.map(Tag::getDescription).orElse(Collections.singletonList(formattedPlaceholder)));
            case "total" -> String.valueOf(this.manager.getCachedTags().size());
            case "joined" -> this.joinTags(Optional.ofNullable(player.getPlayer())
                    .map(this.manager::getPlayersTags)
                    .orElse(this.manager.getCachedTags().values().stream().toList()));
            case "unlocked" -> player.getPlayer() != null ? String.valueOf(this.manager.getPlayersTags(player.getPlayer()).size()) : "0";
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
        return tags.stream().map(Tag::getTag).reduce("", (a, b) -> a + b);
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
