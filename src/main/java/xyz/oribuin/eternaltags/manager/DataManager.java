package xyz.oribuin.eternaltags.manager;

import org.bukkit.configuration.file.FileConfiguration;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class DataManager extends Manager {

    private final EternalTags plugin = (EternalTags) this.getPlugin();
    private final Map<UUID, Tag> cachedUsers = new HashMap<>();
    private DatabaseConnector connector;

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
            final String query = "CREATE TABLE IF NOT EXISTS eternaltags_tags (player VARCHAR(50), tagID TEXT, PRIMARY KEY(player))";

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.executeUpdate();
            }

            this.cacheUsers();
        }));

    }

    private void cacheUsers() {

        // Get tag manager
        final TagManager tags = this.plugin.getManager(TagManager.class);

        // Make sure tags are registered.
        CompletableFuture.runAsync(tags::cacheTags).thenRunAsync(() -> {

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
                        final Tag tag = tags.getTags().stream().filter(x -> x.getId().equalsIgnoreCase(tagId)).findAny().orElse(null);

                        this.cachedUsers.put(player, tag);
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

    private void removeUser(final UUID uuid) {
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
     * Get a users current tag.
     *
     * @param uuid The player's uuid
     * @return The tag
     */
    public Tag getTag(UUID uuid) {
        return this.cachedUsers.get(uuid);
    }

    @Override
    public void disable() {
        this.connector.closeConnection();
    }

    public void async(Consumer<BukkitTask> callback) {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(plugin, callback);
    }

    public Map<UUID, Tag> getCachedUsers() {
        return cachedUsers;
    }

}
