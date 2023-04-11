package xyz.oribuin.eternaltags.manager;

import com.google.gson.Gson;
import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.manager.AbstractDataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.database.migration._1_CreateInitialTables;
import xyz.oribuin.eternaltags.database.migration._2_CreateNewTagTables;
import xyz.oribuin.eternaltags.database.migration._3_ModifyTagDataItems;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.obj.TagDescription;
import xyz.oribuin.eternaltags.obj.TagUser;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DataManager extends AbstractDataManager {

    private final Map<UUID, TagUser> cachedUsers = new HashMap<>();

    private final Gson gson = new Gson();

    public DataManager(RosePlugin plugin) {
        super(plugin);
    }

    @Override
    public void reload() {
        super.reload();

        this.cachedUsers.clear();
    }

    /**
     * Update a user's current tag
     *
     * @param uuid The player's uuid
     * @param tag  The tag
     */
    public void saveUser(@NotNull UUID uuid, @NotNull Tag tag) {
        TagUser user = this.cachedUsers.getOrDefault(uuid, new TagUser(uuid));
        user.setActiveTag(tag.getId());
        user.setUsingDefaultTag(false);
        this.cachedUsers.put(uuid, user);

        final String query = "REPLACE INTO " + this.getTablePrefix() + "tags (player, tagID) VALUES (?, ?)";
        this.async(task -> this.databaseConnector.connect(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, tag.getId());
                statement.executeUpdate();
            }
        }));
    }

    /**
     * Clear a player's active tag from the database
     *
     * @param uuid The UUID of the player
     */
    public void removeUser(final UUID uuid) {
        this.cachedUsers.remove(uuid);

        final String query = "DELETE FROM " + this.getTablePrefix() + "tags WHERE player = ?";
        this.async(task -> this.databaseConnector.connect(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            }
        }));

    }

    /**
     * Remove any user from the database with specific tag.
     *
     * @param id The tag id being removed.
     */
    public void deleteUserTag(String id) {
        this.cachedUsers.values().removeIf(tag -> tag.getActiveTag() != null && tag.getActiveTag().equalsIgnoreCase(id));

        final String query = "DELETE FROM " + this.getTablePrefix() + "tags WHERE tagID = ?";
        this.async(task -> this.databaseConnector.connect(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, id);
                statement.executeUpdate();
            }
        }));
    }

    /**
     * Change every select user's active tag to the new tag.
     *
     * @param tag The tag.
     */
    public void updateUsers(Tag tag, List<UUID> players) {
        players.forEach(player -> {
            TagUser user = this.cachedUsers.getOrDefault(player, new TagUser(player));
            user.setActiveTag(tag.getId());
            user.setUsingDefaultTag(false);
            this.cachedUsers.put(player, user);
        });


        this.async(task -> this.databaseConnector.connect(connection -> {
            final String query = "REPLACE INTO " + this.getTablePrefix() + "tags (player, tagID) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (UUID player : players) {
                    statement.setString(1, player.toString());
                    statement.setString(2, tag.getId());
                    statement.executeUpdate();
                }
            }
        }));

    }

    /**
     * Add a tag to a player's favourite tags.
     *
     * @param uuid The uuid of the player
     * @param tag  The tag being added
     */
    public void addFavourite(UUID uuid, Tag tag) {
        TagUser user = this.cachedUsers.getOrDefault(uuid, new TagUser(uuid));
        user.getFavourites().add(tag.getId());
        this.cachedUsers.put(uuid, user);


        this.async(task -> this.databaseConnector.connect(connection -> {
            final String query = "INSERT INTO " + this.getTablePrefix() + "favourites (player, tagID) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, tag.getId());
                statement.executeUpdate();
            }
        }));
    }

    /**
     * Remove a player's favourited tags.
     *
     * @param uuid The uuid of the player
     * @param tag  The tag being removed.
     */
    public void removeFavourite(UUID uuid, Tag tag) {
        TagUser user = this.cachedUsers.getOrDefault(uuid, new TagUser(uuid));
        user.getFavourites().remove(tag.getId());
        this.cachedUsers.put(uuid, user);

        this.async(task -> this.databaseConnector.connect(connection -> {
            final String query = "DELETE FROM " + this.getTablePrefix() + "favourites WHERE player = ? AND tagID = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, tag.getId());
                statement.executeUpdate();
            }
        }));

    }

    public void clearFavourites(UUID uuid) {
        this.cachedUsers.remove(uuid);

        this.async(task -> this.databaseConnector.connect(connection -> {
            final String query = "DELETE FROM " + this.getTablePrefix() + "favourites WHERE player = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
            }
        }));
    }

    /**
     * Load a user from the database and cache them.
     *
     * @param uuid The player's uuid
     */
    public void loadUser(@NotNull UUID uuid) {
        final TagUser user = new TagUser(uuid);

        this.async(task -> this.databaseConnector.connect(connection -> {
            final String selectTag = "SELECT tagID FROM " + this.getTablePrefix() + "tags WHERE player = ?";

            // Load the active tag from the database.
            try (PreparedStatement statement = connection.prepareStatement(selectTag)) {
                statement.setString(1, uuid.toString());
                final ResultSet result = statement.executeQuery();
                if (result.next()) {
                    user.setActiveTag(result.getString(1));
                }
            }

            final String favouriteTagsQuery = "SELECT tagID FROM " + this.getTablePrefix() + "favourites WHERE player = ?";
            try (PreparedStatement statement = connection.prepareStatement(favouriteTagsQuery)) {
                statement.setString(1, uuid.toString());
                final ResultSet result = statement.executeQuery();
                while (result.next()) {
                    user.getFavourites().add(result.getString(1));
                }
            }

            this.cachedUsers.put(uuid, user);
        }));
    }


    /**
     * Load all tag data from the database.
     *
     * @param cachedTags The map of cached tags.
     */
    public void loadTagData(Map<String, Tag> cachedTags) {
        cachedTags.clear();

        this.async(task -> this.databaseConnector.connect(connection -> {
            final String query = "SELECT * FROM " + this.getTablePrefix() + "tag_data";
            try (PreparedStatement statement = connection.prepareStatement(query)) {

                final ResultSet result = statement.executeQuery();
                while (result.next()) {
                    final String id = result.getString("tagId");
                    final List<String> description = gson.fromJson(result.getString("description"), TagDescription.class).getDescription();

                    Tag tag = new Tag(id, result.getString("name"), result.getString("tag"));
                    tag.setPermission(result.getString("permission"));
                    tag.setDescription(description);
                    tag.setOrder(result.getInt("order"));
                    tag.setIcon(TagsUtils.deserializeItem(result.getBytes("icon")));
                    cachedTags.put(id, tag);
                }
            }
        }));
    }

    /**
     * Save tag data to the database.
     *
     * @param tag The tag to save.
     */
    public void saveTagData(@NotNull Tag tag) {
        this.async(task -> this.databaseConnector.connect(connection -> {
            final String query = "REPLACE INTO " + this.getTablePrefix() + "tag_data (tagId, `name`, description, tag, permission, `order`, icon) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, tag.getId());
                statement.setString(2, tag.getName());
                statement.setString(3, gson.toJson(new TagDescription(tag.getDescription())));
                statement.setString(4, tag.getTag());
                statement.setString(5, tag.getPermission());
                statement.setInt(6, tag.getOrder());
                statement.setBytes(7, tag.getIcon() != null ? TagsUtils.serializeItem(tag.getIcon()) : null);
                statement.executeUpdate();
            }
        }));
    }

    /**
     * Mass save tag data to the database.
     *
     * @param tags The tags to save.
     */
    public void saveTagData(Map<String, Tag> tags) {
        this.async(task -> this.databaseConnector.connect(connection -> {
            final String query = "REPLACE INTO " + this.getTablePrefix() + "tag_data (tagId, `name`, description, tag, permission, `order`, icon) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (Tag tag : tags.values()) {
                    statement.setString(1, tag.getId());
                    statement.setString(2, tag.getName());
                    statement.setString(3, gson.toJson(new TagDescription(tag.getDescription())));
                    statement.setString(4, tag.getTag());
                    statement.setString(5, tag.getPermission());
                    statement.setInt(6, tag.getOrder());
                    statement.setBytes(7, tag.getIcon() != null ? TagsUtils.serializeItem(tag.getIcon()) : null);
                    statement.addBatch();
                }

                statement.executeBatch();
            }
        }));
    }

    /**
     * Delete tag data from the database.
     *
     * @param tag The tag to delete.
     */
    public void deleteTagData(Tag tag) {
        this.async(task -> this.databaseConnector.connect(connection -> {
            final String query = "DELETE FROM " + this.getTablePrefix() + "tag_data WHERE tagId = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, tag.getId());
                statement.executeUpdate();
            }
        }));
    }

    /**
     * Delete all the tag data from the database.
     */
    public void deleteAllTagData() {
        this.async(task -> this.databaseConnector.connect(connection -> {
            final String query = "DELETE FROM " + this.getTablePrefix() + "tag_data";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.executeUpdate();
            }
        }));
    }

    @Override
    public List<Class<? extends DataMigration>> getDataMigrations() {
        return Arrays.asList(_1_CreateInitialTables.class, _2_CreateNewTagTables.class, _3_ModifyTagDataItems.class);
    }

    private void async(Consumer<BukkitTask> callback) {
        this.rosePlugin.getServer().getScheduler().runTaskAsynchronously(rosePlugin, callback);
    }

    public Map<UUID, TagUser> getCachedUsers() {
        return cachedUsers;
    }

}
