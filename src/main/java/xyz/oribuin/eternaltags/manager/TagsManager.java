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
import xyz.oribuin.eternaltags.hook.VaultHook;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.obj.Category;
import xyz.oribuin.eternaltags.obj.CategoryType;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.obj.TagUser;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TagsManager extends Manager {

    private final Map<String, Tag> cachedTags = new HashMap<>();
    private final Random random = new Random();

    // Configuration Files for tags.yml and categories.yml
    private File tagsFile;
    private CommentedFileConfiguration tagConfig;

    // Other Values.
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
        // Create the tags.yml
        this.tagsFile = TagsUtils.createFile(this.rosePlugin, "tags.yml");
        this.tagConfig = CommentedFileConfiguration.loadConfiguration(this.tagsFile);
        this.loadTags();

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

    public void loadTags() {
        CommentedConfigurationSection tagSection = this.tagConfig.getConfigurationSection("tags");
        if (tagSection == null) {
            this.rosePlugin.getLogger().severe("WARNING: We could not find any tags in the tags.yml file. Please make sure you have at least one tag saved.");
            return;
        }

        this.cachedTags.clear();
        for (String key : tagSection.getKeys(false)) {
            this.loadTag(tagSection, key);
        }
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
    public void saveTag(Tag tag) {
        Category defaultCategory = this.rosePlugin.getManager(CategoryManager.class)
                .getFirst(CategoryType.DEFAULT);

        if (tag.getCategory() == null && defaultCategory != null)
            tag.setCategory(defaultCategory.getId());

        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).saveTagData(tag);
        } else {
            this.saveToConfig(tag);
        }

        this.cachedTags.put(tag.getId(), tag);
    }

    /**
     * Save a tag to the tags.yml file.
     *
     * @param tag The tag being saved.
     */
    public void saveToConfig(Tag tag) {
        if (this.tagConfig == null) return;

        this.tagConfig.set("tags." + tag.getId() + ".name", tag.getName());
        this.tagConfig.set("tags." + tag.getId() + ".tag", tag.getTag());
        this.tagConfig.set("tags." + tag.getId() + ".description", tag.getDescription());
        this.tagConfig.set("tags." + tag.getId() + ".permission", tag.getPermission());
        this.tagConfig.set("tags." + tag.getId() + ".order", tag.getOrder());
        this.tagConfig.set("tags." + tag.getId() + ".category", tag.getCategory());
        this.tagConfig.set("tags." + tag.getId() + ".hand-icon", tag.isHandIcon());

        if (tag.getIcon() != null && tag.isHandIcon()) // Only save the icon if it's a hand icon to prevent overwriting config defined items.
            this.tagConfig.set("tags." + tag.getId() + ".icon", TagsUtils.serializeItem(tag.getIcon()));

        this.tagConfig.save(this.tagsFile);
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

        // If MySQL Tags is enabled, save the tags to the database instead of the tags.yml
        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).saveTagData(tags);
            return;
        }

        // Save the tags to the tags.yml
        CompletableFuture.runAsync(() -> tags.forEach((id, tag) -> {
            this.tagConfig.set("tags." + id + ".name", tag.getName());
            this.tagConfig.set("tags." + id + ".tag", tag.getTag());
            this.tagConfig.set("tags." + id + ".description", tag.getDescription());
            this.tagConfig.set("tags." + id + ".permission", tag.getPermission());
            this.tagConfig.set("tags." + id + ".order", tag.getOrder());
            this.tagConfig.set("tags." + id + ".hand-icon", tag.isHandIcon());
            this.tagConfig.set("tags." + id + ".icon", TagsUtils.serializeItem(tag.getIcon()));
            this.tagConfig.set("tags." + id + ".category", tag.getCategory());
        })).thenRun(() -> this.tagConfig.save(this.tagsFile));
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

        // Delete the tag from the database if MySQL TagData is enabled.
        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).deleteTagData(tag);
            return;
        }

        // Delete the tag from the tags.yml
        this.tagConfig.set("tags." + id, null);
        this.tagConfig.save(this.tagsFile);
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
    public Tag getUserTag(@NotNull UUID uuid) {
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
        if (player == null) return null;

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
    public Tag getTagFromId(@Nullable String id) {
        if (id == null)
            return null;

        return this.cachedTags.get(id.toLowerCase());
    }

    @Nullable
    public Tag getDefaultTag(@NotNull Player player) {
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
     *
     * @return The display tag.
     */
    public String getDisplayTag(@Nullable Tag tag, OfflinePlayer player, @NotNull String placeholder) {
        if (tag == null) return placeholder; // Return the placeholder if the tag is null

        StringPlaceholders placeholders = this.getTagPlaceholders(tag);
        return TagsUtils.colorAsString(PlaceholderAPI.setPlaceholders(player, placeholders.apply(
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
     * Get the tags in a category
     *
     * @param category The category
     *
     * @return The tags in the category
     */
    public List<Tag> getTagsInCategory(Category category) {

        // Check the default values if the category is global or disabled
        if (category.getType() == CategoryType.GLOBAL) return new ArrayList<>(this.cachedTags.values());
        if (!this.rosePlugin.getManager(CategoryManager.class).isEnabled()) return new ArrayList<>(this.cachedTags.values());

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
    public boolean canUseTag(@NotNull Player player, @NotNull Tag tag) {
        CategoryManager manager = this.rosePlugin.getManager(CategoryManager.class);
        boolean defaultResult = tag.hasPermission(player); // Check if the player has permission to use the tag
        TagUser user = this.rosePlugin.getManager(DataManager.class).getCachedUser(player.getUniqueId());
        if (user.isUsingDefaultTag()) return true; // Players can always use a default tag.

        // If the tag has no category, then we can just return the tag unlocked status
        Category category = manager.getCategory(tag.getCategory());
        if (category == null) return defaultResult;
        if (category.getPermission() == null) return defaultResult;

        // If the category can bypass tag permissions and the category has a permission set
        if (category.isBypassPermission() && category.getPermission() != null)
            return player.hasPermission(category.getPermission()) || defaultResult;

        return defaultResult;
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
