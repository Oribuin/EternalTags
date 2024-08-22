package xyz.oribuin.eternaltags.obj;

import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Category {

    private final String id;  // The id of the category
    private String displayName;  // The display name of the category
    private CategoryType type;  // The type of the category
    private int order;  // The order of the category
    private boolean bypassPermission;  // If the category bypasses the permission check for tags.
    private String permission;  // The permission required to view the category

    public Category(String id) {
        this.id = id;
        this.displayName = StringUtils.capitalize(id.toLowerCase());
        this.type = CategoryType.CUSTOM;
        this.order = -1;
        this.bypassPermission = false;
        this.permission = "eternaltags.category." + id.toLowerCase();
    }

    /**
     * Check if a player has access to the category
     *
     * @param player The player
     *
     * @return If the player has access to the category
     */
    public boolean canUse(Player player) {
        if (this.permission == null) return true;

        return player.hasPermission(this.permission);
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType categoryType) {
        this.type = categoryType;
    }

    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isBypassPermission() {
        return this.bypassPermission;
    }

    public void setBypassPermission(boolean bypassPermission) {
        this.bypassPermission = bypassPermission;
    }

    @Nullable
    public String getPermission() {
        return this.permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

}
