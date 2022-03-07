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

        switch (params.toLowerCase()) {
            case "tag":
                return HexUtils.colorify(activeTag.map(Tag::getTag).orElse(""));
            case "tag_formatted":
                return HexUtils.colorify(activeTag.map(Tag::getTag).orElse(formattedPlaceholder));
            case "tag_stripped":
                return activeTag.map(Tag::getTag).orElse("");
            case "tag_stripped_formatted":
                return activeTag.map(Tag::getTag).orElse(formattedPlaceholder);
            case "tag_name":
                return activeTag.map(Tag::getName).orElse(formattedPlaceholder);
            case "tag_id":
                return activeTag.map(Tag::getId).orElse(formattedPlaceholder);
            case "tag_permission":
                return activeTag.map(Tag::getPermission).orElse(formattedPlaceholder);
            case "tag_description":
                return TagsUtil.formatList(activeTag.map(Tag::getDescription).orElse(Collections.singletonList(formattedPlaceholder)));
            case "total":
                return String.valueOf(this.manager.getCachedTags().size());
            case "unlocked":
                if (player.getPlayer() != null)
                    return String.valueOf(this.manager.getPlayersTags(player.getPlayer()).size());
                else
                    return "0";
            default:
                return null;
        }
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
