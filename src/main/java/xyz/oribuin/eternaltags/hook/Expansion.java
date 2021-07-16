package xyz.oribuin.eternaltags.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.util.HexUtils;

import java.util.Optional;
import java.util.UUID;

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
        if (player == null)
            return null;
        if (params == null)
            return null;

        final UUID uuid = player.getUniqueId();
        final Tag tag = this.data.getTag(uuid);
        final String currentTag = tag != null ? tag.getTag() : "";

        // Allow the ability to get any tag from the id
        final String[] args = params.split("_");
        if (args.length == 2 && args[0].equalsIgnoreCase("get")) {

            final String tagId = String.join(" ", args).substring(args[0].length() + 1);
            final Optional<Tag> tagOptional = this.tag.getTags().stream().filter(x -> x.getId().equalsIgnoreCase(tagId)).findFirst();
            return tagOptional.filter(x -> x.getTag() != null)
                    .map(Tag::getTag)
                    .orElse("");
        }

        switch (params.toLowerCase()) {
            case "tag":
                return currentTag;
            case "tag_formatted":
                return currentTag.length() == 0 ? "None" : currentTag;
            case "tag_name":
                return tag != null ? tag.getName() : "";
            case "tag_id":
                return tag != null ? tag.getId() : "";
            case "tag_permission":
                return tag != null ? tag.getPermission() : "";
            case "total":
                return String.valueOf(this.tag.getTags().size());
            case "unlocked":
                if (player.getPlayer() != null)
                    return String.valueOf(this.tag.getPlayersTag((Player) player).size());
                else
                    return null;
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
