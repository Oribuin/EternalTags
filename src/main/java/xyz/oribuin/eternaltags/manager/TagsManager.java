package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.manager.Manager;
import dev.rosewood.rosegarden.utils.HexUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.event.TagDeleteEvent;
import xyz.oribuin.eternaltags.event.TagSaveEvent;
import xyz.oribuin.eternaltags.hook.OraxenHook;
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
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TagsManager extends Manager {

    private final Map<String, Tag> cachedTags = new HashMap<>();
    private final Random random = new Random();

    private boolean removeInaccessible;
    private CommentedFileConfiguration config;

    public TagsManager(RosePlugin plugin) {
        super(plugin);
    }

    @Override
    public void reload() {
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
        this.removeInaccessible = ConfigurationManager.Setting.REMOVE_TAGS.getBoolean();
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

            final Tag obj = new Tag(key, name, tag);
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

            this.cachedTags.put(key, obj);
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
        this.config.set("tags." + tag.getId() + ".name", tag.getName());
        this.config.set("tags." + tag.getId() + ".tag", tag.getTag());
        this.config.set("tags." + tag.getId() + ".description", tag.getDescription());
        this.config.set("tags." + tag.getId() + ".permission", tag.getPermission());
        this.config.set("tags." + tag.getId() + ".order", tag.getOrder());
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
        Optional<Tag> optional = this.matchTagFromID(id);
        if (optional.isEmpty())
            return;

        final TagDeleteEvent event = new TagDeleteEvent(optional.get());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        // remove anyone with the tag active.
        this.rosePlugin.getManager(DataManager.class).deleteTag(id);

        this.cachedTags.remove(id);
        this.config.set("tags." + id, null);
        this.config.save();
    }

    /**
     * Wipes all the tags from the tags.yml
     */
    public void wipeTags() {
        CompletableFuture.runAsync(() -> this.cachedTags.forEach((id, tag)
                -> this.config.set("tags." + id, null))).thenRun(()
                -> this.config.save());

        this.cachedTags.clear();
    }

    /**
     * Get a user's active tag by UUID
     *
     * @param uuid The UUID of the user.
     * @return The active tag if present
     */
    public Optional<Tag> getUsersTag(UUID uuid) {
        Tag tag = this.rosePlugin.getManager(DataManager.class).getCachedUsers().get(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (tag == null)
            return this.getDefaultTag(player);

        if (this.removeInaccessible && player != null && !player.hasPermission(tag.getPermission()))
            return this.getDefaultTag(player);

        return Optional.of(tag);
    }

    /**
     * Change a user's current active tag.
     *
     * @param uuid The UUID of the player.
     * @param tag  The tag of the user.
     */
    public void setTag(UUID uuid, Tag tag) {
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
     * Get a user's active tag by the player object
     *
     * @param player The player
     * @return The active tag if present
     */
    public Optional<Tag> getUsersTag(Player player) {
        // I could return this.getTag(player.getUniqueId()); but its *slightly* more efficient for the removeInaccessible option
        Tag tag = this.rosePlugin.getManager(DataManager.class).getCachedUsers().get(player.getUniqueId());
        if (tag == null)
            return this.getDefaultTag(player);

        if (this.removeInaccessible && !player.hasPermission(tag.getPermission()))
            return this.getDefaultTag(player);

        return Optional.of(tag);
    }

    /**
     * Get a user's favourite tags.
     *
     * @param uuid The UUID of the player.
     * @return The map of favourite tags.
     */
    public Map<String, Tag> getUsersFavourites(UUID uuid) {
        final Map<String, Tag> favourites = new HashMap<>();
        Set<Tag> tags = this.rosePlugin.getManager(DataManager.class).getCachedFavourites().get(uuid);

        if (tags == null || tags.isEmpty())
            return favourites;

        tags.forEach(tag -> favourites.put(tag.getId().toLowerCase(), tag));
        return favourites;
    }

    /**
     * Get all the tags a player has permission to use.
     *
     * @param player The player
     * @return The tags the player has.
     */
    public List<Tag> getPlayersTags(Player player) {
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
    public Optional<Tag> matchTagFromID(String id) {
        return Optional.ofNullable(this.cachedTags.get(id.toLowerCase()));
    }

    /**
     * @param player The player
     * @return The default tag.
     */
    public Optional<Tag> getDefaultTag(Player player) {
        String defaultTagID = ConfigurationManager.Setting.DEFAULT_TAG.getString();

        if (defaultTagID == null || defaultTagID.equalsIgnoreCase("none"))
            return Optional.empty();

        if (defaultTagID.equalsIgnoreCase("random") && player != null) {
            Optional<Tag> randomTag = this.getRandomTag(player);
            randomTag.ifPresent(tag -> this.setTag(player.getUniqueId(), tag));

            return randomTag;
        }

        return this.matchTagFromID(defaultTagID);
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
        this.rosePlugin.getManager(DataManager.class).updateEveryone(tag, new ArrayList<>(Bukkit.getOnlinePlayers()));
    }

    /**
     * Get a randomized tag from a user's available tags.
     *
     * @param player The player
     * @return The random tag.
     */
    public Optional<Tag> getRandomTag(Player player) {
        List<Tag> tags = this.getPlayersTags(player);
        if (tags.isEmpty())
            return Optional.empty();

        return Optional.ofNullable(tags.get(random.nextInt(tags.size())));
    }

    /**
     * Get the display version of a tag using placeholderapi
     *
     * @param tag    The tag.
     * @param player The player.
     * @param placeholder The placeholder.
     * @return The display tag.
     */
    public String getDisplayTag(@Nullable Tag tag, OfflinePlayer player, String placeholder) {
        return HexUtils.colorify(PlaceholderAPI.setPlaceholders(player, tag == null ? placeholder : tag.getTag()));
    }

    /**
     * Get the display version of a tag using placeholderapi
     *
     * @param tag   The tag.
     * @param player The player.
     * @return The display tag.
     */
    public String getDisplayTag(@Nullable Tag tag, OfflinePlayer player) {
        return this.getDisplayTag(tag, player, ""); // Empty placeholder string
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
            this.put("tags.automatic-gradient.tag", "&7[<g#10:#12c2e9:#c471ed>Gradient&7]");
            this.put("tags.automatic-gradient.description", Arrays.asList("A gradient tag that", "will update with every", "message that you send."));
            this.put("tags.automatic-gradient.permission", "eternaltags.tag.animated-gradient");
        }};
    }

    public Map<String, Tag> getCachedTags() {
        return cachedTags;
    }

}
