package xyz.oribuin.eternaltags.gui.menu;

import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.gui.MenuItem;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.gui.PluginMenu;
import xyz.oribuin.eternaltags.gui.enums.SortType;
import xyz.oribuin.eternaltags.manager.CategoryManager;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Category;
import xyz.oribuin.eternaltags.obj.CategoryType;
import xyz.oribuin.eternaltags.util.ItemBuilder;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoryGUI extends PluginMenu {

    private final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
    private final CategoryManager categoryManager = this.rosePlugin.getManager(CategoryManager.class);
    private final Map<Category, GuiItem> categoryIcons = new LinkedHashMap<>(); // Cache the tag items, so we don't have to create them every time.

    /**
     * Constructor for CategoryGUI
     */
    public CategoryGUI() {
        super(EternalTags.getInstance());
    }

    /**
     * Load the GUI configuration and allocated slots
     */
    @Override
    public void load() {
        super.load();

        this.categoryIcons.clear();
        this.loadAllocatedSlots();
    }

    /**
     * Open the GUI for a player
     *
     * @param player The player to open the GUI for
     */
    public void open(Player player) {
        if (!categoryManager.isEnabled()) {
            MenuProvider.get(TagsGUI.class).open(player);
            return;
        }

        String menuTitle = this.config.getString("gui-settings.title");
        if (menuTitle == null)
            menuTitle = "Category Menu";

        String finalMenuTitle = menuTitle;


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

        gui.setPageSize(this.allocatedSlots.size());
        gui.open(player);

        Runnable task = () -> {
            this.addCategories(gui, player);

            if (this.reloadTitle())
                this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
        };

        if (this.addPagesAsynchronously()) this.async(task);
        else task.run();
    }

    /**
     * Add functional items to the GUI
     *
     * @param gui    The GUI to add items to
     * @param player The player viewing the GUI
     */
    private void addFunctionalItems(PaginatedGui gui, Player player) {
        String finalMenuTitle = this.config.getString("gui-settings.title", "Category Menu | %page%/%total%");

        MenuItem.create(this.config)
                .path("next-page")
                .player(player)
                .action(event -> {
                    gui.next();
                    this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
                })
                .player(player)
                .place(gui);

        MenuItem.create(this.config)
                .path("previous-page")
                .player(player)
                .action(event -> {
                    gui.previous();
                    this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
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
                .path("favorite-tags")
                .player(player)
                .action((item, event) -> {
                    item.sound((Player) event.getWhoClicked());
                    MenuProvider.get(FavouritesGUI.class).open(player);
                })
                .place(gui);

        MenuItem.create(this.config)
                .path("search")
                .player(player)
                .action((item, event) -> {
                    item.sound((Player) event.getWhoClicked());
                    this.searchTags(player, gui);
                })
                .place(gui);
    }

    /**
     * Add categories to the GUI
     *
     * @param gui    The GUI to add categories to
     * @param player The player viewing the GUI
     */
    private void addCategories(PaginatedGui gui, Player player) {
        gui.clearPageItems();

        TagsGUI tagsGUI = MenuProvider.get(TagsGUI.class);
        if (tagsGUI == null) // This should never happen, but just in case.
            return;

        this.getCategories(player).forEach(category -> {
            String categoryPath = "categories." + category.getId();
            if (this.config.getBoolean(categoryPath + ".hidden", false)) {
                return;
            }

            GuiAction<InventoryClickEvent> action = event -> {
                if (category.getType() == CategoryType.GLOBAL) {
                    tagsGUI.open(player);
                } else {
                    tagsGUI.open(player, tag -> tag.getCategory() != null && tag.getCategory().equalsIgnoreCase(category.getId()));
                }
            };

            if (Setting.CACHE_GUI_CATEGORIES.getBoolean() && this.categoryIcons.containsKey(category)) {
                GuiItem item = this.categoryIcons.get(category);
                item.setAction(action);

                int slotFill = this.config.getInt(categoryPath + ".slot", 1);
                for (int i = 0; i < slotFill; i++) {
                    gui.addItem(item);
                }
                return;
            }

            StringPlaceholders.Builder placeholders = StringPlaceholders.builder()
                    .add("category", category.getDisplayName())
                    .add("total", this.manager.getTagsInCategory(category).size());

            if (this.config.getBoolean("gui-settings.only-unlocked-categories"))
                placeholders.add("unlocked", this.manager.getCategoryTags(category, player).size());

            ItemStack item = TagsUtils.deserialize(this.config, player, categoryPath + ".display-item", placeholders.build());
            if (item == null) {
                item = new ItemBuilder(Material.OAK_SIGN)
                        .name(formatString(player, "#00B4DB" + category.getDisplayName()))
                        .build();
            }

            GuiItem guiItem = new GuiItem(item, action);

            int slotFill = this.config.getInt(categoryPath + ".slot", 1);
            for (int i = 0; i < slotFill; i++) {
                gui.addItem(guiItem);
            }

            if (Setting.CACHE_GUI_CATEGORIES.getBoolean())
                this.categoryIcons.put(category, guiItem);
        });

        gui.update();
    }

    /**
     * Get a list of categories for a player
     *
     * @param player The player to get categories for
     *
     * @return A list of categories
     */
    private List<Category> getCategories(Player player) {
        List<Category> categories = new ArrayList<>(this.categoryManager.getCategories());
        SortType sortType = TagsUtils.getEnum(
                SortType.class,
                this.config.getString("gui-settings.sort-type"),
                SortType.ALPHABETICAL
        );

        categories.removeIf(category -> {
            String categoryPath = "categories." + category.getId();
            if (!this.config.contains(categoryPath) || this.config.getBoolean(categoryPath + ".hidden", false)) {
                return true;
            }
            if (this.config.getBoolean("gui-settings.use-category-permissions", false) && !category.canUse(player)) {
                return true;
            }
            if (this.config.getBoolean("gui-settings.only-unlocked-categories", false)) {
                return category.getType() != CategoryType.GLOBAL && this.manager.getCategoryTags(category, player).isEmpty();
            }
            return false;
        });

        sortType.sortCategories(categories);
        return categories;
    }


    /**
     * Get the name of the menu
     *
     * @return The name of the menu
     */
    @Override
    public String getMenuName() {
        return "category-gui";
    }

}