package xyz.oribuin.eternaltags.gui.menu;

import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import dev.triumphteam.gui.guis.ScrollingGui;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.action.Action;
import xyz.oribuin.eternaltags.gui.MenuItem;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.gui.PluginMenu;
import xyz.oribuin.eternaltags.gui.enums.SortType;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FavouritesGUI extends PluginMenu {

    protected final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
    private final Map<String, ItemStack> tagItems = new LinkedHashMap<>(); // Cache the tag items, so we don't have to create them every time.

    /**
     * Constructor for FavouritesGUI
     */
    public FavouritesGUI() {
        super(EternalTags.getInstance());
    }

    /**
     * Load the GUI configuration and tag slots
     */
    @Override
    public void load() {
        super.load();

        this.tagItems.clear();
        loadSlots("tag-item.slots");
    }

    /**
     * Open the GUI for a player with an optional filter
     *
     * @param player The player to open the GUI for
     */
    public void open(Player player) {
        super.openGui(player, "EternalTags | %page%/%total%", this::addTags);
    }

    /**
     * Add tags to the GUI
     *
     * @param gui    The GUI to add tags to
     * @param player The player viewing the GUI
     */
    private void addTags(BaseGui gui, Player player) {
        if (gui instanceof PaginatedGui paginatedGui) // Remove all items from the GUI
            paginatedGui.clearPageItems();

        if (gui instanceof ScrollingGui scrollingGui) // Remove all items from the GUI
            scrollingGui.clearPageItems();


        Map<ClickType, List<Action>> tagActions = this.getTagActions();
        for (Tag tag : this.getTags(player)) {
            GuiAction<InventoryClickEvent> action = event -> {

                // Make sure the player has permission to use the tag
                if (!this.manager.canUseTag(player, tag)) {
                    this.locale.sendMessage(player, "no-permission");
                    gui.close(player);
                    return;
                }

                // Run the tag actions
                if (!tagActions.isEmpty()) {
                    this.runActions(tagActions, event, this.getTagPlaceholders(tag, player));
                    gui.close(player);
                    return;
                }

                // If the player is shift clicking, toggle the favourite
                if (event.isShiftClick()) {
                    this.toggleFavourite(player, tag, manager);
                    this.addTags(gui, player);
                    return;
                }

                // Set the tag
                this.setTag(player, tag);
                gui.close(player);
            };

            // If the tag is already in the cache, use that instead of creating a new one.
            if (Setting.CACHE_GUI_TAGS.getBoolean() && this.tagItems.containsKey(tag.getId())) {
                GuiItem item = new GuiItem(this.tagItems.get(tag.getId()));
                item.setAction(action);
                gui.addItem(item);
                continue;
            }

            GuiItem item = new GuiItem(this.getTagItem(player, tag), action);

            // Add the tag to the cache
            if (Setting.CACHE_GUI_TAGS.getBoolean()) this.tagItems.put(tag.getId(), item.getItemStack());

            gui.addItem(item);

        }

        gui.update();
    }


    /**
     * Get all the tags that should be displayed in the GUI
     *
     * @param player The player to get the tags for
     * @return A list of tags
     */
    private List<Tag> getTags(Player player) {
        SortType sortType = TagsUtils.getEnum(SortType.class, this.config.getString("gui-settings.sort-type"));
        if (sortType == null) sortType = SortType.ALPHABETICAL;

        List<Tag> tags = new ArrayList<>(this.manager.getUsersFavourites(player.getUniqueId()).values());

        sortType.sort(tags);
        return tags;
    }

    /**
     * Change a player's active tag, and send the message to the player.
     *
     * @param player The player
     * @param tag    The tag
     */
    private void setTag(Player player, Tag tag) {
        Tag activeTag = this.manager.getUserTag(player);
        if (activeTag != null && activeTag.equals(tag) && Setting.RE_EQUIP_CLEAR.getBoolean()) {
            this.clearTag(player);
            return;
        }

        tag.equip(player);
        this.locale.sendMessage(player, "command-set-changed", StringPlaceholders.of("tag", this.manager.getDisplayTag(tag, player)));
    }

    /**
     * Get the name of the menu
     *
     * @return The name of the menu
     */
    @Override
    public String getMenuName() {
        return "favorites-gui";
    }


    /**
     * Add functional items to the GUI
     *
     * @param gui    The GUI to add items to
     * @param player The player viewing the GUI
     */
    @Override
    protected void addFunctionalItems(PaginatedGui gui, Player player) {
        super.addFunctionalItems(gui, player);

        MenuItem.create(this.config)
                .path("categories")
                .player(player)
                .action((item, event) -> {
                    item.sound((Player) event.getWhoClicked());
                    MenuProvider.get(CategoryGUI.class).open(player);
                })
                .place(gui);

        MenuItem.create(this.config)
                .path("reset-favourites")
                .player(player)
                .action(event -> {
                    if (event.getClick() == ClickType.DOUBLE_CLICK)
                        this.clearFavourites(player, gui);
                })
                .place(gui);
    }

    private void clearFavourites(Player player, BaseGui gui) {
        this.manager.clearFavourites(player.getUniqueId());
        this.locale.sendMessage(player, "command-favorite-cleared");
        this.close(gui, player);
    }

}