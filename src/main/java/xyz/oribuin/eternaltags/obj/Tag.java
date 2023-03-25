package xyz.oribuin.eternaltags.obj;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Tag {

    private final @NotNull String id;
    private @NotNull String name;
    private @NotNull String tag;
    private @NotNull String permission;
    private @NotNull List<String> description;
    private int order;
    private @Nullable ItemStack icon;

    public Tag(@NotNull String id, @NotNull String name, @NotNull String tag) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.description = new ArrayList<>();
        this.permission = "eternaltags.tag." + id.toLowerCase();
        this.order = -1;
        this.icon = null;
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

    public @NotNull String getPermission() {
        return permission;
    }

    public void setPermission(@NotNull String permission) {
        this.permission = permission;
    }

    public @NotNull List<String> getDescription() {
        return description;
    }

    public void setDescription(@NotNull List<String> description) {
        this.description = description;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    public @Nullable ItemStack getIcon() {
        return icon;
    }

    public void setIcon(@Nullable ItemStack icon) {
        this.icon = icon;
    }
}
