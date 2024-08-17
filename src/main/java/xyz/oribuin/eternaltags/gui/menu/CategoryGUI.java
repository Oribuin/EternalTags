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
import org.bukkit.event.inventory.InventoryClickEvent;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CategoryGUI extends PluginMenu {

    private final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
    private final CategoryManager categoryManager = this.rosePlugin.getManager(CategoryManager.class);
    private final Map<Category, GuiItem> categoryIcons = new LinkedHashMap<>(); // Cache the tag items, so we don't have to create them every time.

    public CategoryGUI() {
        super(EternalTags.getInstance());
    }

    @Override
    public void load() {
        super.load();

        this.categoryIcons.clear();
    }

    public void open(@NotNull Player player) {
        // Check if categories are enabled.
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

        CommentedConfigurationSection extraItems = this.config.getConfigurationSection("extra-items");
        if (extraItems != null) {
            for (String key : extraItems.getKeys(false)) {
                MenuItem.create(this.config)
                        .path("extra-items." + key)
                        .player(player)
                        .place(gui);
            }
        }

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
                .path("favorite-tags")
                .player(player)
                .action(event -> MenuProvider.get(FavouritesGUI.class).open(player))
                .place(gui);

        MenuItem.create(this.config)
                .path("search")
                .player(player)
                .action(event -> this.searchTags(player, gui))
                .place(gui);

        MenuItem.create(this.config)
                .path("main-menu")
                .player(player)
                .action(event -> MenuProvider.get(TagsGUI.class).open(player, null))
                .place(gui);

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
     * Add the categories to the gui.
     *
     * @param gui    The gui to add the categories to.
     * @param player The player to add the categories for.
     */
    public void addCategories(@NotNull BaseGui gui, @NotNull Player player) {
        if (gui instanceof PaginatedGui paginated)
            paginated.clearPageItems();

        TagsGUI tagsGUI = MenuProvider.get(TagsGUI.class);
        if (tagsGUI == null)
            return;

        this.getCategories(player).forEach(category -> {
            String categoryPath = "categories." + category.getId();

            // Skip categories that are not in the config or are set to hidden
            if (!this.config.contains(categoryPath) || this.config.getBoolean(categoryPath + ".hidden", false)) {
                return;
            }

            GuiAction<InventoryClickEvent> action = event -> {
                if (category.getType() == CategoryType.GLOBAL) {
                    tagsGUI.open(player);
                    return;
                }

                tagsGUI.open(player, tag -> tag.getCategory() != null && tag.getCategory().equalsIgnoreCase(category.getId()));
            };

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

            // Get the slots for this category
            List<Integer> slots = this.getCategorySlots(category);

            // If no slots are specified, add to the first empty slot
            if (slots.isEmpty()) {
                gui.addItem(guiItem);
            } else {
                // Add the item to all specified slots
                for (int slot : slots) {
                    gui.setItem(slot, guiItem);
                }
            }

            if (Setting.CACHE_GUI_CATEGORIES.getBoolean())
                this.categoryIcons.put(category, guiItem);
        });
    }

    private List<Integer> getCategorySlots(Category category) {
        List<Integer> slots = new ArrayList<>();

        // Try to get slots as a list first
        List<String> slotsConfig = this.config.getStringList("categories." + category.getId() + ".slots");

        // If the list is empty, try to get it as a single string (for backwards compatibility)
        if (slotsConfig.isEmpty()) {
            String slotString = this.config.getString("categories." + category.getId() + ".slots");
            if (slotString != null && !slotString.isEmpty()) {
                slotsConfig = List.of(slotString);
            }
        }

        // If we still don't have any slots, check for a single 'slot' entry
        if (slotsConfig.isEmpty()) {
            int singleSlot = this.config.getInt("categories." + category.getId() + ".slot", -1);
            if (singleSlot != -1) {
                return List.of(singleSlot);
            }
        }

        // Parse the slot ranges
        for (String slotRange : slotsConfig) {
            slots.addAll(TagsUtils.parseList(List.of(slotRange)));
        }

        return slots;
    }

    /**
     * Get a list of categories
     *
     * @param player The player to get the categories for
     * @return A list of categories
     */
    public List<Category> getCategories(@NotNull Player player) {
        List<Category> categories = new ArrayList<>(this.categoryManager.getCategories());
        SortType sortType = TagsUtils.getEnum(
                SortType.class,
                this.config.getString("gui-settings.sort-type"),
                SortType.ALPHABETICAL
        );

        // Remove categories that are not defined in the config or are set to hidden
        categories.removeIf(category -> {
            String categoryPath = "categories." + category.getId();
            return !this.config.contains(categoryPath) || this.config.getBoolean(categoryPath + ".hidden", false);
        });

        if (this.config.getBoolean("gui-settings.use-category-permissions", false)) {
            categories.removeIf(category -> !category.canUse(player));
        }

        if (this.config.getBoolean("gui-settings.only-unlocked-categories", false)) {
            categories.removeIf(category -> {
                if (category.getType() == CategoryType.GLOBAL) return false;
                return this.manager.getCategoryTags(category, player).isEmpty();
            });
        }

        sortType.sortCategories(categories);

        return categories;
    }

    @Override
    public String getMenuName() {
        return "category-gui";
    }

}
