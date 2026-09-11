package dev.oribuin.eternaltags.manager;

import dev.oribuin.eternaltags.EternalTags;
import dev.oribuin.eternaltags.config.Setting;
import dev.oribuin.eternaltags.obj.Tag;
import dev.oribuin.eternaltags.obj.TagUser;
import dev.oribuin.eternaltags.util.TagsUtils;
import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.manager.Manager;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.events.CommentEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static dev.oribuin.eternaltags.config.Setting.TAG_FORMATTING;

public class TagsManager extends Manager {

    public static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();
    public static final Path TAGS_FOLDER = EternalTags.get().getDataPath().resolve("tags");

    private final Map<String, Tag> tagCache = new HashMap<>();
    private final Map<Path, CommentedFileConfiguration> configCache = new HashMap<>();
    private Path defaultFile;

    public TagsManager(RosePlugin plugin) {
        super(plugin);
    }

    @Override
    public void reload() {
        DataManager dataManager = this.rosePlugin.getManager(DataManager.class);

        // Establish the default files 
        File folder = TAGS_FOLDER.toFile();
        File[] files = folder.listFiles();
        if (!folder.exists() || files == null || files.length == 0) {
            TagsUtils.createFile(this.rosePlugin, "tags", "default.yml");
            TagsUtils.createFile(this.rosePlugin, "tags", "dynamic.yml");
            TagsUtils.createFile(this.rosePlugin, "tags", "pride.yml");
        }

        this.defaultFile = TAGS_FOLDER.resolve(Setting.DEFAULT_FILE.get());
        this.configCache.putIfAbsent(this.defaultFile, CommentedFileConfiguration.loadConfiguration(defaultFile.toFile()));

        // Load all the config files <3
        CompletableFuture.runAsync(() -> {
            List<File> results = this.searchFolder(folder);
            if (results.isEmpty()) {
                EternalTags.get().getLogger().severe("We were unable to detect any files in your /EternalTags/tags/ folder, Can you make sure they exist?");
                return;
            }

            for (File file : results) {
                if (!file.getName().endsWith(".yml")) continue;

                CommentedFileConfiguration config = this.configCache.computeIfAbsent(
                        file.toPath(),
                        path -> CommentedFileConfiguration.loadConfiguration(path.toFile())
                );

                CommentedConfigurationSection section = config.getConfigurationSection("tags");
                if (section == null) continue;

                section.getKeys(false).forEach(tagId -> {
                    Tag tag = Tag.fromConfig(section, tagId);
                    if (tag == null) return;

                    tag.setDestination(file.toPath());
                    this.tagCache.put(tag.getId(), tag);
                });
            }

            // Load users here in a better, less ugly way    
            List<Player> users = Bukkit.getOnlinePlayers()
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Load all the users from the database
            List<UUID> uuids = users.stream().map(Player::getUniqueId).toList();
            dataManager.loadUsers(uuids);

            // Get each user and load their tags
            users.forEach(this::getUserTag);
        });
    }

    @Override
    public void disable() {
        this.tagCache.clear();
    }

    /**
     * Search all the files in a directory, return them as a list of files
     *
     * @param file The directory to load files from
     * @return A list of files in the directory
     */
    private List<File> searchFolder(File file) {
        // If the file is not a directory, check if it is a yml file, if it is, return a list containing the file
        if (!file.isDirectory()) {
            if (file.getName().endsWith(".yml")) return List.of(file);
            return List.of();
        }

        // If the file is a directory, return a list of all files in the directory
        List<File> files = new ArrayList<>();
        File[] listFiles = file.listFiles();
        if (listFiles == null) return files;

        for (File f : listFiles) {
            files.addAll(searchFolder(f));
        }

        return files;
    }

    /**
     * Update every player's with a specific tag with a new one
     *
     * @param tag The tag
     */
    public void updateActiveTag(Tag tag) {
        DataManager data = this.rosePlugin.getManager(DataManager.class);

        data.getCachedUsers().values().forEach(user -> {
            if (user.getActiveTag() != null && user.getActiveTag().equalsIgnoreCase(tag.getId())) {
                user.setActiveTag(tag.getId());
                data.updateCachedUser(user);
            }
        });
    }

    /**
     * Write a tag into the config & cache
     *
     * @param tag The tag being saved.
     */
    public void writeTag(@NotNull Tag tag) {
        if (!this.tagCache.containsKey(tag.getId()) || tag.getDestination() == null) {
            this.createTag(this.defaultFile.toFile(), tag);
            return;
        }

        // region Save the tag into it's designated file
        File file = tag.getDestination().toFile();
        CommentedFileConfiguration config = this.configCache.get(tag.getDestination());
        if (config != null) {
            CommentedConfigurationSection section = this.getTagSection(config);
            Tag.SERIALIZER.write(section, tag.getId(), tag);
            config.save(file);
        }
        // endregion

        this.tagCache.put(tag.getId(), tag);
        this.updateActiveTag(tag);
    }

