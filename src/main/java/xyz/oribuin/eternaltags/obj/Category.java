package xyz.oribuin.eternaltags.obj;

import org.bukkit.inventory.ItemStack;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Category {

    private final @NotNull String id;  // The id of the category
    private @NotNull String displayName;  // The display name of the category
    private @NotNull List<String> tags;  // The tags in the category
    private @Nullable ItemStack icon;  // The icon of the category
    private int order;  // The order of the category

    public Category(@NotNull String id) {
        this.id = id;
        this.displayName = StringUtils.capitalise(id.toLowerCase());
        this.tags = new ArrayList<>();
        this.icon = null;
        this.order = -1;
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

    @NotNull
    public List<String> getTags() {
        return tags;
    }

    public void setTags(@NotNull List<String> tags) {
        this.tags = tags;
    }

    @Nullable
    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(@Nullable ItemStack icon) {
        this.icon = icon;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
