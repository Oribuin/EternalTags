package dev.oribuin.eternaltags.obj;

import dev.oribuin.eternaltags.EternalTags;
import dev.oribuin.eternaltags.config.Setting;
import dev.oribuin.eternaltags.manager.DataManager;
import dev.rosewood.rosegarden.config.BaseSettingSerializer;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.SettingSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Tag {

    private @NotNull String id; // The id of the tag
    private @NotNull String name; // The name of the tag
    private @NotNull String content; // The tag to be added to the player
    private @Nullable String permission;   // The permission required to use the tag
    private @NotNull List<String> description; // The description of the tag
    private Integer order; // The order of the tag

    /**
     * Create a new tag from the plugin config file
     *
     * @param id          The id of the tag
     * @param name        The display name of the tag
     * @param content     The content to display inside the tag
     * @param description The description for the tag
     * @param permission  The permission required to use the tag
     * @param order       The order of the tag in the gui
     */
    public Tag(@NotNull String id, @NotNull String name, @NotNull String content, @NotNull List<String> description, @Nullable String permission, @Nullable Integer order) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.description = description;
        this.permission = permission;
        this.order = order;
    }

    /**
     * Create a new tag from the plugin config file
     *
     * @param id      The id of the tag
     * @param name    The display name of the tag
     * @param content The content to display inside the tag
     */
    public Tag(@NotNull String id, @NotNull String name, @NotNull String content) {
        this(id, name, content, new ArrayList<>(), "eternaltags.tag." + id, -1);
    }

    /**
     * Create a new setting serializer for the tag to be used in configs
     */
    public final static SettingSerializer<Tag> SERIALIZER = new BaseSettingSerializer<>(Tag.class) {
        @Override
        public void write(ConfigurationSection config, String key, Tag value, String... comments) {
            config.set(key + ".name", value.getName());
            config.set(key + ".content", value.getContent());
            config.set(key + ".description", value.getDescription());
            config.set(key + ".permission", value.getPermission());
            config.set(key + ".order", value.getOrder());
        }

        @Override
        public Tag read(ConfigurationSection config, String key) {
            String name = config.getString(key + ".name");
            String content = config.getString(key + ".content");
            List<String> description = config.getStringList(key + ".description");
            String permission = config.getString(key + ".permission");
            int order = config.getInt(key + ".order", -1);
            if (name == null || content == null) return null;

            Tag tag = new Tag(key.toLowerCase(), name, content);
            tag.setDescription(description);
            tag.setPermission(permission);
            tag.setOrder(order);
            return tag;
        }
    };

    /**
     * Load a tag from a configuration section in the config file.
     *
     * @param base The base configuration section, Usually the 'tags' section
     * @param key  The id of the tag to load
     * @return The loaded tag
     */
    public static Tag fromConfig(CommentedConfigurationSection base, String key) {
        Tag tag = SERIALIZER.read(base, key);
        if (tag == null) return null;

        tag.setId(key.toLowerCase());
        return tag;
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

        return player.hasPermission(this.permission);
    }

    public @NotNull String getId() {
        return id;
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    public @NotNull String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    public @NotNull String getContent() {
        return content;
    }

    public void setContent(@NotNull String content) {
        this.content = content;
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

}
