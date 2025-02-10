package dev.oribuin.eternaltags.obj;

import dev.oribuin.eternaltags.config.Setting;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.oribuin.eternaltags.EternalTags;
import dev.oribuin.eternaltags.manager.DataManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Tag {

    private final @NotNull String id; // The id of the tag
    private @NotNull String name; // The name of the tag
    private @NotNull String content; // The tag to be added to the player
    private @Nullable String permission;   // The permission required to use the tag
    private @NotNull List<String> description; // The description of the tag
    private int order; // The order of the tag
    private File source;

    public Tag(@NotNull String id, @NotNull String name, @NotNull String content) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.description = new ArrayList<>();
        this.permission = "eternaltags.tag." + id.toLowerCase();
        this.order = -1;
        this.source = null;
    }

    /**
     * Load a tag from a configuration section in the config file.
     *
     * @param base The base configuration section, Usually the 'tags' section
     * @param key  The id of the tag to load
     * @return The loaded tag
     */
    public static Tag fromConfig(File source, CommentedConfigurationSection base, String key) {
        CommentedConfigurationSection section = base.getConfigurationSection(key);
        if (section == null) return null;

        // Load the important values first
        String name = section.getString("name");
        String content = section.getString("content");
        if (name == null || content == null) return null;

        // Load the optional values
        String permission = section.getString("permission");
        List<String> description = section.getStringList("description");
        int order = section.getInt("order", -1);

        // Create the tag object
        Tag tag = new Tag(key.toLowerCase(), name, content);
        tag.setSource(source);
        
        if (permission != null) tag.setPermission(permission);
        if (!description.isEmpty()) tag.setDescription(description);
        if (order != -1) tag.setOrder(order);
        return tag;
    }
    
    /**
     * Save the tag to a configuration section in the config file.
     *
     * @param section The section to save the tag to
     */
    public void save(CommentedConfigurationSection section) {
        section.set("name", this.name);
        section.set("content", this.content);
        section.set("permission", this.permission);
        section.set("description", this.description);
        section.set("order", this.order);
    }
    
    /**
     * Save the tag to the source file.
     */
    public void save() {
        if (this.source == null) {
            CompletableFuture.completedFuture(null);
            return;
        }

        CompletableFuture.runAsync(() -> {
            CommentedFileConfiguration config = CommentedFileConfiguration.loadConfiguration(this.source);
            CommentedConfigurationSection section = config.getConfigurationSection("tags." + this.id);
            if (section == null) section = config.createSection("tags." + this.id);

            this.save(section);
            config.save(this.source);
        });
    }
    
    /**
     * Delete the tag from the source file.
     */
    public void delete() {
        if (this.source == null) {
            CompletableFuture.completedFuture(null);
            return;
        }

        CompletableFuture.runAsync(() -> {
            CommentedFileConfiguration config = CommentedFileConfiguration.loadConfiguration(this.source);
            config.set("tags." + this.id, null);
            config.save(this.source);
        });
    }

    /**
     * Equip a tag to a specific player.
     *
     * @param player The player to equip the tag to
     */
    public void equip(Player player) {
        DataManager dataManager = EternalTags.get().getManager(DataManager.class);

        // Remove the tag if the player does not have permission
        if (Setting.REMOVE_INACCESSIBLE.get() && this.permission != null && !player.hasPermission(this.permission)) {
            dataManager.removeUser(player.getUniqueId());
            return;
        }

        // Set the player's tag
        dataManager.saveUser(player.getUniqueId(), this.id.toLowerCase());
    }

    /**
     * Unequip a tag from a specific player.
     *
     * @param player The player to unequip the tag from
     */
    public void unequip(Player player) {
        DataManager dataManager = EternalTags.get().getManager(DataManager.class);
        dataManager.removeUser(player.getUniqueId());
    }

    /**
     * Check if a player has permission to use the tag.
     *
     * @param player The player to check
     * @return Whether the player has permission or not
     */
    public boolean hasPermission(Player player) {
        if (this.permission == null) return true; // No permission required
        if (player.hasPermission("eternaltags.tags.*")) return true; // Bypass all permissions

        return player.hasPermission(this.permission);
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public @NotNull String getTag() {
        return content;
    }

    public void setTag(@NotNull String tag) {
        this.content = tag;
    }

    public @Nullable String getPermission() {
        return permission;
    }

    public void setPermission(@Nullable String permission) {
        this.permission = permission;
    }

    public @NotNull List<String> getDescription() {
        return description;
    }

    public void setDescription(@NotNull List<String> description) {
        this.description = description;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public File getSource() {
        return source;
    }
    
    public void setSource(File source) {
        this.source = source;
    }
    
}
