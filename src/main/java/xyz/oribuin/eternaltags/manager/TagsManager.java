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
import xyz.oribuin.eternaltags.listener.BungeeListener;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.obj.Category;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TagsManager extends Manager {

    private final Map<String, Tag> cachedTags = new HashMap<>();
    private final Map<String, Category> cachedCategories = new HashMap<>();
    private final Random random = new Random();

    private CommentedFileConfiguration tagConfig;
    private CommentedFileConfiguration categoryConfig;

    public TagsManager(RosePlugin plugin) {
        super(plugin);
    }

    @Override
    public void reload() {
        // Load all tags from mysql instead of tags.yml
        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).loadTagData(this.cachedTags);
            // TODO Load categories from mysql
//            this.rosePlugin.getManager(DataManager.class).loadCategoryData(this.cachedCategories);
            return;
        }

        // Load the tags.yml file and the categories.yml file.
        this.createDefaultFile("tags.yml", this.getDefaultTags(), config -> this.tagConfig = config);
        this.createDefaultFile("categories.yml", this.getDefaultCategories(), config -> this.categoryConfig = config);

        // Load plugin tags.
        this.loadTags();
        this.loadCategories();

    }

    @Override
    public void disable() {
        // Unused
    }

    /**
     * Create the default files for the plugin and assign them to the variables.
     *
     * @param name          The name of the file.
     * @param defaultValues The default values for the file.
     * @param consumer      The consumer to accept the file.
     */
    private void createDefaultFile(String name, Map<String, Object> defaultValues, Consumer<CommentedFileConfiguration> consumer) {
        final File file = new File(this.rosePlugin.getDataFolder(), name);
        boolean newFile = false;
        try {
            if (!file.exists()) {
                file.createNewFile();
                newFile = true;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        CommentedFileConfiguration config = CommentedFileConfiguration.loadConfiguration(file);
        if (newFile) {
            defaultValues.forEach((path, object) -> {
                if (path.startsWith("#"))
                    config.addPathedComments(path, object.toString());
                else
                    config.set(path, object);
            });

            config.save();
        }

        consumer.accept(config);
    }

    /**
     * Load all the tags from the plugin config.
     */
    public void loadTags() {
        this.cachedTags.clear();

        CommentedConfigurationSection tagSection = this.tagConfig.getConfigurationSection("tags");
        if (tagSection == null) {
            this.rosePlugin.getLogger().severe("Couldn't find tags configuration section.");
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

            String permission = tagSection.getString(key + ".permission", null);
            if (permission != null)
                obj.setPermission(permission);

            int order = tagSection.getInt(key + ".order", -1);
            if (order != -1)
                obj.setOrder(order);

            // TODO: Add support for ItemStacks.
            Material icon = Material.matchMaterial(tagSection.getString(key + ".icon", ""));
            if (icon != null)
                obj.setIcon(icon);

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
            this.rosePlugin.getLogger().severe("Couldn't find categories configuration section.");
            return;
        }

        categorySection.getKeys(false).forEach(key -> {
            String displayName = categorySection.getString(key + ".display-name", key);
            List<String> tags = categorySection.getStringList(key + ".tags"); // List of tags in the category.
            ItemStack icon = TagsUtils.getItemStack(categorySection, key + ".icon");
            int order = categorySection.getInt(key + ".order", -1);

            Category obj = new Category(key.toLowerCase());
            obj.setDisplayName(displayName);
            obj.setTags(tags);
            obj.setIcon(icon);
            obj.setOrder(order);


            this.cachedCategories.put(key.toLowerCase(), obj);
        });
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

    public boolean saveToConfig(Tag tag) {
        if (this.tagConfig == null)
            return false;

        this.tagConfig.set("tags." + tag.getId() + ".name", tag.getName());
        this.tagConfig.set("tags." + tag.getId() + ".tag", tag.getTag());
        this.tagConfig.set("tags." + tag.getId() + ".description", tag.getDescription());
        this.tagConfig.set("tags." + tag.getId() + ".permission", tag.getPermission());
        this.tagConfig.set("tags." + tag.getId() + ".order", tag.getOrder());

        if (tag.getIcon() != null)
            this.tagConfig.set("tags." + tag.getId() + ".icon", tag.getIcon().name());


        this.tagConfig.save();
        return true;
    }

    /**
     * Update every player's with a specific tag with a new one
     *
     * @param tag The tag
     */
    public void updateActiveTag(Tag tag) {
        final DataManager data = this.rosePlugin.getManager(DataManager.class);

        for (Map.Entry<UUID, Tag> entry : data.getCachedUsers().entrySet()) {
            if (entry.getValue().getId().equalsIgnoreCase(tag.getId()))
                data.getCachedUsers().put(entry.getKey(), tag);
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
        })).thenRun(() -> this.tagConfig.save());
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
        this.tagConfig.save();
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

        CompletableFuture.runAsync(() -> this.cachedTags.forEach((id, tag)
                -> this.tagConfig.set("tags." + id, null))).thenRun(()
                -> this.tagConfig.save());

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
        return this.rosePlugin.getManager(DataManager.class).getCachedUsers().get(uuid);
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
        final DataManager dataManager = this.rosePlugin.getManager(DataManager.class);
        Tag tag = dataManager.getCachedUsers().get(player.getUniqueId());

        if (tag == null) {
            tag = this.getDefaultTag(player);
            dataManager.getCachedUsers().put(player.getUniqueId(), tag); // Assign the default tag to the user.
            return tag; // We don't need to check for a permission here, as the default tag should always be available.
        }

        if (Setting.REMOVE_TAGS.getBoolean() && !player.hasPermission(tag.getPermission())) {
            this.rosePlugin.getManager(DataManager.class).removeUser(player.getUniqueId()); // Remove the user's tag.
            return null;
        }

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
        Set<Tag> tags = this.rosePlugin.getManager(DataManager.class).getCachedFavourites().get(uuid);

        if (tags == null || tags.isEmpty())
            return favourites;

        tags.stream()
                .filter(Objects::nonNull)
                .forEach(tag -> favourites.put(tag.getId().toLowerCase(), tag));
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
        if (player == null)
            return new ArrayList<>(this.cachedTags.values());

        return this.cachedTags.values().stream()
                .filter(entry -> player.hasPermission(entry.getPermission()))
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
    public Tag getTagFromId(String id) {
        return this.cachedTags.get(id.toLowerCase());
    }

    /**
     * Get the default tag for a player.
     *
     * @param player The player
     * @return The default tag.
     */
    @Nullable
    public Tag getDefaultTag(@Nullable OfflinePlayer player) {
        String defaultTagID = Setting.DEFAULT_TAG.getString();

        if (defaultTagID == null || defaultTagID.equalsIgnoreCase("none"))
            return null;

        if (defaultTagID.equalsIgnoreCase("random")) {
            return this.getRandomTag(player);
        }

        return this.getTagFromId(defaultTagID);
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

    /**
     * Get the default tags for the plugin
     *
     * @return A map of all the original tags.
     */
    @NotNull
    public Map<String, Object> getDefaultTags() {
        return new LinkedHashMap<>() {{

            // First Tag
            this.put("#0", "Configure the plugin tags here.");
            this.put("tags.eternaltags.name", "EternalTags");
            this.put("tags.eternaltags.tag", "&7[#99ff99&lEternalTags&7]");
            this.put("tags.eternaltags.description", Collections.singletonList("The default EternalTags Tag."));
            this.put("tags.eternaltags.permission", "eternaltags.tag.eternaltags");
            this.put("tags.eternaltags.order", 1);
            this.put("tags.eternaltags.icon", "NAME_TAG");

            // Gradient Tag
            this.put("tags.gradient.name", "Gradient");
            this.put("tags.gradient.tag", "&7[<g:#ED213A:#93291E>Gradient&7]");
            this.put("tags.gradient.description", Collections.singletonList("An automagically formatted gradient."));
            this.put("tags.gradient.permission", "eternaltags.tag.gradient");

            // Rainbow Tag.
            this.put("tags.rainbow.name", "Rainbow");
            this.put("tags.rainbow.tag", "&7[<r:0.7>EternalTags&7]");
            this.put("tags.rainbow.description", Collections.singletonList("An automagically formatted rainbow."));
            this.put("tags.rainbow.permission", "eternaltags.tag.rainbow");

            // Animated Rainbow Tag
            this.put("tags.automatic-rainbow.name", "Animated Rainbow");
            this.put("tags.automatic-rainbow.tag", "&7[<r#15:0.7>Rainbow&7]");
            this.put("tags.automatic-rainbow.description", Arrays.asList("An rainbow tag that", "will update with every", "message that you send."));
            this.put("tags.automatic-rainbow.permission", "eternaltags.tag.animated-rainbow");

            // Animated Gradient Tag
            this.put("tags.automatic-gradient.name", "Animated Gradient");
            this.put("tags.automatic-gradient.tag", "&7[<g#10:#12c2e9:#0c6275>Gradient&7]");
            this.put("tags.automatic-gradient.description", Arrays.asList("A gradient tag that", "will update with every", "message that you send."));
            this.put("tags.automatic-gradient.permission", "eternaltags.tag.animated-gradient");
        }};
    }

    public Map<String, Object> getDefaultCategories() {
        return new LinkedHashMap<>() {{
            this.put("categories.default.name", "Default");
            this.put("categories.default.description", Collections.singletonList("The default EternalTags category."));
            this.put("categories.default.permission", "eternaltags.category.default");
            this.put("categories.default.order", 1);
            this.put("categories.default.icon", "NAME_TAG");
        }};

    }

    public Map<String, Tag> getCachedTags() {
        return cachedTags;
    }

}
