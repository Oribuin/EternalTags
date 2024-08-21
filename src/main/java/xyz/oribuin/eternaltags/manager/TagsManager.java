package xyz.oribuin.eternaltags.manager;

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
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.hook.VaultHook;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.obj.Category;
import xyz.oribuin.eternaltags.obj.CategoryType;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.obj.TagUser;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TagsManager extends Manager {

    public static final File TAG_FOLDER = new File(EternalTags.getInstance().getDataFolder(), "tags");

    private final Map<String, Tag> cachedTags = new HashMap<>();
    private final Random random = new Random();

    private Map<String, String> defaultTagGroups;
    private boolean isDefaultTagEnabled;

    public TagsManager(RosePlugin plugin) {
        super(plugin);
    }

    @Override
    public void reload() {
        DataManager dataManager = this.rosePlugin.getManager(DataManager.class);

        // Load the default tag groups
        this.defaultTagGroups = new HashMap<>();
        CommentedConfigurationSection groupSection = Setting.DEFAULT_TAG_GROUPS.getSection();
        groupSection.getKeys(false).forEach(s -> this.defaultTagGroups.put(s.toLowerCase(), groupSection.getString(s)));

        // Check if we're using default tags
        this.isDefaultTagEnabled = this.usingGroupDefaults() || !Setting.DEFAULT_TAG.getString().equalsIgnoreCase("none");

        // Load all tags from mysql instead of tags.yml
        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            dataManager.loadTagData(this.cachedTags);
            return;
        }

        // If the tags.yml doesnt exist,
        if (!TAG_FOLDER.exists()) {
            TAG_FOLDER.mkdirs();

            // If the tags.yml file exists, move it to the tags folder otherwise create a new one.
            File tagsFile = new File(this.rosePlugin.getDataFolder(), "tags.yml");

            if (tagsFile.exists()) {
                TagsUtils.relocateFile(tagsFile, TAG_FOLDER);
            } else {
                TagsUtils.createFile(this.rosePlugin, "tags", "default.yml");
                TagsUtils.createFile(this.rosePlugin, "tags", "placeholders.yml");
            }
        }

        this.cachedTags.clear();
        this.loadFolder(TAG_FOLDER);

        // Load all the users from the database
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
    }

    @Override
    public void disable() {
        this.cachedTags.clear();
    }

    /**
     * Load all the tags from the tags folder.
     * This method recursively loads all the tags from the tags folder.
     *
     * @param folder The folder to load from.
     */
    public void loadFolder(File folder) {
        if (folder == null || !folder.exists()) return;

        File[] files = folder.listFiles();
        if (files == null || files.length == 0) return;

        Arrays.asList(files).forEach(file -> {
            // Load the file if it's a .yml file
            if (file.getName().endsWith(".yml")) {
                this.loadFile(file);
                return;
            }

            // Load the folder if it's a directory
            if (file.isDirectory()) {
                this.loadFolder(file);
            }
        });
    }

    /**
     * Load all the tags from a folder into the cache.
     *
     * @param file The file to load from.
     */
    public void loadFile(File file) {
        if (file == null || !file.exists()) return;

        CommentedFileConfiguration config = CommentedFileConfiguration.loadConfiguration(file);
        CommentedConfigurationSection section = config.getConfigurationSection("tags");
        if (section == null) return;

        section.getKeys(false).forEach(key -> this.loadTag(section, key));
    }

    /**
     * Load a tag from a configuration section and cache it.
     *
     * @param section The configuration section
     * @param key     The key to load.
     */
    private void loadTag(CommentedConfigurationSection section, String key) {
        CategoryManager manager = this.rosePlugin.getManager(CategoryManager.class);

        String name = section.getString(key + ".name", key);
        String text = section.getString(key + ".tag");

        if (name == null || text == null) return;
        Tag tag = new Tag(key.toLowerCase(), name, text);
        tag.setDescription(section.getStringList(key + ".description"));
        tag.setPermission(section.getString(key + ".permission"));
        tag.setOrder(section.getInt(key + ".order", -1));

        Category category = manager.getCategory(section.getString(key + ".category"));
        Category defaultCategory = manager.getFirst(CategoryType.DEFAULT);

        // Assign the category to the tag, Add the default category if the tag has no category.
        if (category != null) tag.setCategory(category.getId());
        if (category == null && defaultCategory != null) tag.setCategory(defaultCategory.getId());

        Object icon = section.get(key + ".icon");
        if (icon != null) {
            tag.setIcon(TagsUtils.getMultiDeserializedItem(section, key));
        }

        this.cachedTags.put(key.toLowerCase(), tag);
    }

    /**
     * Save & Cache a tag into the tags file.
     *
     * @param tag The tag being saved.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void saveTag(Tag tag) {
        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).saveTagData(tag);
            return;
        }

        try {
            File file = tag.getFile() != null ? tag.getFile() : new File(TAG_FOLDER, "default.yml");
            if (!file.exists()) {
                file.createNewFile();
                tag.setFile(file);
            }

            CommentedFileConfiguration config = CommentedFileConfiguration.loadConfiguration(file);
            config.set("tags." + tag.getId() + ".name", tag.getName());
            config.set("tags." + tag.getId() + ".tag", tag.getTag());
            config.set("tags." + tag.getId() + ".description", tag.getDescription());
            config.set("tags." + tag.getId() + ".permission", tag.getPermission());
            config.set("tags." + tag.getId() + ".order", tag.getOrder());
            config.set("tags." + tag.getId() + ".category", tag.getCategory());

            if (tag.getIcon() != null) {
                config.set("tags." + tag.getId() + ".icon", TagsUtils.serializeItem(tag.getIcon()));
            }

            config.save(file);
            this.cachedTags.put(tag.getId(), tag);
        } catch (IOException ex) {
            this.rosePlugin.getLogger().severe("An error occurred while saving the tag " + tag.getId() + ".");
            ex.printStackTrace();
        }
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

    public void deleteTag(String id) {
        Tag tag = this.cachedTags.get(id);
        if (tag == null) return;

        this.cachedTags.remove(id);
        this.rosePlugin.getManager(DataManager.class).clearTagForAll(id);

        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).deleteTagData(tag);
            return;
        }

        File file = tag.getFile();
        if (file == null) return;

        CommentedFileConfiguration config = CommentedFileConfiguration.loadConfiguration(file);
        config.set("tags." + id, null);
    }

    /**
     * Save a collection of tags at once.
     *
     * @param tags The tags being saved.
     */
    public void saveTags(Map<String, Tag> tags) {
        this.cachedTags.putAll(tags);

        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).saveTagData(tags);
            return;
        }

        CompletableFuture.runAsync(() -> {
            for (Map.Entry<String, Tag> entry : tags.entrySet()) {
                String id = entry.getKey();
                Tag tag = entry.getValue();
                File file = new File(TAG_FOLDER, id + ".yml");
                CommentedFileConfiguration config = CommentedFileConfiguration.loadConfiguration(file);

                config.set("tags." + id + ".name", tag.getName());
                config.set("tags." + id + ".tag", tag.getTag());
                config.set("tags." + id + ".description", tag.getDescription());
                config.set("tags." + id + ".permission", tag.getPermission());
                config.set("tags." + id + ".order", tag.getOrder());
                config.set("tags." + id + ".category", tag.getCategory());

                if (tag.getIcon() != null) {
                    config.set("tags." + id + ".icon", TagsUtils.serializeItem(tag.getIcon()));
                }

                config.save(file);
            }
        });

    }


    /**
     * Get a tag by the UUID, load the user if they aren't cached.
     *
     * @param uuid The UUID of the player.
     *
     * @return The active tag if present
     *
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
     *
     * @return The active tag if present
     */
    @Nullable
    public Tag getUserTag(UUID uuid) {
        TagUser user = this.rosePlugin.getManager(DataManager.class).getCachedUser(uuid);

        return this.getTagFromId(user.getActiveTag());
    }

    /**
     * Get a tag by the player, this method is intended to be used for checking
     * if a player needs to have their tag updated. (Remove Inactive Tags or Default Tags)
     *
     * @param player The player object.
     *
     * @return The active tag if present
     */
    @Nullable
    public Tag getUserTag(Player player) {
        DataManager data = this.rosePlugin.getManager(DataManager.class);
        TagUser user = data.getCachedUsers().getOrDefault(player.getUniqueId(), new TagUser(player.getUniqueId()));
        Tag tag = this.getTagFromId(user.getActiveTag());

        // Update the default tag for the user if they don't have one equipped
        if (tag == null && this.isDefaultTagEnabled) {
            // Check if the player is using a default tag.
            tag = this.getDefaultTag(player);

            if (tag != null) {
                user.setActiveTag(tag.getId());
                user.setUsingDefaultTag(true);
                data.getCachedUsers().put(player.getUniqueId(), user);
            }
        }

        // Remove the tag if the player doesn't have permission to use it.
        if (Setting.REMOVE_TAGS.getBoolean() && tag != null && !this.canUseTag(player, tag) && !user.isUsingDefaultTag()) {
            data.removeUser(player.getUniqueId());
            user.setActiveTag(null);
            user.setUsingDefaultTag(false);
            return null;
        }

        return tag;
    }

    /**
     * Get a tag by the offline player object, If the user isn't cached, return null.
     *
     * @param player The offline player object.
     *
     * @return The active tag if present
     *
     * @since 1.1.6
     */
    @Nullable
    public Tag getOfflineUserTag(OfflinePlayer player) {
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
     *
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
     *
     * @return The tags the player has.
     */
    @NotNull
    public List<Tag> getPlayerTags(Player player) {
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
     *
     * @return true if the tag exists.
     */
    public boolean checkTagExists(String id) {
        return this.cachedTags.get(id.toLowerCase().replace(".", "_")) != null;
    }

    /**
     * Match a tag based on the id.
     *
     * @param id The id of the tag.
     *
     * @return An optional tag.
     */
    @Nullable
    public Tag getTagFromId(String id) {
        if (id == null)
            return null;

        return this.cachedTags.get(id.toLowerCase());
    }

    @Nullable
    public Tag getDefaultTag(Player player) {
        if (!this.isDefaultTagEnabled) return null;

        String defaultTagID = Setting.DEFAULT_TAG.getString();

        // Check if the default tag is a group.
        if (VaultHook.isEnabled() && !this.defaultTagGroups.isEmpty()) {
            String group = VaultHook.getPrimaryGroup(player); // Get the highest group of the player.

            if (group != null && this.defaultTagGroups.containsKey(group.toLowerCase())) {
                String tagId = this.defaultTagGroups.get(group.toLowerCase()); // Get the tag id from the group.
                return switch (tagId) {
                    case "none" -> this.getTagFromId(defaultTagID);
                    case "random" -> this.getRandomTag(player);
                    default -> this.getTagFromId(tagId);
                };
            }
        }

        return switch (defaultTagID) {
            case "none" -> null;
            case "random" -> this.getRandomTag(player);
            default -> this.getTagFromId(defaultTagID);
        };
    }

    /**
     * @return Check if the plugin is using group defaults.
     */
    public boolean usingGroupDefaults() {
        if (this.defaultTagGroups.isEmpty()) // No groups are set.
            return false;

        // Check if all the groups are set to none.
        return !this.defaultTagGroups.values().stream().allMatch(tagId -> tagId.equalsIgnoreCase("none"));
    }

    /**
     * Check if a tag is favourite by ap layer
     *
     * @param player The player
     * @param tag    The tag
     *
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
     *
     * @return The random tag.
     */
    public Tag getRandomTag(OfflinePlayer offlinePlayer) {
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
     *
     * @return The display tag.
     */
    public String getDisplayTag(Tag tag, OfflinePlayer player, String placeholder) {
        StringPlaceholders placeholders = StringPlaceholders.empty();
        if (tag != null) placeholders = this.getTagPlaceholders(tag);

        return TagsUtils.colorAsString(PlaceholderAPI.setPlaceholders(player, tag == null
                ? placeholder
                : placeholders.apply(
                Setting.TAG_PREFIX.getString() + tag.getTag() + Setting.TAG_SUFFIX.getString())
        ));
    }

    /**
     * Get the display version of a tag using placeholderapi
     *
     * @param tag    The tag.
     * @param player The player.
     *
     * @return The display tag.
     */
    public String getDisplayTag(Tag tag, OfflinePlayer player) {
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
     * Get the tags in a category
     *
     * @param category The category
     *
     * @return The tags in the category
     */
    public List<Tag> getTagsInCategory(Category category) {
        CategoryManager categoryManager = this.rosePlugin.getManager(CategoryManager.class);

        if (category == categoryManager.getGlobalCategory()) {
            return new ArrayList<>(this.cachedTags.values());
        }

        if (category.getType() == CategoryType.GLOBAL) {
            return new ArrayList<>(this.cachedTags.values());
        }

        if (!categoryManager.isEnabled()) {
            return new ArrayList<>(this.cachedTags.values());
        }

        return this.cachedTags.values().stream()
                .filter(tag -> tag.getCategory() != null && tag.getCategory().equalsIgnoreCase(category.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Get all the tags in a category
     *
     * @param category The category
     *
     * @return The tags in the category
     */
    public List<Tag> getCategoryTags(Category category) {
        return this.getCategoryTags(category, null);
    }

    /**
     * Get the tags in a category that a player has access to
     *
     * @param category The category
     * @param player   The player
     *
     * @return The tags in the category that the player has access to
     */
    public List<Tag> getCategoryTags(Category category, Player player) {
        List<Tag> result = new ArrayList<>(this.cachedTags.values());

        // Remove tags that the player doesn't have access to
        if (player != null)
            result.removeIf(tag -> !this.canUseTag(player, tag));

        // Don't filter if the category is global or categories are disabled
        if (!this.rosePlugin.getManager(CategoryManager.class).isEnabled()) return result;

        // If the category is global, return all tags
        if (category.getType() == CategoryType.GLOBAL) return result;

        result.removeIf(tag -> tag.getCategory() == null || !tag.getCategory().equalsIgnoreCase(category.getId()));
        return result;
    }

    /**
     * Check if a player has access to a tag
     *
     * @param player The player
     * @param tag    The tag
     *
     * @return If the player has access to the tag
     */
    public boolean canUseTag(Player player, Tag tag) {
        CategoryManager manager = this.rosePlugin.getManager(CategoryManager.class);

        boolean defaultResult = tag.getPermission() == null || player.hasPermission(tag.getPermission());

        // If the tag has no category, then we can just return the tag unlocked status
        if (tag.getCategory() == null) {
            return defaultResult;
        }

        Category category = manager.getCategory(tag.getCategory());
        if (category == null) return defaultResult;
        if (category.getPermission() == null) return defaultResult;

        return player.hasPermission(category.getPermission()) || defaultResult;
    }

    /**
     * Get the tag placeholders for the given player
     *
     * @param tag The tag
     *
     * @return The tag placeholders
     */
    private StringPlaceholders getTagPlaceholders(Tag tag) {
        return StringPlaceholders.builder()
                .add("id", tag.getId())
                .add("name", tag.getName())
                .add("description", String.join(Setting.DESCRIPTION_DELIMITER.getString(), tag.getDescription()))
                .add("permission", tag.getPermission())
                .add("order", tag.getOrder())
                .build();
    }

    public Map<String, Tag> getCachedTags() {
        return cachedTags;
    }

}
