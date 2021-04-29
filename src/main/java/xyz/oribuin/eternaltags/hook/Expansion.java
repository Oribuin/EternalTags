package xyz.oribuin.eternaltags.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.orilibrary.util.HexUtils;

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
    public String onPlaceholderRequest(Player player, @NotNull String params) {

        final UUID uuid = player.getUniqueId();
        final String currentTag = this.data.getTag(uuid) == null ? "" : this.data.getTag(uuid).getTag();

        if (params.equalsIgnoreCase("tag")) return HexUtils.colorify(currentTag);
        if (params.equalsIgnoreCase("tag_formatted")) return HexUtils.colorify(currentTag.length() == 0 ? "None" : currentTag);
        if (params.equalsIgnoreCase("total")) return String.valueOf(this.tag.getTags().size());
        if (params.equalsIgnoreCase("unlocked")) return String.valueOf(this.tag.getPlayersTag(player).size());

        return null;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "EternalTags";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Oribuin";
    }

    @Override
    public @NotNull String getVersion() {
        return this.plugin.getDescription().getVersion();
    }
}
