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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.event.TagDeleteEvent;
import xyz.oribuin.eternaltags.event.TagSaveEvent;
import xyz.oribuin.eternaltags.hook.BungeeListener;
import xyz.oribuin.eternaltags.hook.OraxenHook;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.obj.Tag;

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
import java.util.stream.Collectors;

public class TagsManager extends Manager {

    private final Map<String, Tag> cachedTags = new HashMap<>();
    private final Random random = new Random();

    private CommentedFileConfiguration config;

    public TagsManager(RosePlugin plugin) {
        super(plugin);
    }

    @Override
    public void reload() {
        // Load all tags from mysql instead of tags.yml
        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).loadTagData(this.cachedTags);
            return;
        }

        final File file = new File(this.rosePlugin.getDataFolder(), "tags.yml");
        boolean newFile = false;
        try {
            if (!file.exists()) {
                file.createNewFile();
                newFile = true;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.config = CommentedFileConfiguration.loadConfiguration(file);
        if (newFile) {
            this.getDefaultTags().forEach((path, object) -> {
                if (path.startsWith("#"))
                    this.config.addPathedComments(path, object.toString());
                else
                    this.config.set(path, object);
            });

            this.config.save();
        }

        // Load plugin tags.
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

        CommentedConfigurationSection tagSection = this.config.getConfigurationSection("tags");
        if (tagSection == null) {
            this.rosePlugin.getLogger().severe("Couldn't find tags configuration section.");
            return;
        }

        for (String key : tagSection.getKeys(false)) {
            String name = tagSection.getString(key + ".name");
            String tag = tagSection.getString(key + ".tag");

            if (name == null)
                name = key;

            if (tag == null)
                continue;

            final Tag obj = new Tag(key.toLowerCase(), name, tag);
            List<String> description = tagSection.get(key + ".description") instanceof String
                    ? Collections.singletonList(tagSection.getString(key + ".description"))
                    : tagSection.getStringList(key + ".description");

            obj.setDescription(description);

            if (tagSection.getString(key + ".permission") != null)
                obj.setPermission(tagSection.getString(key + ".permission"));

            if (tagSection.getString(key + ".order") != null)
                obj.setOrder(tagSection.getInt(key + ".order"));

            final String iconName = tagSection.getString(key + ".icon");
            if (iconName != null)
                obj.setIcon(Material.matchMaterial(iconName) != null ? Material.matchMaterial(iconName) : Material.NAME_TAG);

            if (OraxenHook.enabled()) {
                obj.setTag(OraxenHook.parseGlyph(tag));
            }

            this.cachedTags.put(key.toLowerCase(), obj);
        }
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
        BungeeListener.modifyTag(tag);

        // Save to mysql instead of tags.yml
        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).saveTagData(tag);
            return true;
        }

        return this.saveToConfig(tag);
    }

    public boolean saveToConfig(Tag tag) {
        if (this.config == null)
            return false;

        this.config.set("tags." + tag.getId() + ".name", tag.getName());
        this.config.set("tags." + tag.getId() + ".tag", tag.getTag());
        this.config.set("tags." + tag.getId() + ".description", tag.getDescription());
        this.config.set("tags." + tag.getId() + ".permission", tag.getPermission());
        this.config.set("tags." + tag.getId() + ".order", tag.getOrder());

        if (tag.getIcon() != null)
            this.config.set("tags." + tag.getId() + ".icon", tag.getIcon().name());


        this.config.save();
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

        CompletableFuture.runAsync(() -> tags.forEach((id, tag) -> {
            this.config.set("tags." + id + ".name", tag.getName());
            this.config.set("tags." + id + ".tag", tag.getTag());
            this.config.set("tags." + id + ".description", tag.getDescription());
            this.config.set("tags." + id + ".permission", tag.getPermission());
            this.config.set("tags." + id + ".order", tag.getOrder());
        })).thenRun(() -> this.config.save());
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

        BungeeListener.deleteTag(id);

        // remove anyone with the tag active.
        this.rosePlugin.getManager(DataManager.class).deleteUserTag(id);
        this.cachedTags.remove(id);

        // Save to mysql instead of tags.yml
        if (Setting.MYSQL_TAGDATA.getBoolean()) {
            this.rosePlugin.getManager(DataManager.class).deleteTagData(tag);
            return;
        }

        this.config.set("tags." + id, null);
        this.config.save();
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
                -> this.config.set("tags." + id, null))).thenRun(()
                -> this.config.save());

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
    public @Nullable Tag getUserTag(UUID uuid) {
        final var dataManager = this.rosePlugin.getManager(DataManager.class);
        final var player = Bukkit.getPlayer(uuid);
        var tag = dataManager.getCachedUsers().get(uuid);
        if (tag == null)
            dataManager.loadUser(uuid);

        // Check if the plugin wants to remove the tag.
        if (Setting.REMOVE_TAGS.getBoolean() && tag != null) {
            if (player == null)
                return null;

            if (!player.hasPermission(tag.getPermission())) {
                var defaultTag = this.getDefaultTag(player);
                dataManager.saveUser(uuid, defaultTag);
                return defaultTag;
            }
        }

        // If the user is still null, return the default tag.
        var newTag = dataManager.getCachedUsers().get(uuid);
        if (newTag == null && player != null)
            return this.getDefaultTag(player);

        return dataManager.getCachedUsers().get(uuid);
    }

    /**
     * Get a tag by the user object, If the user isn't cached, return null.
     *
     * @param player The player object.
     * @return The active tag if present
     * @
     */
    public @Nullable Tag getUserTag(Player player) {
        return this.getUserTag(player.getUniqueId());
    }


    /**
     * Get a tag by the offline player object, If the user isn't cached, return null.
     *
     * @param player The offline player object.
     * @return The active tag if present
     * @since 1.1.6
     */
    public @Nullable Tag getUserTag(OfflinePlayer player) {
        return this.getUserTag(player.getUniqueId());
    }

    /**
     * Get an offline player's active tag.
     *
     * @param offlinePlayer The player.
     * @return The active tag if present
     * @deprecated Use {@link TagsManager#getUserTag(OfflinePlayer)} instead.
     */
    @Nullable
    @Deprecated
    public Tag getPlayersTag(@NotNull OfflinePlayer offlinePlayer) {
        return this.getUserTag(offlinePlayer);
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
    public Tag getDefaultTag(@Nullable OfflinePlayer player) {
        var defaultTagID = Setting.DEFAULT_TAG.getString();

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
    public @NotNull Map<String, Object> getDefaultTags() {
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

    public Map<String, Tag> getCachedTags() {
        return cachedTags;
    }

}
