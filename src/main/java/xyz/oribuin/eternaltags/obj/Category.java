package xyz.oribuin.eternaltags.obj;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Category {

    private final @NotNull String id;  // The id of the category
    private @NotNull String displayName;  // The display name of the category
    private int order;  // The order of the category
    private boolean isDefault;  // If the category is the default category (New tags are added to this category)
    private boolean isGlobal;  // If the category is global (All tags are in this category)
    private boolean bypassPermission;  // If the category bypasses the permission check for tags.
    private @Nullable String permission;  // The permission required to view the category

    public Category(@NotNull String id) {
        this.id = id;
        this.displayName = StringUtils.capitalize(id.toLowerCase());
        this.order = -1;
        this.isDefault = false;
        this.isGlobal = false;
        this.bypassPermission = false;
        this.permission = null;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@NotNull String displayName) {
        this.displayName = displayName;
    }

    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isDefault() {
        return this.isDefault;
    }

    public void setDefault(boolean aDefault) {
        this.isDefault = aDefault;
    }

    public boolean isGlobal() {
        return this.isGlobal;
    }

    public void setGlobal(boolean global) {
        this.isGlobal = global;
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

    public void setPermission(@Nullable String permission) {
        this.permission = permission;
    }

}
