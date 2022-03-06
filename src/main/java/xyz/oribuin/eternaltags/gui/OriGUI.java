package xyz.oribuin.eternaltags.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.utils.HexUtils;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.gui.Item;
import xyz.oribuin.gui.PaginatedGui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class OriGUI {

    protected final RosePlugin rosePlugin;
    private CommentedFileConfiguration config;

    public OriGUI(RosePlugin rosePlugin) {
        this.rosePlugin = rosePlugin;
    }

    /**
     * Create & Display the gui to a player
     *
     * @param player The player viewing the gui.
     */
    public abstract void createGUI(Player player);

    /**
     * Get all the required values for the plugin.
     *
     * @return The map of required values
     */
    @NotNull
    public abstract Map<String, Object> getRequiredValues();

    /**
     * Get the amount of rows for the gui.
     *
     * @return The amount of rows.
     */
    public abstract int getRows();

    /**
     * Get the official gui id
     *
     * @return The id of the gui.
     */
    @NotNull
    public abstract String getMenuName();

    /**
     * Gets the page slots for the gui.
     *
     * @return The list of page slots.
     */
    @NotNull
    public abstract List<Integer> getPageSlots();


    /**
     * A cleaner function for setting an item in the gui.
     *
     * @param gui      The GUI
     * @param slot     The slot of the item
     * @param itemPath The path to the item
     */
    public void put(PaginatedGui gui, int slot, String itemPath, Player viewer) {
        this.put(gui, slot, itemPath, viewer, inventoryClickEvent -> {
            // Empty Function
        });
    }

    /**
     * A cleaner function for setting an item in the gui.
     *
     * @param gui           The GUI
     * @param slot          The slot of the item
     * @param itemPath      The path to the item
     * @param eventConsumer The item functionality.
     */
    public void put(PaginatedGui gui, int slot, String itemPath, Player viewer, Consumer<InventoryClickEvent> eventConsumer) {
        ItemStack item = this.recreateItem(itemPath, viewer);
        if (item == null)
            item = new Item.Builder(Material.BARREL)
                    .setName(HexUtils.colorify("&cInvalid Material:" + itemPath + ".material"))
                    .create();

        gui.setItem(slot, item, eventConsumer);
    }

    /**
     * Recreate an ItemStack from a config option
     *
     * @param path   The path to the item
     * @param viewer The player for associated placeholders.
     * @return The itemstack.
     */
    public final ItemStack recreateItem(String path, Player viewer) {
        final String materialName = this.config.getString(path + ".material");
        if (materialName == null)
            return null;

        final Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null || material.isAir() || !material.isItem())
            return null;

        return new Item.Builder(material)
                .setName(this.format(viewer, this.get(path + ".name", null)))
                .setLore(this.get(path + ".lore", new ArrayList<String>()).stream().map(s -> this.format(viewer, s)).collect(Collectors.toList()))
                .setAmount(Math.max(this.get(path + ".amount", 1), 1))
                .glow(this.get(path + ".glow", false))
                .setTexture(this.get(path + ".texture", null))
                .create();
    }

    /**
     * Create a base paginated gui object used for everything.
     *
     * @param player The player.
     * @return The Paginated GUI
     */
    public final PaginatedGui createBaseGUI(Player player) {
        final PaginatedGui gui = new PaginatedGui(this.getRows() * 9, this.format(player, this.config.getString("menu-name")), this.getPageSlots());
        gui.setDefaultClickFunction(event -> {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
            ((Player) event.getWhoClicked()).updateInventory();
        });

        gui.setPersonalClickAction(event -> gui.getDefaultClickFunction().accept(event));
        return gui;
    }

    /**
     * Load the plugin configuration files.
     */
    public final OriGUI loadConfiguration() {
        final File folder = new File(this.rosePlugin.getDataFolder(), "menus");

        if (!folder.exists())
            folder.mkdir();

        final File file = new File(folder, this.getMenuName());
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.config = CommentedFileConfiguration.loadConfiguration(file);
        this.getRequiredValues().forEach((path, object) -> {
            if (this.config.get(path) == null)
                this.config.set(path, object);
        });

        this.config.save();
        return this;
    }

    /**
     * Format a string through hex utils, placeholder api
     *
     * @param player The player
     * @param text   The text being formatted
     * @return The formatted message.
     */
    public final String format(Player player, String text) {
        return this.format(player, text, StringPlaceholders.empty());
    }

    /**
     * Format a string through hex utils, placeholder api & string placeholders
     *
     * @param player       The player
     * @param text         The text being formatted
     * @param placeholders The string placeholders
     * @return The formatted message.
     */
    public final String format(Player player, String text, StringPlaceholders placeholders) {
        return HexUtils.colorify(PlaceholderAPI.setPlaceholders(player, placeholders.apply(text)));
    }

    /**
     * Get a configuration value or default from the file config
     *
     * @param path The path to the value
     * @param def  The default value if the original value doesnt exist
     * @return The config value or default value.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String path, T def) {
        return this.config.get(path) != null ? (T) this.config.get(path) : def;
    }

}
