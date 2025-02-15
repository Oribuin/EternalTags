package xyz.oribuin.eternaltags.obj;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.config.Setting;
import xyz.oribuin.eternaltags.manager.DataManager;

import java.util.ArrayList;
import java.util.List;

public class Tag {

    private final @NotNull String id; // The id of the tag
    private @NotNull String name; // The name of the tag
    private @NotNull String tag; // The tag to be added to the player
    private @Nullable String permission;   // The permission required to use the tag
    private @NotNull List<String> description; // The description of the tag
    private int order; // The order of the tag
    private @Nullable ItemStack icon; // The icon of the tag
    private @Nullable String category; // The category the tag is in
    private boolean handIcon; // Whether the icon is from the player's hand or not, internal method for tag saving

    public Tag(@NotNull String id, @NotNull String name, @NotNull String tag) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.description = new ArrayList<>();
        this.permission = "eternaltags.tag." + id.toLowerCase();
        this.order = -1;
        this.icon = null;
        this.category = null;
        this.handIcon = false;
    }

    /**
     * Equip a tag to a specific player.
     *
     * @param player The player to equip the tag to
     */
    public void equip(Player player) {
        DataManager dataManager = EternalTags.getInstance().getManager(DataManager.class);

        // Remove the tag if the player does not have permission
        if (Setting.REMOVE_TAGS.get() && this.permission != null && !player.hasPermission(this.permission)) {
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
        DataManager dataManager = EternalTags.getInstance().getManager(DataManager.class);
        dataManager.removeUser(player.getUniqueId());
    }

    /**
     * Check if a player has permission to use the tag.
     *
     * @param player The player to check
     *
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
        return tag;
    }

    public void setTag(@NotNull String tag) {
        this.tag = tag;
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

    public @Nullable ItemStack getIcon() {
        return icon;
    }

    public void setIcon(@Nullable ItemStack icon) {
        this.icon = icon;
    }

    public void setIcon(@Nullable Material material) {
        if (material == null) {
            this.icon = null;
            return;
        }

        this.icon = new ItemStack(material);
    }

    public @Nullable String getCategory() {
        return category;
    }

    public void setCategory(@Nullable String category) {
        this.category = category;
    }

    public boolean isHandIcon() {
        return handIcon;
    }

    public void setHandIcon(boolean handIcon) {
        this.handIcon = handIcon;
    }
}
