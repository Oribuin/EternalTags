package xyz.oribuin.eternaltags.gui.menu;

import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

import java.util.*;
import java.util.function.Predicate;

public class TagsGUI extends PluginMenu {

    private final TagsManager manager;
    private final LocaleManager locale;
    private final Map<String, ItemStack> tagItems;
    private final List<Integer> tagSlots;

    /**
     * Constructor for TagsGUI
     */
    public TagsGUI() {
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
        this.open(player, null);
    }

    /**
     * Open the GUI for a player with an optional filter
     *
     * @param player The player to open the GUI for
     * @param filter An optional filter for the tags
     */
    public void open(@NotNull Player player, @Nullable Predicate<Tag> filter) {
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
        this.addTagsToGui(gui, player, filter);

        this.sync(() -> gui.open(player));

        if (this.reloadTitle()) {
            this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
        }

        this.addNavigationIcons(gui, player, finalMenuTitle);
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
                        .action((item, event) -> item.sound((Player) event.getWhoClicked()))
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
        MenuItem.create(this.config)
                .path("clear-tag")
                .player(player)
                .action((item, event) -> {
                    item.sound((Player) event.getWhoClicked());
                    this.clearTag(player);
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
                .path("categories")
                .player(player)
                .action((item, event) -> {
                    item.sound((Player) event.getWhoClicked());
                    MenuProvider.get(CategoryGUI.class).open(player);
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

        MenuItem.create(this.config)
                .path("main-menu")
                .player(player)
                .action((item, event) -> {
                    item.sound((Player) event.getWhoClicked());
                    if (Setting.OPEN_CATEGORY_GUI_FIRST.getBoolean()) {
                        MenuProvider.get(CategoryGUI.class).open(player);
                    } else {
                        MenuProvider.get(TagsGUI.class).open(player, null);
                    }
                })
                .place(gui);
    }

    /**
     * Add tags to the GUI
     *
     * @param gui    The GUI to add tags to
     * @param player The player viewing the GUI
     * @param filter An optional filter for the tags
     */
    private void addTagsToGui(PaginatedGui gui, Player player, Predicate<Tag> filter) {
        List<Tag> tags = this.getTags(player, filter);
        Map<ClickType, List<Action>> tagActions = this.getTagActions();
        Sound tagSound = TagsUtils.getEnum(Sound.class, this.config.getString("tag-item.sound", ""));

        for (Tag tag : tags) {
            GuiAction<InventoryClickEvent> action = createTagAction(player, gui, tag, tagActions, tagSound, filter);

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
     * Add navigation icons to the GUI
     *
     * @param gui            The GUI to add navigation icons to
     * @param player         The player viewing the GUI
     * @param finalMenuTitle The title of the GUI
     */
    private void addNavigationIcons(PaginatedGui gui, Player player, String finalMenuTitle) {
        MenuItem.create(this.config)
                .path("next-page")
                .player(player)
                .action((item, event) -> {
                    if (gui.next()) {
                        item.sound((Player) event.getWhoClicked());
                        this.addNavigationIcons(gui, player, finalMenuTitle);
                        this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
                    }
                })
                .place(gui);

        MenuItem.create(this.config)
                .path("previous-page")
                .player(player)
                .action((item, event) -> {
                    if (gui.previous()) {
                        item.sound((Player) event.getWhoClicked());
                        this.addNavigationIcons(gui, player, finalMenuTitle);
                        this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
                    }
                })
                .place(gui);

        gui.update();
    }

    /**
     * Create an action for a tag item
     *
     * @param player     The player viewing the GUI
     * @param gui        The GUI containing the tag
     * @param tag        The tag
     * @param tagActions The actions for the tag
     * @param tagSound   The sound to play when clicking the tag
     * @param filter     The filter applied to the tags
     * @return The action for the tag item
     */
    private GuiAction<InventoryClickEvent> createTagAction(Player player, BaseGui gui, Tag tag, Map<ClickType, List<Action>> tagActions, Sound tagSound, Predicate<Tag> filter) {
        return event -> {
            if (!this.manager.canUseTag(player, tag)) {
                this.locale.sendMessage(player, "no-permission");
                gui.close(player);
                return;
            }

            if (!tagActions.isEmpty()) {
                this.runActions(tagActions, event, this.getTagPlaceholders(tag, player));
                return;
            }

            if (tagSound != null) {
                player.playSound(player.getLocation(), tagSound, 75, 1);
            }

            if (event.isShiftClick()) {
                this.toggleFavourite(player, tag);
                if (gui instanceof PaginatedGui paginatedGui) {
                    this.addTagsToGui(paginatedGui, player, filter);
                }
                return;
            }

            this.setTag(player, tag);
            gui.close(player);
        };
    }

    /**
     * Get the list of tags for a player
     *
     * @param player The player to get tags for
     * @param filter An optional filter for the tags
     * @return The list of tags
     */
    private @NotNull List<Tag> getTags(@NotNull Player player, Predicate<Tag> filter) {
        SortType sortType = TagsUtils.getEnum(SortType.class, this.config.getString("gui-settings.sort-type"));
        if (sortType == null)
            sortType = SortType.ALPHABETICAL;

        List<Tag> tags = new ArrayList<>();

        if (this.config.getBoolean("gui-settings.favourite-first")) {
            tags = new ArrayList<>(this.manager.getUsersFavourites(player.getUniqueId()).values());
            sortType.sort(tags);
        }

        List<Tag> playerTags = new ArrayList<>(this.manager.getPlayerTags(player));
        sortType.sort(playerTags);
        tags.addAll(playerTags);

        if (this.config.getBoolean("gui-settings.add-all-tags")) {
            List<Tag> allTags = new ArrayList<>(this.manager.getCachedTags().values());
            sortType.sort(allTags);
            tags.addAll(allTags);
        }

        if (filter != null)
            tags = tags.stream().filter(filter).toList();

        return tags.stream().distinct().filter(Objects::nonNull).toList();
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
    private void toggleFavourite(@NotNull Player player, Tag tag) {
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
     * Get the name of the menu
     *
     * @return The name of the menu
     */
    @Override
    public String getMenuName() {
        return "tags-gui";
    }
}