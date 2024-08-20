package xyz.oribuin.eternaltags.gui.menu;

import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
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

import java.util.*;

public class CategoryGUI extends PluginMenu {

    private final TagsManager manager;
    private final CategoryManager categoryManager;
    private final Map<Category, GuiItem> categoryIcons;
    private List<Integer> allocatedSlots;

    /**
     * Constructor for CategoryGUI
     */
    public CategoryGUI() {
        super(EternalTags.getInstance());
        this.manager = this.rosePlugin.getManager(TagsManager.class);
        this.categoryManager = this.rosePlugin.getManager(CategoryManager.class);
        this.categoryIcons = new LinkedHashMap<>();
        this.allocatedSlots = new ArrayList<>();
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
     * Load the allocated slots from the configuration
     */
    private void loadAllocatedSlots() {
        this.allocatedSlots = new ArrayList<>();
        if (this.config.contains("gui-settings.allocated-slots")) {
            List<String> slotsConfig = this.config.getStringList("gui-settings.allocated-slots");
            for (String slotConfig : slotsConfig) {
                if (slotConfig.contains("-")) {
                    String[] range = slotConfig.split("-");
                    int start = Integer.parseInt(range[0]);
                    int end = Integer.parseInt(range[1]);
                    for (int i = start; i <= end; i++) {
                        this.allocatedSlots.add(i);
                    }
                } else {
                    this.allocatedSlots.add(Integer.parseInt(slotConfig));
                }
            }
        } else {
            // If allocated-slots is not defined, use all available slots
            int rows = this.config.getInt("gui-settings.rows", 6);
            for (int i = 0; i < rows * 9; i++) {
                this.allocatedSlots.add(i);
            }
        }
    }

    /**
     * Open the GUI for a player
     *
     * @param player The player to open the GUI for
     */
    public void open(@NotNull Player player) {
        if (!categoryManager.isEnabled()) {
            MenuProvider.get(TagsGUI.class).open(player);
            return;
        }

        String finalMenuTitle = this.config.getString("gui-settings.title", "Category Menu | %page%/%total%");

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

        this.addCategoriesToGui(gui, player);

        gui.open(player);

        if (this.reloadTitle()) {
            this.updateTitle(gui, player, finalMenuTitle);
        }
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
            if (!this.allocatedSlots.contains(i)) {
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
        String finalMenuTitle = this.config.getString("gui-settings.title", "Category Menu | %page%/%total%");

        MenuItem.create(this.config)
                .path("next-page")
                .player(player)
                .action(event -> {
                    if (gui.next()) {
                        this.updateTitle(gui, player, finalMenuTitle);
                    }
                })
                .place(gui);

        MenuItem.create(this.config)
                .path("previous-page")
                .player(player)
                .action(event -> {
                    if (gui.previous()) {
                        this.updateTitle(gui, player, finalMenuTitle);
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
    private void addCategoriesToGui(@NotNull PaginatedGui gui, @NotNull Player player) {
        List<Category> categories = this.getCategories(player);
        TagsGUI tagsGUI = MenuProvider.get(TagsGUI.class);
        if (tagsGUI == null) return;

        for (Category category : categories) {
            String categoryPath = "categories." + category.getId();
            if (!this.config.contains(categoryPath) || this.config.getBoolean(categoryPath + ".hidden", false)) {
                continue;
            }

            GuiItem guiItem = createCategoryItem(player, category, tagsGUI);
            int slotFill = this.config.getInt(categoryPath + ".slot-fill", 1); // Default to 1 if not specified

            for (int i = 0; i < slotFill; i++) {
                gui.addItem(guiItem);
            }

            if (Setting.CACHE_GUI_CATEGORIES.getBoolean())
                this.categoryIcons.put(category, guiItem);
        }

        gui.update();
    }

    /**
     * Create a GUI item for a category
     *
     * @param player   The player viewing the GUI
     * @param category The category
     * @param tagsGUI  The TagsGUI instance
     * @return The created GUI item
     */
    private GuiItem createCategoryItem(Player player, Category category, TagsGUI tagsGUI) {
        String categoryPath = "categories." + category.getId();

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

        return new GuiItem(item, event -> {
            if (category.getType() == CategoryType.GLOBAL) {
                tagsGUI.open(player);
            } else {
                tagsGUI.open(player, tag -> tag.getCategory() != null && tag.getCategory().equalsIgnoreCase(category.getId()));
            }
        });
    }

    /**
     * Get a list of categories for a player
     *
     * @param player The player to get categories for
     * @return A list of categories
     */
    private List<Category> getCategories(@NotNull Player player) {
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
     * Update the title of the GUI
     *
     * @param gui         The GUI to update
     * @param player      The player viewing the GUI
     * @param titleFormat The format of the title
     */
    private void updateTitle(PaginatedGui gui, Player player, String titleFormat) {
        StringPlaceholders placeholders = StringPlaceholders.builder()
                .add("page", gui.getCurrentPageNum())
                .add("total", gui.getPagesNum())
                .build();
        String title = this.formatString(player, titleFormat, placeholders);
        this.sync(() -> gui.updateTitle(title));
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