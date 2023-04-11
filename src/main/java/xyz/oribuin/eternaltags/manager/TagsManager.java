package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.manager.Manager;
import dev.rosewood.rosegarden.utils.HexUtils;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.event.TagDeleteEvent;
import xyz.oribuin.eternaltags.event.TagSaveEvent;
import xyz.oribuin.eternaltags.hook.OraxenHook;
import xyz.oribuin.eternaltags.hook.VaultHook;
import xyz.oribuin.eternaltags.listener.BungeeListener;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.obj.Category;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.obj.TagUser;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TagsManager extends Manager {

    // this is starting to look like im an iridium dev
    // Cached Tags and Categories
    private final Map<String, Tag> cachedTags = new HashMap<>();
    private final Map<String, Category> cachedCategories = new HashMap<>();
    private final Random random = new Random();

    // Configuration Files for tags.yml and categories.yml
    private File tagsFile, categoriesFile;
    private CommentedFileConfiguration tagConfig, categoryConfig;

    // Other Values.
    private boolean categoriesEnabled = true;
    private Category defaultCategory, globalCategory;
    private Map<String, String> defaultTagGroups;
    private boolean usingDefaultTags;

    public TagsManager(RosePlugin plugin) {
        super(plugin);
    }

    @Override
    public void reload() {
        // Load the default tag groups
        this.defaultTagGroups = new HashMap<>();
        CommentedConfigurationSection groupSection = Setting.DEFAULT_TAG_GROUPS.getSection();
        groupSection.getKeys(false).forEach(s -> this.defaultTagGroups.put(s.toLowerCase(), groupSection.getString(s)));

        // Check if we're using default tags
        this.usingDefaultTags = this.usingGroupDefaults() || !Setting.DEFAULT_TAG.getString().equalsIgnoreCase("none");

        // Load categories if enabled, Categories are not saved in mysql so we're not gonna load categories first.
        this.categoriesFile = TagsUtils.createFile(this.rosePlugin, "categories.yml");
        this.categoryConfig = CommentedFileConfiguration.loadConfiguration(this.categoriesFile);
        this.loadCategories();

        // Load all tags from mysql instead of tags.yml
        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            DataManager dataManager = this.rosePlugin.getManager(DataManager.class);
            dataManager.loadTagData(this.cachedTags);
            return;
        }

        // Create the tags.yml
        this.tagsFile = TagsUtils.createFile(this.rosePlugin, "tags.yml");
        this.tagConfig = CommentedFileConfiguration.loadConfiguration(this.tagsFile);
        this.loadTags();
    }

    @Override
    public void disable() {
        // Unused
    }

    /**
     * Load all the tags from the plugin config.
     */
    public void loadTags() {
        this.cachedTags.clear();

        CommentedConfigurationSection tagSection = this.tagConfig.getConfigurationSection("tags");
        if (tagSection == null) {
            this.rosePlugin.getLogger().severe("WARNING: We could not find any tags in the tags.yml file. Please make sure you have at least one tag saved.");
            return;
        }

        tagSection.getKeys(false).forEach(key -> {
            String name = tagSection.getString(key + ".name", key);
            String tag = tagSection.getString(key + ".tag");

            if (name == null || tag == null)
                return;

            final Tag obj = new Tag(key.toLowerCase(), name, tag);
            List<String> description = tagSection.get(key + ".description") instanceof String
                    ? Collections.singletonList(tagSection.getString(key + ".description"))
                    : tagSection.getStringList(key + ".description");

            obj.setDescription(description);
            obj.setPermission(tagSection.getString(key + ".permission", null));
            obj.setOrder(tagSection.getInt(key + ".order", -1));
            obj.setHandIcon(tagSection.getBoolean(key + ".hand-icon", false));

            String category = tagSection.getString(key + ".category", null);
            obj.setCategory(category == null ? this.defaultCategory.getId() : category.toLowerCase());


            // Icons can either be a material or a byte array
            Object icon = tagSection.get(key + ".icon");
            if (icon != null) {
                // Read the material from the string
                if (icon instanceof String iconString) {
                    Material material = Material.matchMaterial(iconString);
                    if (material != null)
                        obj.setIcon(new ItemStack(material));
                }

                // Read from a configuration section
                CommentedConfigurationSection iconSection = tagSection.getConfigurationSection(key + ".icon");
                if (iconSection != null && iconSection.getKeys(false).size() > 0) {
                    ItemStack itemStack = TagsUtils.getItemStack(tagSection, key + ".icon");
                    if (itemStack != null)
                        obj.setIcon(itemStack);
                }

                // Read from a byte array
                if (icon instanceof byte[] iconBytes && obj.isHandIcon()) {
                    ItemStack itemStack = TagsUtils.deserializeItem(iconBytes);
                    if (itemStack != null)
                        obj.setIcon(itemStack);
                }
            }

            if (OraxenHook.enabled())
                obj.setTag(OraxenHook.parseGlyph(tag));

            this.cachedTags.put(key.toLowerCase(), obj);
        });
    }

    /**
     * Load all the categories from the plugin config.
     */
    public void loadCategories() {
        this.cachedCategories.clear();

        CommentedConfigurationSection categorySection = this.categoryConfig.getConfigurationSection("categories");
        if (categorySection == null) {
            this.rosePlugin.getLogger().info("No categories found in the categories.yml file, Categories will be disabled.");
            this.categoriesEnabled = false;
            return;
        }

        categorySection.getKeys(false).forEach(key -> {
            String displayName = categorySection.getString(key + ".display-name", key);
            int order = categorySection.getInt(key + ".order", -1);
            boolean isDefault = categorySection.getBoolean(key + ".default", false);
            boolean isGlobal = categorySection.getBoolean(key + ".global", false);
            String permission = categorySection.getString(key + ".permission", null);
            boolean bypass = categorySection.getBoolean(key + ".unlocks-all-tags", false);

            Category obj = new Category(key.toLowerCase());
            obj.setDisplayName(displayName);
            obj.setOrder(order);
            obj.setDefault(isDefault);
            obj.setGlobal(isGlobal);
            obj.setPermission(permission);
            obj.setBypassPermission(bypass);

            this.cachedCategories.put(key.toLowerCase(), obj);
        });

        // Check if the default category exists
        this.defaultCategory = this.cachedCategories.values().stream()
                .filter(Category::isDefault)
                .findFirst()
                .orElse(null);

        // Check if the global category exists
        this.globalCategory = this.cachedCategories.values().stream()
                .filter(Category::isGlobal)
                .findFirst()
                .orElse(null);

        this.categoriesEnabled = this.cachedCategories.size() > 0;
    }

    /**
     * Save & Cache a tag into the tags file.
     *
     * @param tag The tag being saved.
     */
    public boolean saveTag(Tag tag) {

        final TagSaveEvent event = new TagSaveEvent(tag);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return false;

        if (tag.getCategory() == null && this.defaultCategory != null)
            tag.setCategory(this.defaultCategory.getId());

        this.cachedTags.put(tag.getId(), tag);

        // Send the tag to bungee if enabled.
        if (Setting.PLUGIN_MESSAGING.getBoolean()) {
            BungeeListener.modifyTag(tag);
        }

        // Save to mysql instead of tags.yml
        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).saveTagData(tag);
            return true;
        }

        return this.saveToConfig(tag);
    }

    /**
     * Save a tag to the tags.yml file.
     *
     * @param tag The tag being saved.
     */
    public boolean saveToConfig(Tag tag) {
        if (this.tagConfig == null)
            return false;

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
        return true;
    }

    /**
     * Save & Cache a category into the categories file.
     *
     * @param cat The category being saved.
     */
    public void saveCategory(Category cat) {
        if (this.categoryConfig == null)
            return;

        this.cachedCategories.put(cat.getId(), cat);
        this.categoryConfig.set("categories." + cat.getId() + ".display-name", cat.getDisplayName());
        this.categoryConfig.set("categories." + cat.getId() + ".order", cat.getOrder());
        this.categoryConfig.set("categories." + cat.getId() + ".default", cat.isDefault());
        this.categoryConfig.set("categories." + cat.getId() + ".global", cat.isGlobal());
        this.categoryConfig.set("categories." + cat.getId() + ".permission", cat.getPermission());
        this.categoryConfig.set("categories." + cat.getId() + ".unlocks-all-tags", cat.isBypassPermission());
        this.categoryConfig.save(this.categoriesFile);
    }

    /**
     * Update every player's with a specific tag with a new one
     *
     * @param tag The tag
     */
    public void updateActiveTag(Tag tag) {
        final DataManager data = this.rosePlugin.getManager(DataManager.class);

        for (TagUser user : data.getCachedUsers().values()) {
            if (user == null)
                continue;

            if (user.getActiveTag() != null && user.getActiveTag().equalsIgnoreCase(tag.getId())) {
                user.setActiveTag(tag.getId());
            }
        }
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

        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).saveTagData(tags);
            return;
        }

        // Send the tags to bungee if enabled.
        if (Setting.PLUGIN_MESSAGING.getBoolean()) {
            tags.values().forEach(BungeeListener::modifyTag);
        }

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
        if (tag == null)
            return;

        final TagDeleteEvent event = new TagDeleteEvent(tag);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        if (Setting.PLUGIN_MESSAGING.getBoolean()) {
            BungeeListener.deleteTag(id);
        }

        // remove anyone with the tag active.
        this.rosePlugin.getManager(DataManager.class).deleteUserTag(id);
        this.cachedTags.remove(id);

        // Save to mysql instead of tags.yml
        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).deleteTagData(tag);
            return;
        }

        this.tagConfig.set("tags." + id, null);
        this.tagConfig.save(this.tagsFile);
    }

    public void clearTagFromUsers(String id) {
        this.rosePlugin.getManager(DataManager.class).deleteUserTag(id);
    }

    /**
     * Wipes all the tags from the tags.yml
     */
    public void wipeTags() {
        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).deleteAllTagData();
            return;
        }

        CompletableFuture.runAsync(() -> this.cachedTags.forEach((id, tag) -> this.tagConfig.set("tags." + id, null)))
                .thenRun(() -> this.tagConfig.save(this.tagsFile));

        this.cachedTags.clear();
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
     * @since 1.1.6
     */
    @Nullable
    public Tag getUserTag(@NotNull UUID uuid) {
        TagUser user = this.rosePlugin.getManager(DataManager.class).getCachedUsers().get(uuid);
        if (user == null)
            return null;

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
    public Tag getUserTag(@NotNull Player player) {
        DataManager dataManager = this.rosePlugin.getManager(DataManager.class);
        TagUser user = dataManager.getCachedUsers().computeIfAbsent(player.getUniqueId(), k -> new TagUser(player));
        Tag tag = this.getTagFromId(user.getActiveTag());

        // Check if the player is using a default tag.
        if (user.isUsingDefaultTag() && this.usingDefaultTags) {
            tag = this.getDefaultTag(player);
        }

        // Remove the tag if the player doesn't have permission to use it.
        if (Setting.REMOVE_TAGS.getBoolean() && tag != null && !this.canUseTag(player, tag)) {
            dataManager.removeUser(player.getUniqueId());
            user.setActiveTag(null); // Remove the tag from the user.
            user.setUsingDefaultTag(false); // Remove the default tag flag.
            tag = null; // Set the tag to null.
        }

        // Use default tag if no active tag found.
        if (tag == null && this.usingDefaultTags) {
            tag = this.getDefaultTag(player);
            if (tag == null)
                return null;

            user.setActiveTag(tag.getId());
            user.setUsingDefaultTag(true);
        }

        dataManager.getCachedUsers().put(player.getUniqueId(), user);
        return tag;
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
     * Get an offline player's active tag.
     *
     * @param offlinePlayer The player.
     * @return The active tag if present
     * @deprecated Use {@link TagsManager#getOfflineUserTag(OfflinePlayer)} instead.
     */
    @Nullable
    @Deprecated
    public Tag getPlayersTag(@NotNull OfflinePlayer offlinePlayer) {
        return this.getOfflineUserTag(offlinePlayer);
    }

    /**
     * Change a user's current active tag.
     *
     * @param uuid The UUID of the player.
     * @param tag  The tag of the user.
     */
    public void setTag(@NotNull UUID uuid, @NotNull Tag tag) {
        this.rosePlugin.getManager(DataManager.class).saveUser(uuid, tag);
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
        final Map<String, Tag> favourites = new HashMap<>();
        final TagUser user = this.rosePlugin.getManager(DataManager.class).getCachedUsers().getOrDefault(uuid, new TagUser(uuid));

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
        if (id == null)
            return null;

        return this.cachedTags.get(id.toLowerCase());
    }

    @Nullable
    public Tag getDefaultTag(@NotNull Player player) {
        if (!this.usingDefaultTags) // Default tags are disabled.
            return null;

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
     * Get the default tag for an offline player.
     *
     * @param player The player
     * @return The default tag.
     */
    @Nullable
    public Tag getDefaultTag(@Nullable OfflinePlayer player) {
        String defaultTagID = Setting.DEFAULT_TAG.getString();

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
     * Check if a tag is favourited by ap layer
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
        if (tag == null)
            return placeholder;

        return HexUtils.colorify(PlaceholderAPI.setPlaceholders(player, this.getTagPlaceholders(tag).apply(Setting.TAG_PREFIX.getString() + tag.getTag() + Setting.TAG_SUFFIX.getString())));
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
     * Get the tags in a category
     *
     * @param category The category
     * @return The tags in the category
     */
    public List<Tag> getTagsInCategory(Category category) {
        if (!this.categoriesEnabled || category.isGlobal()) // Categories are disabled or the category is default
            return new ArrayList<>(this.cachedTags.values());

        return this.cachedTags.values().stream()
                .filter(tag -> tag.getCategory() != null && tag.getCategory().equalsIgnoreCase(category.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Get the tags in a category that a player has access to
     *
     * @param category The category
     * @param player   The player
     * @return The tags in the category that the player has access to
     */
    public List<Tag> getAccessibleTagsInCategory(Category category, Player player) {
        if (category.isGlobal()) // Categories are disabled or the category is default
            return this.getPlayerTags(player);

        return this.getPlayerTags(player).stream()
                .filter(tag -> tag.getCategory() != null && tag.getCategory().equalsIgnoreCase(category.getId()))
                .toList();
    }

    /**
     * Check if a player has access to a tag
     *
     * @param player The player
     * @param tag    The tag
     * @return If the player has access to the tag
     */
    public boolean canUseTag(@NotNull Player player, @NotNull Tag tag) {
        boolean hasAccessToTag = tag.getPermission() == null || player.hasPermission(tag.getPermission());

        // If there's no categories, or all categories are default, then we can just return the tag unlocked status
        if (!this.categoriesEnabled) {
            return hasAccessToTag;
        }

        // If the tag has the category, the category bypasses tag perms.
        Category category = this.getCategory(tag);
        if (category != null) {
            // If the category bypasses tag perms, and the player has the category permission, then they can use the tag or if they have the tag permission
            if (category.isBypassPermission() && category.getPermission() != null && player.hasPermission(category.getPermission()))
                return true;
        }

        return hasAccessToTag;
    }

    /**
     * Get all categories from a predicate filter
     *
     * @param predicate The predicate
     * @return The categories
     */
    @NotNull
    public List<Category> getCategories(Predicate<Category> predicate) {
        return this.cachedCategories.values().stream().filter(predicate).toList();
    }

    /**
     * Get the category from a predicate filter
     *
     * @param predicate The predicate
     * @return The category
     */
    @Nullable
    public Category getCategory(Predicate<Category> predicate) {
        return this.cachedCategories.values().stream().filter(predicate).findFirst().orElse(null);
    }

    /**
     * Get the category from an id
     *
     * @param id The id
     * @return The category
     */
    @Nullable
    public Category getCategory(String id) {
        return this.getCategory(category -> category.getId().equals(id));
    }

    /**
     * Get the category from a tag
     *
     * @param tag The tag
     * @return The category
     */
    @Nullable
    public Category getCategory(@NotNull Tag tag) {
        if (tag.getCategory() == null)
            return null;

        // Check if the tag has an associated category
        Category category = this.cachedCategories.get(tag.getCategory());
        if (category != null)
            return category;

        // Get the default category if the tag doesn't have an associated category, we don't use global categories here
        return this.defaultCategory;
    }

    /**
     * Get the tags with categories
     *
     * @return The tags with categories
     */
    public Map<Tag, Category> getTagsWithCategories() {
        Map<Tag, Category> tagsWithCategories = new HashMap<>();
        for (Tag tag : this.cachedTags.values()) {
            Category category = this.getCategory(tag);
            if (category != null)
                tagsWithCategories.put(tag, category);
        }

        return tagsWithCategories;
    }

    /**
     * Does the player have the group tag active?
     *
     * @param player The player
     * @return The tags with categories that the player has access to
     */
    public boolean hasPrimaryGroupTag(@NotNull Player player) {
        if (!VaultHook.isEnabled() || !this.usingGroupDefaults())
            return false;

        String userTag = Optional.ofNullable(this.getUserTag(player.getUniqueId()))
                .map(Tag::getId)
                .orElse(null);

        return userTag != null && userTag.equals(this.getGroupTag(player));
    }

    /**
     * Get the tag for a group
     *
     * @param group The group
     * @return The tag
     */
    @Nullable
    public String getGroupTag(String group) {
        if (!VaultHook.isEnabled() || !this.usingGroupDefaults())
            return null;

        return this.defaultTagGroups.get(group);
    }

    /**
     * Get the tag for a group
     *
     * @param player The player
     * @return The tag
     */
    @Nullable
    public String getGroupTag(@NotNull Player player) {
        if (!VaultHook.isEnabled() || !this.usingGroupDefaults())
            return null;

        String group = VaultHook.getPrimaryGroup(player);
        String tag = this.defaultTagGroups.get(group);
        if (tag == null)
            return null;

        return switch (tag) {
            case "default" -> Setting.DEFAULT_TAG.getString();
            case "random" -> this.getRandomTag(player).getId();
            case "none" -> null;
            default -> this.cachedTags.containsKey(tag) ? tag : null;
        };
    }

    /**
     * Get the tag placeholders for the given player
     *
     * @param tag The tag
     * @return The tag placeholders
     */
    private StringPlaceholders getTagPlaceholders(Tag tag) {
        return StringPlaceholders.builder()
                .addPlaceholder("id", tag.getId())
                .addPlaceholder("name", tag.getName())
                .addPlaceholder("description", String.join(Setting.DESCRIPTION_DELIMITER.getString(), tag.getDescription()))
                .addPlaceholder("permission", tag.getPermission())
                .addPlaceholder("order", tag.getOrder())
                .build();
    }

    public Map<String, Tag> getCachedTags() {
        return cachedTags;
    }

    public Map<String, Category> getCachedCategories() {
        return cachedCategories;
    }

    public boolean isCategoriesEnabled() {
        return categoriesEnabled;
    }

    public Category getDefaultCategory() {
        return defaultCategory;
    }

    public Category getGlobalCategory() {
        return globalCategory;
    }

}
