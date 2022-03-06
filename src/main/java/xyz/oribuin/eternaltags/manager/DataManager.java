package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.manager.AbstractDataManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.database.migration._1_CreateInitialTables;
import xyz.oribuin.eternaltags.listener.PlayerListeners;
import xyz.oribuin.eternaltags.obj.Tag;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
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
     * Change every single person in the database's current tag.
     *
     * @param tag The tag.
     */
    public void updateEveryone(Tag tag, List<Player> players) {
        players.forEach(player -> this.cachedUsers.put(player.getUniqueId(), tag));

        this.async(task -> this.databaseConnector.connect(connection -> {
            final String query = "UPDATE " + this.getTablePrefix() + "tags SET tagID = ?";

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, tag.getId());
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
     * Add a tag to a player's favourite tags.
     *
     * @param uuid The uuid of the player
     * @param tag  The tag being added
     */
    public void addFavourite(UUID uuid, Tag tag) {
        Map<String, Tag> favourites = this.rosePlugin.getManager(TagsManager.class).getUsersFavourites(uuid);
        favourites.put(tag.getId(), tag);
        this.cachedFavourites.put(uuid, new HashSet<>(favourites.values()));

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

        final Map<String, Tag> favourites = this.rosePlugin.getManager(TagsManager.class).getUsersFavourites(uuid);
        favourites.remove(tag.getId());
        this.cachedFavourites.put(uuid, new HashSet<>(favourites.values()));

        this.async(task -> this.databaseConnector.connect(connection -> {
            final String query = "DELETE FROM " + this.getTablePrefix() + "favourites WHERE player = ? AND tagID = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, tag.getId());
                statement.executeUpdate();
            }
        }));

    }

    /**
     * Load & Cache a user's current active tag.
     *
     * @param uuid The player's uuid
     */
    public void loadUser(UUID uuid) {
        this.async(task -> this.databaseConnector.connect(connection -> {
            final String query = "SELECT tagID FROM " + this.getTablePrefix() + "tags WHERE player = ?";

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                final ResultSet result = statement.executeQuery();
                if (result.next()) {
                    this.rosePlugin.getManager(TagsManager.class).matchTagFromID(result.getString(1)).ifPresent(tag -> this.cachedUsers.put(uuid, tag));
                }
            }
        }));
    }

    /**
     * Load a user's favourite tags from the database.
     *
     * @param uuid The player's UUID
     */
    public void loadFavourites(UUID uuid) {
        final Set<Tag> tags = new HashSet<>();
        this.async(task -> this.databaseConnector.connect(connection -> {

            // Select all the favourite tags from the database set by the player.
            final String query = "SELECT tagID FROM " + this.getTablePrefix() + "favourites WHERE player = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                final ResultSet result = statement.executeQuery();

                final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
                while (result.next()) {
                    manager.matchTagFromID(result.getString("tagID")).ifPresent(tags::add);
                    this.cachedFavourites.put(uuid, tags);
                }
            }

        }));
    }

    @Override
    public List<Class<? extends DataMigration>> getDataMigrations() {
        return Collections.singletonList(_1_CreateInitialTables.class);
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
