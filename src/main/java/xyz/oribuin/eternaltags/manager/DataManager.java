package xyz.oribuin.eternaltags.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.database.DatabaseConnector;
import xyz.oribuin.orilibrary.database.MySQLConnector;
import xyz.oribuin.orilibrary.database.SQLiteConnector;
import xyz.oribuin.orilibrary.manager.Manager;
import xyz.oribuin.orilibrary.util.FileUtils;

import javax.annotation.Nullable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DataManager extends Manager {

    private final EternalTags plugin = (EternalTags) this.getPlugin();
    private final TagManager tagManager = this.plugin.getManager(TagManager.class);
    private DatabaseConnector connector;

    private final Map<UUID, Tag> cachedUsers = new HashMap<>();
    private final Map<UUID, Set<Tag>> cachedFavourites = new HashMap<>();

    public DataManager(final EternalTags plugin) {
        super(plugin);
    }

    @Override
    public void enable() {
        final FileConfiguration config = this.plugin.getConfig();

        if (config.getBoolean("mysql.enabled")) {
            // Define all the MySQL Values.
            String hostName = config.getString("mysql.host");
            int port = config.getInt("mysql.port");
            String dbname = config.getString("mysql.dbname");
            String username = config.getString("mysql.username");
            String password = config.getString("mysql.password");
            boolean ssl = config.getBoolean("mysql.ssl");

            // Connect to MySQL.
            this.connector = new MySQLConnector(this.plugin, hostName, port, dbname, username, password, ssl);
            this.plugin.getLogger().info("Using MySQL for Database ~ " + hostName + ":" + port);
        } else {

            // Create the database File
            FileUtils.createFile(this.plugin, "eternaltags.db");

            // Connect to SQLite
            this.connector = new SQLiteConnector(this.plugin, "eternaltags.db");
            this.getPlugin().getLogger().info("Using SQLite for Database ~ eternaltags.db");
        }

        this.async(task -> this.connector.connect(connection -> {
            final String baseTable = "CREATE TABLE IF NOT EXISTS eternaltags_tags (player VARCHAR(50), tagID TEXT, PRIMARY KEY(player))";

            try (PreparedStatement statement = connection.prepareStatement(baseTable)) {
                statement.executeUpdate();
            }

            final String favouriteTable = "CREATE TABLE IF NOT EXISTS eternaltags_favourites (player VARCHAR(50), tagID TEXT)";
            try (PreparedStatement statement = connection.prepareStatement(favouriteTable)) {
                statement.executeUpdate();
            }

            this.cacheUsers();
            this.cacheFavourites();
        }));

    }

    private void cacheUsers() {
        // Make sure tags are registered.
        CompletableFuture.runAsync(tagManager::cacheTags).thenRunAsync(() -> {

            this.cachedUsers.clear();
            final String query = "SELECT * FROM eternaltags_tags";

            // Get all users from the database.
            this.connector.connect(connection -> {

                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    final ResultSet result = statement.executeQuery();

                    while (result.next()) {
                        final String tagId = result.getString("tagID");
                        final UUID player = UUID.fromString(result.getString("player"));

                        // Check if tag is available before adding it to map
                        final Tag tag = tagManager.getTags().stream().filter(x -> x.getId().equalsIgnoreCase(tagId)).findAny().orElse(null);

                        this.cachedUsers.put(player, tag);
                    }

                }

            });

        });
    }

    private void cacheFavourites() {
        this.async((t) -> {
            this.cachedUsers.clear();

            this.connector.connect(connection -> {
                final String query = "SELECT * FROM eternaltags_favourites";

                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    final ResultSet result = statement.executeQuery();

                    // Ori and getting a collection of stuff literally never goes well but thats fine.
                    while (result.next()) {
                        final UUID uuid = UUID.fromString(result.getString("player"));
                        final Set<Tag> favouritedTags = this.cachedFavourites.getOrDefault(uuid, new HashSet<>());

                        // inb4 StackOverflowException or ConcurrentModificationException
                        Optional<Tag> optional = tagManager.getTagFromID(result.getString("tagID"));
                        optional.ifPresent(favouritedTags::add);
                        this.cachedFavourites.put(uuid, favouritedTags);
                    }
                }
            });

        });

    }

    /**
     * Update a user's current tag
     *
     * @param uuid The player's uuid
     * @param tag  The tag
     */
    public void updateUser(final UUID uuid, final @Nullable Tag tag) {

        if (tag == null) {
            this.removeUser(uuid);
            return;
        }

        // Save the tag if it doesnt exist in the config file.
        if (!tagManager.getTags().contains(tag))
            tagManager.createTag(tag);

        this.cachedUsers.put(uuid, tag);
        final String query = "REPLACE INTO eternaltags_tags (player, tagID) VALUES (?, ?)";
        this.async(task -> this.connector.connect(connection -> {

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
    public void updateEveryone(Tag tag) {
        Set<Map.Entry<UUID, Tag>> entry = new HashSet<>(this.cachedUsers.entrySet());

        entry.forEach(uuidTagEntry -> this.cachedUsers.put(uuidTagEntry.getKey(), tag));

        this.async(task -> this.connector.connect(connection -> {
            final String query = "UPDATE eternaltags_tags SET tagID = ?";

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

        final String query = "DELETE FROM eternaltags_tags WHERE player = ?";
        this.async(task -> this.connector.connect(connection -> {
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
        final Set<Tag> favourites = this.getFavourites(uuid);
        favourites.add(tag);
        this.cachedFavourites.put(uuid, favourites);

        this.async(task -> this.connector.connect(connection -> {
            final String query = "INSERT INTO eternaltags_favourites (player, tagID) VALUES (?, ?)";
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
        final Set<Tag> favourites = this.getFavourites(uuid);
        favourites.removeIf(x -> x.getId().equalsIgnoreCase(tag.getId()));
        this.cachedFavourites.put(uuid, favourites);

        this.async(task -> this.connector.connect(connection -> {
            final String query = "DELETE FROM eternaltags_favourites WHERE player = ? AND tagID = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                statement.setString(2, tag.getId());
                statement.executeUpdate();
            }
        }));

    }

    /**
     * Get a users current tag.
     *
     * @param uuid The player's uuid
     * @return The tag
     */
    public Tag getTag(UUID uuid) {
        return this.cachedUsers.getOrDefault(uuid, null);
    }

    public Set<Tag> getFavourites(UUID uuid) {
        return this.cachedFavourites.getOrDefault(uuid, new HashSet<>());
    }

    public Set<Tag> getFavourites(Player player) {
        return this.getFavourites(player.getUniqueId())
                .stream()
                .filter(tag -> player.hasPermission(tag.getPermission()))
                .collect(Collectors.toSet());

    }

    @Override
    public void disable() {
        this.connector.closeConnection();
    }

    private void async(Consumer<BukkitTask> callback) {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(plugin, callback);
    }

    public Map<UUID, Tag> getCachedUsers() {
        return cachedUsers;
    }

    public Map<UUID, Set<Tag>> getCachedFavourites() {
        return cachedFavourites;
    }

}
