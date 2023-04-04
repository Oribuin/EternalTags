package xyz.oribuin.eternaltags;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Category;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.UUID;

/**
 * @author Oribuin
 * @since v1.0.11
 */
public class EternalAPI {

    private static EternalAPI instance;
    private final TagsManager tagManager;

    private EternalAPI() {
        this.tagManager = EternalTags.getInstance().getManager(TagsManager.class);
    }

    /**
     * Get an offline player's active tag
     *
     * @param uuid The player's UUID
     * @return The [Tag] belonging to the player, This tag is nullable
     * @since v1.1.4
     */
    @Nullable
    public Tag getUserTag(UUID uuid) {
        return this.getTagManager().getUserTag(uuid);
    }

    /**
     * Get a player's active tag if they are online.
     *
     * @param player The player to get the tag of
     * @return The [Tag] belonging to the player, This tag is nullable
     * @since v1.1.4
     */
    @Nullable
    public Tag getOnlineTag(Player player) {
        return this.tagManager.getUserTag(player);
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

    /**
     * Get a category from the id
     *
     * @param id The id of the tag
     * @return The [Category] belonging to the id, This category is nullable
     */
    @Nullable
    public Category getCategory(String id) {
        return this.tagManager.getCategory(id);
    }

    /**
     * Get the category of a tag
     *
     * @param tag The tag
     * @return The [Category] belonging to the tag, This category is nullable
     */
    @Nullable
    public Category getCategory(Tag tag) {
        return this.tagManager.getCategory(tag);
    }

    public static EternalAPI getInstance() {
        if (instance == null)
            instance = new EternalAPI();
        return instance;
    }

    public TagsManager getTagManager() {
        return tagManager;
    }

}
