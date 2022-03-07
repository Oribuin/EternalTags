package xyz.oribuin.eternaltags;

import org.bukkit.OfflinePlayer;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.Optional;

/**
 * @author Oribuin
 * @since v1.0.11
 */
public class EternalAPI {

    private static EternalAPI instance;
    private final TagsManager tagManager;

    public EternalAPI(final EternalTags plugin) {
        instance = this;
        this.tagManager = plugin.getManager(TagsManager.class);
    }

    /**
     * Get an offline player's active tag
     *
     * @param player The offline player.
     * @return The [Tag] belonging to the player, This tag is nullable
     */
    @Deprecated
    public Tag getUserTag(OfflinePlayer player) {
        return null;
    }

    public Optional<Tag> getUser(OfflinePlayer player) {
        return this.tagManager.getUsersTag(player.getUniqueId());
    }

    /**
     * Set a player's active tag
     *
     * @param player The player
     * @param tag    The tag, Set this to null to remove the tag.
     */
    public void setTag(OfflinePlayer player, Tag tag) {
        this.tagManager.setTag(player.getUniqueId(), tag);
    }

    public TagsManager getTagManager() {
        return tagManager;
    }

    public static EternalAPI getInstance() {
        return instance;
    }

}