    /**
     * Write a tag into the config & cache
     *
     * @param identifier The tag being saved.
     */
    public void writeTag(String identifier) {
        Tag tag = this.tagCache.get(identifier);
        if (tag != null) this.writeTag(tag);
    }

    /**
     * Delete a tag from the config & cache by object.
     *
     * @param tag The tag being deleted.
     */
    public void deleteTag(@NotNull Tag tag) {
        this.tagCache.remove(tag.getId());
        if (tag.getDestination() == null) return;

        File file = tag.getDestination().toFile();
        CommentedFileConfiguration config = this.configCache.get(tag.getDestination());
        if (config == null) return;

        CommentedConfigurationSection section = this.getTagSection(config);
        section.set(tag.getId(), null);
        config.save(file);
    }

    /**
     * Delete the tag from the server files and cache
     *
     * @param identifier The id of the tag being deleted
     */
    public void deleteTag(String identifier) {
        Tag tag = this.tagCache.get(identifier);
        if (tag != null) this.deleteTag(tag);
    }

    /**
     * Create a new tag for the plugin
     *
     * @param file The file to create tags in
     * @param tag  The tag for it
     */
    public void createTag(@NotNull File file, @NotNull Tag tag) {
        try {
            if (!file.exists()) file.createNewFile();
        } catch (IOException ignored) {
        }

        CommentedFileConfiguration config = this.configCache.computeIfAbsent(
                file.toPath(), 
                path -> CommentedFileConfiguration.loadConfiguration(file)
        );
        CommentedConfigurationSection section = this.getTagSection(config);
        String tagId = tag.getId().toLowerCase();

        Tag.SERIALIZER.write(section, tagId, tag);
        config.save(file);
        tag.setDestination(file.toPath());
        this.tagCache.put(tagId, tag);
    }

    /**
     * Get a tag by the UUID, load the user if they aren't cached.
     *
     * @param uuid The UUID of the player.
     * @return The active tag if present
     * @deprecated Use {@link TagsManager#getUserTag(UUID)} instead.
     */
    @Nullable
    @Deprecated
    public Tag getTagFromUUID(UUID uuid) {
        return this.getUserTag(uuid);
    }

    /**
     * Get a tag by the UUID, If the user isn't cached, return null.
     *
     * @param uuid The UUID of the player.
     * @return The active tag if present
     */
    @Nullable
    public Tag getUserTag(@NotNull UUID uuid) {
        TagUser user = this.rosePlugin.getManager(DataManager.class).getCachedUser(uuid);

        return this.getTagFromId(user.getActiveTag());
    }

    /**
     * Get a tag by the player, this method is intended to be used for checking
     * if a player needs to have their tag updated. (Remove Inactive Tags or Default Tags)
     *
     * @param player The player object.
     * @return The active tag if present
     */
    @Nullable
    public Tag getUserTag(Player player) {
        if (player == null) return null;

        DataManager data = this.rosePlugin.getManager(DataManager.class);
        TagUser user = data.getCachedUsers().computeIfAbsent(player.getUniqueId(), TagUser::new);
        return this.getTagFromId(user.getActiveTag());
    }

    /**
     * Get a tag by the offline player object, If the user isn't cached, return null.
     *
     * @param player The offline player object.
     * @return The active tag if present
     * @since 1.1.6
     */
    @Nullable
    public Tag getOfflineUserTag(@NotNull OfflinePlayer player) {
        return this.getUserTag(player.getUniqueId());
    }

    /**
     * Remove a user's current active tag.
     *
     * @param uuid The UUID of the player.
     */
    public void clearTag(UUID uuid) {
        this.rosePlugin.getManager(DataManager.class).removeUser(uuid);
    }

    /**
     * Add & cache a user's favourite tag
     *
     * @param uuid The UUID of the tag.
     * @param tag  The tag being added
     */
    public void addFavourite(UUID uuid, Tag tag) {
        this.rosePlugin.getManager(DataManager.class).addFavourite(uuid, tag);
    }

    /**
     * Add & cache a user's favourite tag
     *
     * @param uuid The UUID of the tag.
     * @param tag  The tag being added
     */
    public void removeFavourite(UUID uuid, Tag tag) {
        this.rosePlugin.getManager(DataManager.class).removeFavourite(uuid, tag);
    }

    /**
     * Get a user's favourite tags.
     *
     * @param uuid The UUID of the player.
     * @return The map of favourite tags.
     */
    @NotNull
    public Map<String, Tag> getUsersFavourites(UUID uuid) {
        Map<String, Tag> favourites = new HashMap<>();
        TagUser user = this.rosePlugin.getManager(DataManager.class).getCachedUser(uuid);

        user.getFavourites().stream().filter(Objects::nonNull).forEach(tag -> favourites.put(tag, this.getTagFromId(tag)));

        return favourites;
    }

