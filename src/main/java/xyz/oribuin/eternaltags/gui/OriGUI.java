package xyz.oribuin.eternaltags.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.utils.HexUtils;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.manager.ConfigurationManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.obj.BukkitColour;
import xyz.oribuin.gui.Item;
import xyz.oribuin.gui.PaginatedGui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
    public abstract void createGUI(Player player, @Nullable String keyword);

    /**
     * Get all the required values for the plugin.
     *
     * @return The map of required values
     */
    @NotNull
    public abstract Map<String, Object> getDefaultValues();

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
     * Place an empty item in the gui.
     *
     * @param gui  The GUI
     * @param slot The Item Slot
     * @param item The Item
     */
    public final void put(PaginatedGui gui, int slot, ItemStack item) {
        gui.setItem(slot, item, inventoryClickEvent -> {
            // Empty Function
        });
    }

    /**
     * A cleaner function for setting an item in the gui.
     *
     * @param gui      The GUI
     * @param itemPath The path to the item
     * @param viewer   The item viewer
     */
    public final void put(PaginatedGui gui, String itemPath, Player viewer) {
        this.put(gui, this.get(itemPath + ".slot", null), itemPath, viewer);
    }

    /**
     * A cleaner function for setting an item in the gui.
     *
     * @param gui      The GUI
     * @param itemPath The path to the item
     * @param viewer   The item viewer
     */
    public final void put(PaginatedGui gui, String itemPath, Player viewer, Consumer<InventoryClickEvent> eventConsumer) {
        this.put(gui, this.get(itemPath + ".slot", null), itemPath, viewer, eventConsumer);
    }

    /**
     * A cleaner function for setting an item in the gui.
     *
     * @param gui      The GUI
     * @param slot     The slot of the item
     * @param itemPath The path to the item
     */
    public final void put(PaginatedGui gui, int slot, String itemPath, Player viewer) {
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
    public final void put(PaginatedGui gui, int slot, String itemPath, Player viewer, Consumer<InventoryClickEvent> eventConsumer) {
        ItemStack item = this.recreateItem(itemPath, viewer);
        if (item == null)
            item = new Item.Builder(Material.BARREL)
                    .setName(HexUtils.colorify("&cInvalid Material:" + itemPath + ".material"))
                    .create();

        gui.setItem(slot, item, eventConsumer);
    }

    /**
     * Create the itemstack for a tag item.
     *
     * @param tag    The tag
     * @param path   The path to the tag item config
     * @param viewer The player viewing the item
     * @return The ItemStack.
     */
    public final ItemStack createTagItem(Tag tag, String path, Player viewer) {
        final StringPlaceholders plc = this.getTagPlaceholders(tag);
        final String materialName = this.config.getString(path + ".material");
        if (materialName == null)
            return null;

        Material material;
        if (tag.getIcon() != null) {
            material = tag.getIcon();
        } else {
            material = Material.matchMaterial(materialName.toUpperCase());
            if (material == null || material.isAir() || !material.isItem())
                return null;
        }

        List<String> lore = this.get(path + ".lore", new ArrayList<>());
        lore = this.getTagLore(viewer, tag, lore);
//        lore = lore.stream().map(s -> this.format(viewer, s, plc)).collect(Collectors.toList());

        ItemFlag[] flags = new ItemFlag[]{};
        if (this.get(path + ".flags", null) != null)
            flags = this.get(path + ".flags", new ArrayList<String>()).stream()
                    .map(String::toLowerCase)
                    .map(ItemFlag::valueOf)
                    .toArray(ItemFlag[]::new);

        return new Item.Builder(material)
                .setName(this.format(viewer, this.get(path + ".name", null), plc))
                .setLore(lore)
                .setAmount(Math.max(this.get(path + ".amount", 1), 1))
                .setFlags(flags)
                .glow(this.get(path + ".glow", false))
                .setTexture(this.get(path + ".texture", null))
                .setModel(this.get(path + ".model-data", -1))
                .create();
    }

    public final ItemStack recreateItem(String path, Player viewer) {
        return this.recreateItem(path, viewer, StringPlaceholders.empty());
    }

    /**
     * Recreate an ItemStack from a config option
     *
     * @param path   The path to the item
     * @param viewer The player for associated placeholders.
     * @return The itemstack.
     */
    public final ItemStack recreateItem(String path, Player viewer, StringPlaceholders placeholders) {
        final String materialName = this.config.getString(path + ".material");
        if (materialName == null)
            return null;

        final Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null || material.isAir() || !material.isItem())
            return null;

        List<String> lore = this.get(path + ".lore", new ArrayList<>());
        lore = lore.stream().map(s -> this.format(viewer, s, placeholders))
                .collect(Collectors.toList());

        ItemFlag[] flags = new ItemFlag[]{};
        if (this.get(path + ".flags", null) != null)
            flags = this.get(path + ".flags", new ArrayList<String>()).stream()
                    .map(String::toLowerCase)
                    .map(ItemFlag::valueOf)
                    .toArray(ItemFlag[]::new);

        String potionColor = this.get(path + ".potion-color", null);
        Color color = null;
        if (potionColor != null) {
            if (potionColor.startsWith("#"))
                color = this.hexToColor(potionColor);
            else
                color = BukkitColour.match(potionColor).orElse(BukkitColour.AQUA).get();
        }

        return new Item.Builder(material)
                .setName(this.format(viewer, this.get(path + ".name", null), placeholders))
                .setLore(lore)
                .setAmount(Math.max(this.get(path + ".amount", 1), 1))
                .setFlags(flags)
                .glow(this.get(path + ".glow", false))
                .setTexture(this.get(path + ".texture", null))
                .setModel(this.get(path + ".model-data", -1))
                .setPotionColor(color)
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
    public final void loadConfiguration() {
        final File folder = new File(this.rosePlugin.getDataFolder(), "menus");
        boolean newFile = false;
        if (!folder.exists())
            folder.mkdir();

        final File file = new File(folder, this.getMenuName() + ".yml");
        try {
            if (!file.exists()) {
                file.createNewFile();
                newFile = true;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.config = CommentedFileConfiguration.loadConfiguration(file);
        if (newFile) {
            this.getDefaultValues().forEach((path, object) -> {
                if (path.startsWith("#"))
                    this.config.addPathedComments(path, object.toString());
                else
                    this.config.set(path, object);
            });
        }

        this.config.save();
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
        if (text == null)
            return "";

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
    public final <T> T get(String path, T def) {
        return this.config.get(path) != null ? (T) this.config.get(path) : def;
    }

    /**
     * Get the basic placeholders for a tag.
     *
     * @param tag The tag
     * @return The placeholders
     */
    public StringPlaceholders getTagPlaceholders(Tag tag) {
        final StringPlaceholders.Builder builder = StringPlaceholders.builder();
        builder.addPlaceholder("tag", HexUtils.colorify(tag.getTag()));
        builder.addPlaceholder("id", tag.getId());
        builder.addPlaceholder("name", tag.getName());
//        builder.addPlaceholder("description", TagsUtil.formatList(tag.getDescription())); // Unused
        return builder.build();
    }

    /**
     * Get the placeholders for the Page GUI
     *
     * @param gui The GUI
     * @return The placeholders.
     */
    public StringPlaceholders getPages(PaginatedGui gui) {
        return StringPlaceholders.builder("page", gui.getPage())
                .addPlaceholder("previous", gui.getPrevPage())
                .addPlaceholder("next", gui.getNextPage())
                .addPlaceholder("total", Math.max(gui.getPage(), gui.getTotalPages()))
                .build();
    }

    /**
     * Convert a hex code to a color
     *
     * @param hex The hex code
     * @return The bukkit color
     */
    private Color hexToColor(String hex) {
        if (hex == null)
            return Color.AQUA;

        java.awt.Color hexColor;
        try {
            hexColor = java.awt.Color.decode(hex);
        } catch (NumberFormatException ex) {
            return Color.AQUA;
        }

        return Color.fromRGB(hexColor.getRed(), hexColor.getGreen(), hexColor.getBlue());
    }


    /**
     * Reformat a stringlist created for tags into supporting multiline description.
     *
     * @param list unformatted list
     * @return the new list.
     */
    public List<String> getTagLore(Player player, Tag tag, List<String> list) {
        final StringPlaceholders plc = this.getTagPlaceholders(tag);
        final List<String> lore = new ArrayList<>(list);

        for (int i = 0; i < lore.size(); i++) {
            String index = lore.get(i);

            if (!index.toLowerCase().contains("%description%"))
                continue;

            final List<String> desc = new ArrayList<>(tag.getDescription());
            if (desc.isEmpty()) {
                lore.set(i, index.replace("%description%", ConfigurationManager.Setting.FORMATTED_PLACEHOLDER.getString()));
                break;
            }

            lore.set(i, index.replace("%description%", desc.get(0)));
            desc.remove(0);

            final String descriptionFormat = this.get("description-format", " &f| &7");
            AtomicInteger integer = new AtomicInteger(i);
            desc.forEach(s -> lore.add(integer.incrementAndGet(), descriptionFormat + s));
            break;
        }

        return lore.stream().map(s -> this.format(player, s, plc)).collect(Collectors.toList());
    }
}
