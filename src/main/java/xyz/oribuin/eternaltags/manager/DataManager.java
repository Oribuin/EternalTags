package xyz.oribuin.eternaltags.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.manager.DataHandler;

import javax.annotation.Nullable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DataManager extends DataHandler {

    private final EternalTags plugin = (EternalTags) this.getPlugin();
    private final TagManager tagManager = this.plugin.getManager(TagManager.class);

    private final Map<UUID, Tag> cachedUsers = new HashMap<>();
    private final Map<UUID, Set<Tag>> cachedFavourites = new HashMap<>();
    private boolean removeInaccessible = false;

    public DataManager(EternalTags plugin) {
        super(plugin);
    }

    @Override
    public void enable() {
        super.enable();
        this.removeInaccessible = this.plugin.getConfig().getBoolean("remove-inaccessible-tags");

        this.async(task -> this.getConnector().connect(connection -> {
            final String baseTable = "CREATE TABLE IF NOT EXISTS " + this.getTableName() + "_tags (player VARCHAR(50), tagID TEXT, PRIMARY KEY(player))";
            connection.prepareStatement(baseTable).executeUpdate();

            final String favouriteTable = "CREATE TABLE IF NOT EXISTS " + this.getTableName() + "_favourites (player VARCHAR(50), tagID TEXT)";
            connection.prepareStatement(favouriteTable).executeUpdate();
        }));
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

        // Save the tag if it doesn't exist in the config file.
        if (!tagManager.getTags().contains(tag))
            tagManager.createTag(tag);

        this.cachedUsers.put(uuid, tag);
        final String query = "REPLACE INTO " + this.getTableName() + "_tags (player, tagID) VALUES (?, ?)";
        this.async(task -> this.getConnector().connect(connection -> {

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

        this.async(task -> this.getConnector().connect(connection -> {
            final String query = "UPDATE " + this.getTableName() + "_tags SET tagID = ?";

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

        final String query = "DELETE FROM " + this.getTableName() + "_tags WHERE player = ?";
        this.async(task -> this.getConnector().connect(connection -> {
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

        this.async(task -> this.getConnector().connect(connection -> {
            final String query = "INSERT INTO " + this.getTableName() + "_favourites (player, tagID) VALUES (?, ?)";
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

        this.async(task -> this.getConnector().connect(connection -> {
            final String query = "DELETE FROM " + this.getTableName() + "_favourites WHERE player = ? AND tagID = ?";
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
        // get the user's cached tag.
        if (this.cachedUsers.get(uuid) != null) {
            final Tag tag = this.cachedUsers.get(uuid);
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null)
                return tag;

            if (removeInaccessible && !player.hasPermission(tag.getPermission())) {
                this.removeUser(uuid);
                return null;
            }

            return tag;
        }

        // If the user's tag isnt cached, Cache it.
        this.async(task -> this.getConnector().connect(connection -> {
            final String query = "SELECT tagID FROM " + this.getTableName() + "_tags WHERE player = ?";

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                final ResultSet result = statement.executeQuery();
                if (result.next()) {
                    this.tagManager.getTagFromID(result.getString(1)).ifPresent(tag -> this.cachedUsers.put(uuid, tag));
                }
            }
        }));

        return this.cachedUsers.get(uuid);
    }

    /**
     * Get a user's current set favourite tags from the gui.
     *
     * @param uuid The UUID of the player.
     * @return The set of favourite tags.
     */
    public Set<Tag> getFavourites(UUID uuid) {
        if (this.cachedFavourites.get(uuid) != null)
            return this.cachedFavourites.get(uuid);

        final Set<Tag> tags = new HashSet<>();

        this.async(task -> this.getConnector().connect(connection -> {

            // Select all the favourite tags from the database set by the player.
            final String query = "SELECT tagID FROM " + this.getTableName() + "_favourites WHERE player = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                final ResultSet result = statement.executeQuery();

                while (result.next()) {
                    // keep recaching the tags.
                    Optional<Tag> optional = tagManager.getTagFromID(result.getString("tagID"));
                    optional.ifPresent(tags::add);
                    this.cachedFavourites.put(uuid, tags);
                }
            }

        }));

        return tags;
    }

    public Set<Tag> getFavourites(Player player) {
        return this.getFavourites(player.getUniqueId())
                .stream()
                .filter(tag -> player.hasPermission(tag.getPermission()))
                .collect(Collectors.toSet());

    }

    @Override
    public void disable() {
        super.disable();
    }

    private void async(Consumer<BukkitTask> callback) {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(plugin, callback);
    }

}
