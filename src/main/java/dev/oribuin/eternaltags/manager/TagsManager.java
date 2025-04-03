package dev.oribuin.eternaltags.manager;

import dev.oribuin.eternaltags.EternalTags;
import dev.oribuin.eternaltags.config.Setting;
import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.manager.Manager;
import dev.rosewood.rosegarden.utils.NMSUtil;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.oribuin.eternaltags.obj.Tag;
import dev.oribuin.eternaltags.obj.TagUser;
import dev.oribuin.eternaltags.util.TagsUtils;

import java.io.File;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static dev.oribuin.eternaltags.config.Setting.TAG_FORMATTING;

public class TagsManager extends Manager {

    public static final Path TAGS_FOLDER = EternalTags.get().getDataPath().resolve("tags");
    private final Map<String, Tag> cachedTags = new HashMap<>();
    private final Random random = new Random();

    public TagsManager(RosePlugin plugin) {
        super(plugin);
    }

    @Override
    public void reload() {
        DataManager dataManager = this.rosePlugin.getManager(DataManager.class);

        // TODO: Load all tags from MySQL instead of tags.yml
//        if (Setting.MYSQL_TAGDATA.getBoolean()) {
//            dataManager.loadTagData(this.cachedTags);
//            return;
//        }
//
        // Establish the default files 
        File folder = TAGS_FOLDER.toFile();
        if (!folder.exists() || folder.listFiles() == null || folder.listFiles().length == 0) {
            TagsUtils.createFile(this.rosePlugin, "tags", "default.yml");
            TagsUtils.createFile(this.rosePlugin, "tags", "dynamic.yml");
            
            if (NMSUtil.getVersionNumber() >= 21) { // todo: require 1.21.4 
                TagsUtils.createFile(this.rosePlugin, "tags", "pride.yml");
            }
        }

        // Load all the config files <3
        CompletableFuture.runAsync(() -> {
            List<File> results = this.searchFolder(folder);
            if (results.isEmpty()) {
                EternalTags.get().getLogger().severe("We were unable to detect any files in your /EternalTags/tags/ folder, Can you make sure they exist?");
                return;
            }

            results.forEach(this::loadFile);
        }).thenAccept(unused -> {

            // Load users here in a better, less ugly way    
            List<Player> users = Bukkit.getOnlinePlayers().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // Load all the users from the database
            dataManager.loadUsers(users.stream()
                    .map(Player::getUniqueId)
                    .collect(Collectors.toList())
            );

            // Get each user and load their tags
            users.forEach(this::getUserTag);
        });
    }

    @Override
    public void disable() {
        this.cachedTags.clear();
    }

    /**
     * Load all the items from the item directory into the items map
     *
     * @param file The directory to load items from
     */
    public void loadFile(File file) {
        System.out.println("Searching file: " + file.getPath());
        if (!file.getName().endsWith(".yml")) return; // check if the file is a yml file

        CommentedFileConfiguration config = CommentedFileConfiguration.loadConfiguration(file);
        CommentedConfigurationSection section = config.getConfigurationSection("tags");
        if (section == null) return;

        section.getKeys(false).forEach(tagId -> {
            Tag tag = Tag.fromConfig(file, section, tagId);
            System.out.println("Found the tag from config: " + tag);
            if (tag == null) return;

            this.cachedTags.put(tag.getId(), tag);
        });
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
     * Delete a tag from the config & cache by object.
     *
     * @param tag The tag being deleted.
     */
    public void deleteTag(Tag tag) {
        this.deleteTag(tag.getId().toLowerCase());
    }

    /**
     * Save a collection of tags at once.
     *
     * @param tags The tags being saved.
     */
    public void saveTags(Map<String, Tag> tags) {
        this.cachedTags.putAll(tags);

        // TODO: If MySQL Tags is enabled, save the tags to the database instead of the tags.yml
//        if (Setting.MYSQL_TAGDATA.getBoolean()) {
//            this.rosePlugin.getManager(DataManager.class).saveTagData(tags);
//            return;
//        }

        // Save the tags to the tags.yml
        CompletableFuture.runAsync(() -> tags.values().forEach(Tag::save));
    }

    /**
     * Delete a tag from the config & cache by id.
     *
     * @param id The id of the tag.
     */
    public void deleteTag(String id) {
        Tag tag = this.getTagFromId(id);
        if (tag == null) return;

        this.cachedTags.remove(id);
        this.rosePlugin.getManager(DataManager.class).clearTagForAll(id);

        // TODO: Delete the tag from the database if MySQL TagData is enabled.
//        if (Setting.MYSQL_TAGDATA.getBoolean()) {
//            this.rosePlugin.getManager(DataManager.class).deleteTagData(tag);
//            return;
//        }

        // Delete the tag from the tags.yml
        tag.delete();
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

        user.getFavourites().stream()
                .filter(Objects::nonNull)
                .forEach(tag -> favourites.put(tag, this.getTagFromId(tag)));

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
        if (player == null || player.hasPermission("eternaltags.tags.*"))
            return new ArrayList<>(this.cachedTags.values());

        return this.cachedTags.values().stream()
                .filter(entry -> this.canUseTag(player, entry))
                .collect(Collectors.toList());
    }

    /**
     * Check if a tag exists from the id.
     *
     * @param id The id of the tag.
     * @return true if the tag exists.
     */
    public boolean checkTagExists(String id) {
        return this.cachedTags.get(id.toLowerCase().replace(".", "_")) != null;
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

        return this.cachedTags.get(id.toLowerCase());
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
        this.rosePlugin.getManager(DataManager.class).updateUsers(tag, new ArrayList<>(
                Bukkit.getOnlinePlayers()
                        .stream()
                        .map(Player::getUniqueId)
                        .collect(Collectors.toList()))
        );
    }

    /**
     * Get a randomized tag from a user's available tags.
     *
     * @param offlinePlayer The offlinePlayer
     * @return The random tag.
     */
    public Tag getRandomTag(@Nullable OfflinePlayer offlinePlayer) {
        List<Tag> tags = new ArrayList<>(this.getCachedTags().values());

        if (offlinePlayer != null && offlinePlayer.getPlayer() != null)
            tags = this.getPlayerTags(offlinePlayer.getPlayer());

        if (tags.isEmpty())
            return null;

        return tags.get(random.nextInt(tags.size()));
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
        placeholders.add("tag", tag.getTag());

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
                .add("permission", tag.getPermission())
                .add("order", tag.getOrder())
                .build();
    }

    public Map<String, Tag> getCachedTags() {
        return cachedTags;
    }

}
