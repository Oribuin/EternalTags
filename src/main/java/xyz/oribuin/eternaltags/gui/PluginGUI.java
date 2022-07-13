package xyz.oribuin.eternaltags.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.utils.HexUtils;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import xyz.oribuin.eternaltags.util.ItemBuilder;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("unckecked")
public abstract class PluginGUI {

    protected final RosePlugin rosePlugin;
    protected CommentedFileConfiguration config;

    public PluginGUI(RosePlugin rosePlugin) {
        this.rosePlugin = rosePlugin;
    }


    /**
     * Get the amount of rows for the gui
     */
    protected abstract int rows();

    /**
     * Get the default config values for the GUI
     *
     * @return The default config values
     */
    protected abstract Map<String, Object> getDefaultValues();

    /**
     * @return The name of the GUI
     */
    protected abstract String getMenuName();

    /**
     * Get the config values for the GUI
     *
     * @param path The path to the config values
     * @param def  The default value if the path is not found
     * @param <T>  The type of the config value
     */
    protected final <T> T get(String path, T def) {
        return TagsUtils.get(this.config, path, def);
    }

    protected final <T> T get(String path) {
        return this.get(path, null);
    }

    /**
     * Create the menu file if it doesn't exist and add the default values
     */
    public void load() {
        final File folder = new File(this.rosePlugin.getDataFolder(), "menus");
        boolean newFile = false;
        if (!folder.exists()) {
            folder.mkdirs();
        }

        final File file = new File(folder, this.getMenuName() + ".yml");
        try {
            if (!file.exists()) {
                file.createNewFile();
                newFile = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
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

        this.config.save(file);
    }

    /**
     * Create a paged GUI for the given player
     *
     * @param player The player to create the GUI for
     * @return The created GUI
     */
    protected final PaginatedGui createPagedGUI(Player player) {
        return Gui.paginated().rows(this.rows()).title(this.format(player, this.get("menu-name", this.getMenuName()))).disableAllInteractions().create();
    }

    /**
     * Create a GUI for the given player without pages
     *
     * @param player The player to create the GUI for
     * @return The created GUI
     */
    protected Gui createGUI(Player player) {
        return Gui.gui().rows(this.rows()).title(this.format(player, this.get("menu-name", this.getMenuName()))).disableAllInteractions().create();
    }

    /**
     * Place an empty item in the gui.
     *
     * @param gui  The GUI
     * @param slot The Item Slot
     * @param item The Item
     */
    protected final void put(BaseGui gui, int slot, ItemStack item) {
        gui.setItem(slot, new GuiItem(item));
    }

    /**
     * A cleaner function for setting an item in the gui.
     *
     * @param gui      The GUI
     * @param itemPath The path to the item
     * @param player   The item viewer
     */
    protected final void put(BaseGui gui, String itemPath, Player player) {
        this.put(gui, itemPath, player, StringPlaceholders.empty());
    }

    /**
     * A cleaner function for setting an item in the gui.
     *
     * @param gui          The GUI
     * @param itemPath     The path to the item
     * @param viewer       The item viewer
     * @param placeholders The placeholders to use
     */
    protected final void put(BaseGui gui, String itemPath, Player viewer, StringPlaceholders placeholders) {
        this.put(gui, itemPath, viewer, placeholders, event -> {
            // Empty Function
        });
    }

    /**
     * A cleaner function for setting an item in the gui.
     *
     * @param gui           The GUI
     * @param itemPath      The path to the item
     * @param viewer        The item viewer
     * @param eventConsumer The event consumer
     */
    protected final void put(BaseGui gui, String itemPath, Player viewer, Consumer<InventoryClickEvent> eventConsumer) {
        this.put(gui, itemPath, viewer, StringPlaceholders.empty(), eventConsumer);
    }

    /**
     * A cleaner function for setting an item in the gui.
     *
     * @param gui           The GUI
     * @param itemPath      The path to the item
     * @param viewer        The item viewer
     * @param placeholders  The placeholders to use
     * @param eventConsumer The event consumer
     */
    protected final void put(BaseGui gui, String itemPath, Player viewer, StringPlaceholders placeholders, Consumer<InventoryClickEvent> eventConsumer) {
        if (!this.get(itemPath + ".enabled", true)) return;

        Integer slot = this.get(itemPath + ".slot", null);
        if (slot != null) this.put(gui, slot, itemPath, viewer, placeholders, eventConsumer);

        final List<?> slots = this.config.getList(itemPath + ".slots");
        if (slots == null) return;

        for (Object slotObject : slots) {
            if (slotObject instanceof Integer) this.put(gui, (Integer) slotObject, itemPath, viewer, placeholders, eventConsumer);

            if (slotObject instanceof String) {
                this.put(gui, this.parseStringToSlots((String) slotObject), itemPath, viewer, placeholders, eventConsumer);
            }
        }
    }

    /**
     * A cleaner function for setting an item in the gui.
     *
     * @param gui      The GUI
     * @param slot     The slot of the item
     * @param itemPath The path to the item
     */
    protected final void put(BaseGui gui, int slot, String itemPath, Player viewer) {
        this.put(gui, slot, itemPath, viewer, StringPlaceholders.empty());
    }

    /**
     * A cleaner function for setting an item in the gui.
     *
     * @param gui          The GUI
     * @param slot         The slot of the item
     * @param itemPath     The path to the item
     * @param placeholders The placeholders to use
     */
    protected final void put(BaseGui gui, int slot, String itemPath, Player viewer, StringPlaceholders placeholders) {
        this.put(gui, slot, itemPath, viewer, placeholders, inventoryClickEvent -> {
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
    protected final void put(BaseGui gui, int slot, String itemPath, Player viewer, StringPlaceholders placeholders, Consumer<InventoryClickEvent> eventConsumer) {
        this.put(gui, List.of(slot), itemPath, viewer, placeholders, eventConsumer);
    }

    /**
     * A messy function for setting multiple items in the gui.
     *
     * @param gui           The GUI
     * @param slots         The slots of the items
     * @param itemPath      The path to the item
     * @param viewer        The item viewer
     * @param placeholders  The placeholders to use
     * @param eventConsumer The event consumer
     */
    protected final void put(BaseGui gui, List<Integer> slots, String itemPath, Player viewer, StringPlaceholders placeholders, Consumer<InventoryClickEvent> eventConsumer) {
        ItemStack item = TagsUtils.getItemStack(this.config, itemPath, viewer, placeholders);
        if (item == null) item = new ItemBuilder(Material.BARRIER).setName(HexUtils.colorify("&cInvalid Material:" + itemPath + ".material")).create();

        gui.setItem(slots, new GuiItem(item, eventConsumer::accept));
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player The player to format the string for
     * @param text   The string to format
     * @return The formatted string
     */
    protected final Component format(Player player, String text) {
        return Component.text(TagsUtils.format(player, text));
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player       The player to format the string for
     * @param text         The text to format
     * @param placeholders The placeholders to replace
     * @return The formatted string
     */
    protected final Component format(Player player, String text, StringPlaceholders placeholders) {
        return Component.text(TagsUtils.format(player, text, placeholders));
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player The player to format the string for
     * @param text   The text to format
     * @return The formatted string
     */
    protected final String formatString(Player player, String text) {
        return TagsUtils.format(player, text);
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player       The player to format the string protected
     * @param text         The text to format
     * @param placeholders The placeholders to replace
     * @return The formatted string
     */
    protected final String formatString(Player player, String text, StringPlaceholders placeholders) {
        return TagsUtils.format(player, text, placeholders);
    }

    /**
     * Get a bukkit color from a hex code
     *
     * @param hex The hex code
     */
    protected Color getColor(String hex) {
        return TagsUtils.fromHex(hex);
    }


    protected List<Integer> parseStringToSlots(String string) {
        String[] split = string.split("-");
        if (split.length != 2) {
            try {
                return List.of(Integer.parseInt(string));
            } catch (NumberFormatException ignored) {
            }
        }

        return this.getNumberRange(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }

    /**
     * Get a range of numbers as a list
     *
     * @param start The start of the range
     * @param end   The end of the range
     * @return A list of numbers
     */
    protected List<Integer> getNumberRange(int start, int end) {
        if (start == end) {
            return List.of(start);
        }

        final List<Integer> list = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            list.add(i);
        }

        return list;
    }

    /**
     * Get the page placeholders for the gui
     *
     * @param gui The gui
     * @return The page placeholders
     */
    protected StringPlaceholders getPagePlaceholders(PaginatedGui gui) {
        return StringPlaceholders.builder().addPlaceholder("page", gui.getCurrentPageNum()).addPlaceholder("total", Math.max(gui.getPagesNum(), 1)).addPlaceholder("next", gui.getNextPageNum()).addPlaceholder("previous", gui.getPrevPageNum()).build();
    }

    protected final void sync(Runnable runnable) {
        this.rosePlugin.getServer().getScheduler().runTask(this.rosePlugin, runnable);
    }

    protected final void async(Runnable runnable) {
        this.rosePlugin.getServer().getScheduler().runTaskAsynchronously(this.rosePlugin, runnable);
    }

}
