package xyz.oribuin.eternaltags.obj;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.manager.DataManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Tag {

    private final String id; // The id of the tag
    private String name; // The name of the tag
    private String tag; // The tag to be added to the player
    private String permission;   // The permission required to use the tag
    private List<String> description; // The description of the tag
    private int order; // The order of the tag
    private ItemStack icon; // The icon of the tag
    private String category; // The category the tag is in
    private boolean handIcon; // Whether the icon is from the player's hand or not, internal method for tag saving
    private File file; // The file the tag is saved in

    /**
     * The tag instance for the plugin
     *
     * @param id   The id of the tag
     * @param name The name of the tag
     * @param tag  The tag to be added to the player
     */
    public Tag(String id, String name, String tag) {
        this.file = null;
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
     * The tag instance for the plugin
     *
     * @param file The file the tag is saved in
     * @param id   The id of the tag
     * @param name The name of the tag
     * @param tag  The tag to be added to the player
     */
    public Tag(File file, String id, String name, String tag) {
        this.file = file;
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
        if (Setting.REMOVE_TAGS.getBoolean() && this.permission != null && !player.hasPermission(this.permission)) {
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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon;
    }

    public void setIcon(Material material) {
        if (material == null) {
            this.icon = null;
            return;
        }

        this.icon = new ItemStack(material);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isHandIcon() {
        return handIcon;
    }

    public void setHandIcon(boolean handIcon) {
        this.handIcon = handIcon;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

}