    /**
     * Get all the tags a player has permission to use.
     *
     * @param player The player
     * @return The tags the player has.
     */
    @NotNull
    public List<Tag> getPlayerTags(@Nullable Player player) {
        if (player == null) return new ArrayList<>();

        return this.getCachedTags().values().stream().filter(entry -> this.canUseTag(player, entry)).collect(Collectors.toList());
    }

    /**
     * Check if a tag exists from the id.
     *
     * @param id The id of the tag.
     * @return true if the tag exists.
     */
    public boolean checkTagExists(String id) {
        return this.tagCache.containsKey(id);
    }

    /**
     * Match a tag based on the id.
     *
     * @param id The id of the tag.
     * @return An optional tag.
     */
    @Nullable
    public Tag getTagFromId(@Nullable String id) {
        if (id == null) return null;
        return this.tagCache.get(id);
    }

    /**
     * Check if a tag is favourite by ap layer
     *
     * @param player The player
     * @param tag    The tag
     * @return If the tag is favourited.
     */
    public boolean isFavourite(UUID player, Tag tag) {
        return this.getUsersFavourites(player).get(tag.getId()) != null;
    }

    /**
     * Change everyone's active tag to one specific tag.
     *
     * @param tag The tag
     */
    public void setEveryone(Tag tag) {
        this.rosePlugin.getManager(DataManager.class).updateUsers(tag,
                new ArrayList<>(Bukkit.getOnlinePlayers()
                        .stream()
                        .map(Player::getUniqueId)
                        .collect(Collectors.toList())
                ));
    }

    /**
     * Get a randomized tag from a user's available tags.
     *
     * @param offlinePlayer The offlinePlayer
     * @return The random tag.
     */
    public Tag getRandomTag(@Nullable OfflinePlayer offlinePlayer) {
        List<Tag> tags = new ArrayList<>(this.getCachedTags().values());

        if (offlinePlayer != null && offlinePlayer.getPlayer() != null) tags = this.getPlayerTags(offlinePlayer.getPlayer());
        if (tags.isEmpty()) return null;

        return tags.get(RANDOM.nextInt(tags.size()));
    }

    /**
     * Get the display version of a tag using placeholderapi
     *
     * @param tag         The tag.
     * @param player      The player.
     * @param placeholder The placeholder.
     * @return The display tag.
     */
    public String getDisplayTag(@Nullable Tag tag, OfflinePlayer player, @NotNull String placeholder) {
        if (tag == null) return placeholder; // Return the placeholder if the tag is null

        StringPlaceholders.Builder placeholders = StringPlaceholders.builder();
        placeholders.addAll(this.getTagPlaceholders(tag));
        placeholders.add("tag", tag.getContent());

        return TagsUtils.colorAsString(PlaceholderAPI.setPlaceholders(player, placeholders.build().apply(TAG_FORMATTING.get())));
    }

    /**
     * Get the display version of a tag using placeholderapi
     *
     * @param tag    The tag.
     * @param player The player.
     * @return The display tag.
     */
    public String getDisplayTag(@Nullable Tag tag, OfflinePlayer player) {
        return this.getDisplayTag(tag, player, ""); // Empty placeholder string
    }

    /**
     * Clear of a player's favourite tags.
     *
     * @param uuid The UUID of the player.
     */
    public void clearFavourites(UUID uuid) {
        this.rosePlugin.getManager(DataManager.class).clearFavourites(uuid);
    }

    /**
     * Check if a player has access to a tag
     *
     * @param player The player
     * @param tag    The tag
     * @return If the player has access to the tag
     */
    public boolean canUseTag(@NotNull Player player, @NotNull Tag tag) {
        return tag.hasPermission(player);
    }

    /**
     * Get the tags section from an existing config
     *
     * @param section The section or config to load from
     * @return The resulting section
     */
    public CommentedConfigurationSection getTagSection(@NotNull CommentedConfigurationSection section) {
        CommentedConfigurationSection result = section.getConfigurationSection("tags");
        if (result == null) section.createSection("tags");
        return result;
    }

    /**
     * Get the tag placeholders for the given player
     *
     * @param tag The tag
     * @return The tag placeholders
     */
    private StringPlaceholders getTagPlaceholders(Tag tag) {
        return StringPlaceholders.builder()
                .add("id", tag.getId())
                .add("name", tag.getName())
//                .add("description", String.join(Setting.DESCRIPTION_DELIMITER.getString(), tag.getDescription()))
                .add("permission", tag.getPermission()).add("order", tag.getOrder()).build();
    }

    public Map<String, Tag> getCachedTags() {
        return this.tagCache;
    }

}
