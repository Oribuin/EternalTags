package dev.oribuin.eternaltags.obj;

import dev.oribuin.eternaltags.EternalTags;
import dev.oribuin.eternaltags.config.Setting;
import dev.oribuin.eternaltags.manager.DataManager;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.SettingField;
import dev.rosewood.rosegarden.config.SettingSerializer;
import dev.rosewood.rosegarden.config.SettingSerializers;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static dev.rosewood.rosegarden.config.SettingSerializers.INTEGER;
import static dev.rosewood.rosegarden.config.SettingSerializers.STRING;
import static dev.rosewood.rosegarden.config.SettingSerializers.STRING_LIST;

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
    public final static SettingSerializer<Tag> SERIALIZER = SettingSerializers.ofRecord(Tag.class, instance ->
            instance.group(SettingField.ofOptionalValue("id", STRING, Tag::getId, null),
                            SettingField.of("name", STRING, Tag::getName), SettingField.of("content", STRING, Tag::getContent),
                            SettingField.ofOptionalValue("description", STRING_LIST, Tag::getDescription, new ArrayList<>()),
                            SettingField.ofOptionalValue("permission", STRING, Tag::getPermission, null),
                            SettingField.ofOptionalValue("order", INTEGER, Tag::getOrder, -1))
                    .apply(instance, Tag::new));

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
