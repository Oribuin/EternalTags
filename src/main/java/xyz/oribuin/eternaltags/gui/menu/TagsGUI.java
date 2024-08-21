package xyz.oribuin.eternaltags.gui.menu;

import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.Sound;
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
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class TagsGUI extends PluginMenu {

    private final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
    private final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

    private final Map<String, ItemStack> tagItems = new LinkedHashMap<>(); // Cache the tag items, so we don't have to create them every time.

    /**
     * Constructor for TagsGUI
     */
    public TagsGUI() {
        super(EternalTags.getInstance());
    }

    /**
     * Load the GUI configuration and tag slots
     */
    @Override
    public void load() {
        super.load();

        this.tagItems.clear(); // Clear the cache, so we don't have any old items.
        this.loadTagSlots(); // load specified slots from configuration
    }

    /**
     * Open the GUI for a player
     *
     * @param player The player to open the GUI for
     */
    public void open(Player player) {
        this.open(player, null);
    }

    /**
     * Open the GUI for a player with an optional filter
     *
     * @param player The player to open the GUI for
     * @param filter An optional filter for the tags
     */
    public void open(Player player, Predicate<Tag> filter) {

        String menuTitle = this.config.getString("gui-settings.title");
        if (menuTitle == null)
            menuTitle = "EternalTags | %page%/%total%";
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

        this.sync(() -> gui.open(player));

        Runnable task = () -> {
            gui.setPageSize(this.tagSlots.size());
            this.addTags(gui, player, filter);

            if (this.reloadTitle())
                this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
        };

        if (this.addPagesAsynchronously())
            this.async(task);
        else
            task.run();

        this.addNavigationIcons(gui, player, finalMenuTitle);
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
     */
    private void addTags(BaseGui gui, Player player, Predicate<Tag> filter) {
        if (gui instanceof PaginatedGui paginatedGui) // Remove all items from the GUI
            paginatedGui.clearPageItems();

        Map<ClickType, List<Action>> tagActions = this.getTagActions();
        Sound tagSound = TagsUtils.getEnum(Sound.class, this.config.getString("tag-item.sound", ""));

        this.getTags(player, filter).forEach(tag -> {
            GuiAction<InventoryClickEvent> action = event -> {
                // Check if the player has permission to use the tag
                if (!this.manager.canUseTag(player, tag)) {
                    this.locale.sendMessage(player, "no-permission");
                    gui.close(player);
                    return;
                }

                // Run the actions for the tag
                if (!tagActions.isEmpty()) {
                    this.runActions(tagActions, event, this.getTagPlaceholders(tag, player));
                    return;
                }

                // Play the sound if it's not null
                if (tagSound != null) {
                    player.playSound(player.getLocation(), tagSound, 75, 1);
                }

                // If the player is shift clicking, toggle the favourite status of the tag
                if (event.isShiftClick()) {
                    this.toggleFavourite(player, tag);
                    this.addTags(gui, player, filter);
                    return;
                }

                // Set the tag if the player is not shift clicking
                this.setTag(player, tag);
                gui.close(player);
            };

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
        });

        gui.update();
    }


    /**
     * Get the list of tags for a player
     *
     * @param player The player to get tags for
     * @param filter An optional filter for the tags
     *
     * @return The list of tags
     */
    private List<Tag> getTags(Player player, Predicate<Tag> filter) {
        SortType sortType = TagsUtils.getEnum(SortType.class, this.config.getString("gui-settings.sort-type"));
        if (sortType == null)
            sortType = SortType.ALPHABETICAL;

        List<Tag> tags = new ArrayList<>();

        if (this.config.getBoolean("gui-settings.favourite-first")) {
            tags = new ArrayList<>(this.manager.getUsersFavourites(player.getUniqueId()).values());
            sortType.sort(tags);
        }

        List<Tag> playerTags = new ArrayList<>(this.manager.getPlayerTags(player)); // Get the player's tags
        sortType.sort(playerTags); // Individually sort the player's tags
        tags.addAll(playerTags); // Add all the list of tags

        // We're adding all the remaining tags to the list if the option is enabled
        if (this.config.getBoolean("gui-settings.add-all-tags")) {
            List<Tag> allTags = new ArrayList<>(this.manager.getCachedTags().values());
            sortType.sort(allTags);
            tags.addAll(allTags);
        }

        // If the keyword is not null, filter the list of tags
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
     * Get the name of the menu
     *
     * @return The name of the menu
     */
    @Override
    public String getMenuName() {
        return "tags-gui";
    }
}