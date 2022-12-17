package xyz.oribuin.eternaltags.manager;

import com.google.gson.Gson;
import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.manager.AbstractDataManager;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.database.migration._1_CreateInitialTables;
import xyz.oribuin.eternaltags.database.migration._2_CreateNewTagTables;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.obj.TagDescription;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class DataManager extends AbstractDataManager {

    private final Map<UUID, Tag> cachedUsers = new HashMap<>();
    private final Map<UUID, Set<Tag>> cachedFavourites = new HashMap<>();

    private final Gson gson = new Gson();

    public DataManager(RosePlugin plugin) {
        super(plugin);
    }

    /**
     * Update a user's current tag
     *
     * @param uuid The player's uuid
     * @param tag  The tag
     */
    public void saveUser(UUID uuid, @NotNull Tag tag) {
        this.cachedUsers.put(uuid, tag);
        final var query = "REPLACE INTO " + this.getTablePrefix() + "tags (player, tagID) VALUES (?, ?)";
        this.async(task -> this.databaseConnector.connect(connection -> {
            try (var statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, tag.getId());
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
        for (var entry : this.cachedUsers.entrySet()) {
            if (entry.getValue() == null)
                continue;

            if (entry.getValue().getId().equalsIgnoreCase(id))
                this.cachedUsers.remove(entry.getKey());
        }

        final var query = "DELETE FROM " + this.getTablePrefix() + "tags WHERE tagID = ?";
        this.async(task -> this.databaseConnector.connect(connection -> {
            try (var statement = connection.prepareStatement(query)) {
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
        players.forEach(player -> this.cachedUsers.put(player, tag));

        this.async(task -> this.databaseConnector.connect(connection -> {
            final var query = "REPLACE INTO " + this.getTablePrefix() + "tags (player, tagID) VALUES (?, ?)";
            try (var statement = connection.prepareStatement(query)) {
                for (UUID player : players) {
                    statement.setString(1, player.toString());
                    statement.setString(2, tag.getId());
                    statement.executeUpdate();
                }
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

        final var query = "DELETE FROM " + this.getTablePrefix() + "tags WHERE player = ?";
        this.async(task -> this.databaseConnector.connect(connection -> {
            try (var statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                statement.executeUpdate();
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
        var favourites = this.rosePlugin.getManager(TagsManager.class).getUsersFavourites(uuid);
        favourites.put(tag.getId(), tag);
        this.cachedFavourites.put(uuid, new HashSet<>(favourites.values()));

        this.async(task -> this.databaseConnector.connect(connection -> {
            final var query = "INSERT INTO " + this.getTablePrefix() + "favourites (player, tagID) VALUES (?, ?)";
            try (var statement = connection.prepareStatement(query)) {
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

        final var favourites = this.rosePlugin.getManager(TagsManager.class).getUsersFavourites(uuid);
        favourites.remove(tag.getId());
        this.cachedFavourites.put(uuid, new HashSet<>(favourites.values()));

        this.async(task -> this.databaseConnector.connect(connection -> {
            final var query = "DELETE FROM " + this.getTablePrefix() + "favourites WHERE player = ? AND tagID = ?";
            try (var statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, tag.getId());
                statement.executeUpdate();
            }
        }));

    }

    public void clearFavourites(UUID uuid) {
        this.cachedUsers.remove(uuid);

        this.async(task -> this.databaseConnector.connect(connection -> {
            final var query = "DELETE FROM " + this.getTablePrefix() + "favourites WHERE player = ?";
            try (var statement = connection.prepareStatement(query)) {
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
    public void loadUser(UUID uuid) {
        final var manager = this.rosePlugin.getManager(TagsManager.class);
        final Set<Tag> favouriteTags = new HashSet<>();

        this.async(task -> this.databaseConnector.connect(connection -> {
            final var selectTag = "SELECT tagID FROM " + this.getTablePrefix() + "tags WHERE player = ?";

            // Load the active tag from the database.
            try (var statement = connection.prepareStatement(selectTag)) {
                statement.setString(1, uuid.toString());
                final var result = statement.executeQuery();
                if (result.next()) {
                    this.cachedUsers.put(uuid, manager.getTagFromId(result.getString(1)));
                }
            }

            final var favouriteTagsQuery = "SELECT tagID FROM " + this.getTablePrefix() + "favourites WHERE player = ?";
            try (var statement = connection.prepareStatement(favouriteTagsQuery)) {
                statement.setString(1, uuid.toString());
                final ResultSet result = statement.executeQuery();
                while (result.next()) {
                    favouriteTags.add(manager.getTagFromId(result.getString(1)));
                    this.cachedFavourites.put(uuid, favouriteTags);
                }
            }
        }));
    }


    /**
     * Load all tag data from the database.
     *
     * @return A map of all the tag data.
     */
    public void loadTagData(Map<String, Tag> cachedTags) {
        cachedTags.clear();

        this.async(task -> this.databaseConnector.connect(connection -> {
            final var query = "SELECT * FROM " + this.getTablePrefix() + "tag_data";
            try (var statement = connection.prepareStatement(query)) {

                final var result = statement.executeQuery();
                while (result.next()) {
                    final var id = result.getString("tagId");
                    final var description = gson.fromJson(result.getString("description"), TagDescription.class).getDescription();
                    final var icon = result.getString("icon");

                    var tag = new Tag(id, result.getString("name"), result.getString("tag"));
                    tag.setPermission(result.getString("permission"));
                    tag.setDescription(description);
                    tag.setOrder(result.getInt("order"));
                    tag.setIcon(icon == null ? null : Material.valueOf(icon));
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
            final var query = "REPLACE INTO " + this.getTablePrefix() + "tag_data (tagId, `name`, description, tag, permission, `order`, icon) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (var statement = connection.prepareStatement(query)) {
                statement.setString(1, tag.getId());
                statement.setString(2, tag.getName());
                statement.setString(3, gson.toJson(new TagDescription(tag.getDescription())));
                statement.setString(4, tag.getTag());
                statement.setString(5, tag.getPermission());
                statement.setInt(6, tag.getOrder());
                statement.setString(7, tag.getIcon() != null ? tag.getIcon().name() : null);
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
            final var query = "REPLACE INTO " + this.getTablePrefix() + "tag_data (tagId, `name`, description, tag, permission, `order`, icon) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (var statement = connection.prepareStatement(query)) {
                for (var tag : tags.values()) {
                    statement.setString(1, tag.getId());
                    statement.setString(2, tag.getName());
                    statement.setString(3, gson.toJson(new TagDescription(tag.getDescription())));
                    statement.setString(4, tag.getTag());
                    statement.setString(5, tag.getPermission());
                    statement.setInt(6, tag.getOrder());
                    statement.setString(7, tag.getIcon() != null ? tag.getIcon().name() : null);
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
            final var query = "DELETE FROM " + this.getTablePrefix() + "tag_data WHERE tagId = ?";
            try (var statement = connection.prepareStatement(query)) {
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
            final var query = "DELETE FROM " + this.getTablePrefix() + "tag_data";
            try (var statement = connection.prepareStatement(query)) {
                statement.executeUpdate();
            }
        }));
    }

    @Override
    public List<Class<? extends DataMigration>> getDataMigrations() {
        return Arrays.asList(_1_CreateInitialTables.class, _2_CreateNewTagTables.class);
    }

    private void async(Consumer<BukkitTask> callback) {
        this.rosePlugin.getServer().getScheduler().runTaskAsynchronously(rosePlugin, callback);
    }

    public Map<UUID, Tag> getCachedUsers() {
        return cachedUsers;
    }

    public Map<UUID, Set<Tag>> getCachedFavourites() {
        return cachedFavourites;
    }

}
