package xyz.oribuin.eternaltags.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;

import static xyz.oribuin.orilibrary.util.HexUtils.colorify;

public class Expansion extends PlaceholderExpansion {

    private final EternalTags plugin;
    private final TagManager tag;
    private final DataManager data;

    public Expansion(final EternalTags plugin) {
        this.plugin = plugin;
        this.tag = plugin.getManager(TagManager.class);
        this.data = plugin.getManager(DataManager.class);
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {

        // Allow the ability to get any tag from the id
        final String[] args = params.split("_");

        if (args.length == 2 && args[0].equalsIgnoreCase("get")) {
            final String tagId = String.join(" ", args).substring(args[0].length() + 1);
            return this.tag.getTags().stream().filter(x -> x.getId().equalsIgnoreCase(tagId)).map(Tag::getTag).findAny().orElse("");
        }

        final Tag tag = this.data.getTag(player.getUniqueId());
        final String currentTag = colorify(tag != null ? tag.getTag() : "");

        final String formattedPlaceholder = colorify(this.plugin.getConfig().get("formatted_placeholder") != null
                ? this.plugin.getConfig().getString("formatted_placeholder")
                : "None");

        switch (params.toLowerCase()) {
            case "tag":
                return currentTag;
            case "tag_formatted":
                return tag != null ? colorify(tag.getTag()) : formattedPlaceholder;
            case "tag_stripped":
                return tag != null ? tag.getTag() : "";
            case "tag_stripped_formatted":
                return tag != null ? tag.getTag() : formattedPlaceholder;
            case "tag_name":
                return tag != null ? tag.getName() : formattedPlaceholder;
            case "tag_id":
                return tag != null ? tag.getId() : formattedPlaceholder;
            case "tag_permission":
                return tag != null ? tag.getPermission() : formattedPlaceholder;
            case "tag_description":
                return tag != null ? String.join(" ", tag.getDescription()) : formattedPlaceholder;
            case "total":
                return String.valueOf(this.tag.getTags().size());
            case "unlocked":
                if (player.getPlayer() != null)
                    return String.valueOf(this.tag.getPlayersTag((Player) player).size());
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
