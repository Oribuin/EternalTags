package xyz.oribuin.eternaltags;

import org.bukkit.OfflinePlayer;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;

/**
 * @author Oribuin
 * @since v1.0.10
 */
public class EternalAPI {

    private static EternalAPI instance;
    private final DataManager dataManager;
    private final TagManager tagManager;

    public EternalAPI(final EternalTags plugin) {
        instance = this;
        this.dataManager = plugin.getManager(DataManager.class);
        this.tagManager = plugin.getManager(TagManager.class);
    }

    /**
     * Get an offline player's active tag
     *
     * @param player The offline player.
     * @return The [Tag] belonging to the player, This tag is nullable
     */
    public Tag getUserTag(OfflinePlayer player) {
        return this.dataManager.getTag(player.getUniqueId());
    }

    /**
     * Set a player's active tag
     *
     * @param player The player
     * @param tag    The tag, Set this to null to remove the tag.
     */
    public void setTag(OfflinePlayer player, Tag tag) {
        this.dataManager.updateUser(player.getUniqueId(), tag);
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public TagManager getTagManager() {
        return tagManager;
    }

    public static EternalAPI getInstance() {
        return instance;
    }

}
