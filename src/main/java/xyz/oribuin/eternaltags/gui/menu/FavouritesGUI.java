package xyz.oribuin.eternaltags.gui.menu;

import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.action.Action;
import xyz.oribuin.eternaltags.gui.MenuItem;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.gui.PluginMenu;
import xyz.oribuin.eternaltags.gui.enums.SortType;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FavouritesGUI extends PluginMenu {

    private final TagsManager manager;
    private final LocaleManager locale;
    private final Map<String, ItemStack> tagItems;
    private final List<Integer> tagSlots;

    /**
     * Constructor for FavouritesGUI
     */
    public FavouritesGUI() {
        super(EternalTags.getInstance());
        this.manager = this.rosePlugin.getManager(TagsManager.class);
        this.locale = this.rosePlugin.getManager(LocaleManager.class);
        this.tagItems = new LinkedHashMap<>();
        this.tagSlots = new ArrayList<>();
    }

    /**
     * Load the GUI configuration and tag slots
     */
    @Override
    public void load() {
        super.load();
        this.tagItems.clear();
        this.loadTagSlots();
    }

    /**
     * Load the tag slots from the configuration
     */
    private void loadTagSlots() {
        this.tagSlots.clear();
        List<String> slotsConfig = this.config.getStringList("tag-item.slots");
        if (slotsConfig.isEmpty()) {
            int rows = this.config.getInt("gui-settings.rows", 6);
            for (int i = 0; i < rows * 9; i++) {
                this.tagSlots.add(i);
            }
        } else {
            for (String slotConfig : slotsConfig) {
                if (slotConfig.contains("-")) {
                    String[] range = slotConfig.split("-");
                    int start = Integer.parseInt(range[0]);
                    int end = Integer.parseInt(range[1]);
                    for (int i = start; i <= end; i++) {
                        this.tagSlots.add(i);
                    }
                } else {
                    this.tagSlots.add(Integer.parseInt(slotConfig));
                }
            }
        }
    }

    /**
     * Open the GUI for a player
     *
     * @param player The player to open the GUI for
     */
    public void open(@NotNull Player player) {
        String finalMenuTitle = this.config.getString("gui-settings.title", "EternalTags | %page%/%total%");

        boolean scrollingGui = this.config.getBoolean("gui-settings.scrolling-gui", false);
        ScrollType scrollingType = TagsUtils.getEnum(
                ScrollType.class,
                this.config.getString("gui-settings.scrolling-type"),
                ScrollType.VERTICAL
        );

        PaginatedGui gui = (scrollingGui && scrollingType != null) ? this.createScrollingGui(player, scrollingType) : this.createPagedGUI(player);

        this.setupGuiLayout(gui);
        this.addExtraItems(gui, player);
        this.addFunctionalItems(gui, player);

        gui.setPageSize(this.tagSlots.size());

        gui.open(player);

        Runnable task = () -> {
            this.addTagsToGui(gui, player);

            if (this.reloadTitle()) {
                this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
            }
        };

        if (this.addPagesAsynchronously())
            this.async(task);
        else
            task.run();
    }

    /**
     * Set up the initial layout of the GUI
     *
     * @param gui The GUI to set up
     */
    private void setupGuiLayout(PaginatedGui gui) {
        int rows = this.config.getInt("gui-settings.rows", 6);
        int totalSlots = rows * 9;

        for (int i = 0; i < totalSlots; i++) {
            if (!this.tagSlots.contains(i)) {
                gui.setItem(i, new GuiItem(new ItemStack(Material.AIR)));
            }
        }
    }

    /**
     * Add extra items to the GUI
     *
     * @param gui    The GUI to add items to
     * @param player The player viewing the GUI
     */
    private void addExtraItems(PaginatedGui gui, Player player) {
        CommentedConfigurationSection extraItems = this.config.getConfigurationSection("extra-items");
        if (extraItems != null) {
            for (String key : extraItems.getKeys(false)) {
                MenuItem.create(this.config)
                        .path("extra-items." + key)
                        .player(player)
                        .place(gui);
            }
        }
    }

    /**
     * Add functional items to the GUI
     *
     * @param gui    The GUI to add items to
     * @param player The player viewing the GUI
     */
    private void addFunctionalItems(PaginatedGui gui, Player player) {
        String finalMenuTitle = this.config.getString("gui-settings.title", "EternalTags | %page%/%total%");

        MenuItem.create(this.config)
                .path("next-page")
                .player(player)
                .action(event -> {
                    if (gui.next()) {
                        this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
                    }
                })
                .place(gui);

        MenuItem.create(this.config)
                .path("previous-page")
                .player(player)
                .action(event -> {
                    if (gui.previous()) {
                        this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
                    }
                })
                .place(gui);

        MenuItem.create(this.config)
                .path("clear-tag")
                .player(player)
                .action(event -> this.clearTag(player))
                .place(gui);

        MenuItem.create(this.config)
                .path("main-menu")
                .player(player)
                .action(event -> {
                    if (Setting.OPEN_CATEGORY_GUI_FIRST.getBoolean()) {
                        MenuProvider.get(CategoryGUI.class).open(player);
                    } else {
                        MenuProvider.get(TagsGUI.class).open(player, null);
                    }
                })
                .place(gui);

        MenuItem.create(this.config)
                .path("categories")
                .player(player)
                .action(event -> MenuProvider.get(CategoryGUI.class).open(player))
                .place(gui);

        MenuItem.create(this.config)
                .path("reset-favourites")
                .player(player)
                .action(event -> {
                    if (event.getClick() == ClickType.DOUBLE_CLICK)
                        this.clearFavourites(player, gui);
                })
                .place(gui);

        MenuItem.create(this.config)
                .path("search")
                .player(player)
                .action(event -> this.searchTags(player, gui))
                .place(gui);
    }

    /**
     * Add tags to the GUI
     *
     * @param gui    The GUI to add tags to
     * @param player The player viewing the GUI
     */
    private void addTagsToGui(PaginatedGui gui, Player player) {
        List<Tag> tags = this.getTags(player);
        Map<ClickType, List<Action>> tagActions = this.getTagActions();

        for (Tag tag : tags) {
            GuiAction<InventoryClickEvent> action = createTagAction(player, gui, tag, tagActions);

            GuiItem item;
            if (Setting.CACHE_GUI_TAGS.getBoolean() && this.tagItems.containsKey(tag.getId())) {
                item = new GuiItem(this.tagItems.get(tag.getId()));
                item.setAction(action);
            } else {
                ItemStack tagItem = this.getTagItem(player, tag);
                item = new GuiItem(tagItem, action);
                if (Setting.CACHE_GUI_TAGS.getBoolean())
                    this.tagItems.put(tag.getId(), tagItem);
            }

            gui.addItem(item);
        }

        gui.update();
    }

    /**
     * Create an action for a tag item
     *
     * @param player     The player viewing the GUI
     * @param gui        The GUI containing the tag
     * @param tag        The tag
     * @param tagActions The actions for the tag
     * @return The action for the tag item
     */
    private GuiAction<InventoryClickEvent> createTagAction(Player player, BaseGui gui, Tag tag, Map<ClickType, List<Action>> tagActions) {
        return event -> {
            if (!this.manager.canUseTag(player, tag)) {
                this.locale.sendMessage(player, "no-permission");
                gui.close(player);
                return;
            }

            if (!tagActions.isEmpty()) {
                this.runActions(tagActions, event, this.getTagPlaceholders(tag, player));
                gui.close(player);
                return;
            }

            if (event.isShiftClick()) {
                this.toggleFavourite(player, tag);
                if (gui instanceof PaginatedGui paginatedGui) {
                    this.addTagsToGui(paginatedGui, player);
                }
                return;
            }

            this.setTag(player, tag);
            gui.close(player);
        };
    }

    /**
     * Get the list of favourite tags for a player
     *
     * @param player The player to get tags for
     * @return The list of favourite tags
     */
    @NotNull
    private List<Tag> getTags(@NotNull Player player) {
        SortType sortType = TagsUtils.getEnum(SortType.class, this.config.getString("gui-settings.sort-type"));
        if (sortType == null)
            sortType = SortType.ALPHABETICAL;

        List<Tag> tags = new ArrayList<>(this.manager.getUsersFavourites(player.getUniqueId()).values());

        sortType.sort(tags);
        return tags;
    }

    /**
     * Set a tag for a player
     *
     * @param player The player to set the tag for
     * @param tag    The tag to set
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
     * Toggle a tag as a favourite for a player
     *
     * @param player The player to toggle the favourite for
     * @param tag    The tag to toggle
     */
    private void toggleFavourite(Player player, Tag tag) {
        boolean isFavourite = this.manager.isFavourite(player.getUniqueId(), tag);

        if (isFavourite)
            this.manager.removeFavourite(player.getUniqueId(), tag);
        else
            this.manager.addFavourite(player.getUniqueId(), tag);

        String message = locale.getLocaleMessage(isFavourite ? "command-favorite-off" : "command-favorite-on");
        this.locale.sendMessage(player, "command-favorite-toggled", StringPlaceholders.builder("tag", this.manager.getDisplayTag(tag, player))
                .add("toggled", message)
                .build());
    }

    /**
     * Clear all favourites for a player
     *
     * @param player The player to clear favourites for
     * @param gui    The GUI to close after clearing
     */
    private void clearFavourites(Player player, BaseGui gui) {
        this.manager.clearFavourites(player.getUniqueId());
        this.locale.sendMessage(player, "command-favorite-cleared");
        this.close(gui, player);
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
}