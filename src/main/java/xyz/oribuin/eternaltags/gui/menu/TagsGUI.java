package xyz.oribuin.eternaltags.gui.menu;

import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import dev.triumphteam.gui.guis.ScrollingGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.action.Action;
import xyz.oribuin.eternaltags.event.TagEquipEvent;
import xyz.oribuin.eternaltags.gui.MenuItem;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.gui.PluginMenu;
import xyz.oribuin.eternaltags.gui.enums.SortType;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class TagsGUI extends PluginMenu {

    private final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
    private final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

    private final Map<Tag, GuiItem> tagItems = new LinkedHashMap<>(); // Cache the tag items so we don't have to create them every time.

    public TagsGUI() {
        super(EternalTags.getInstance());
    }


    @Override
    public void load() {
        super.load();

        this.tagItems.clear(); // Clear the cache so we don't have any old items.
    }

    public void open(@NotNull Player player) {
        this.open(player, null);
    }

    public void open(@NotNull Player player, @Nullable Predicate<Tag> filter) {

        String menuTitle = this.config.getString("gui-settings.title");
        if (menuTitle == null)
            menuTitle = "EternalTags | %page%/%total%";

        String finalMenuTitle = menuTitle;

        boolean scrollingGui = this.config.getBoolean("gui-settings.scrolling-gui", false);
        ScrollType scrollingType = this.match(this.config.getString("gui-settings.scrolling-type"));

        PaginatedGui gui = (scrollingGui && scrollingType != null) ? this.createScrollingGui(player, scrollingType) : this.createPagedGUI(player);

        final CommentedConfigurationSection extraItems = this.config.getConfigurationSection("extra-items");
        if (extraItems != null) {
            for (String key : extraItems.getKeys(false)) {
                MenuItem.create(this.config)
                        .path("extra-items." + key)
                        .player(player)
                        .place(gui);
            }
        }

        this.addNavigationIcons(gui, player, finalMenuTitle); // Add the navigation icons to the GUI.

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
                .path("categories")
                .player(player)
                .action(event -> MenuProvider.get(CategoryGUI.class).open(player))
                .place(gui);

        MenuItem.create(this.config)
                .path("search")
                .player(player)
                .action(event -> this.searchTags(player, gui))
                .place(gui);


        gui.open(player);

        int dynamicSpeed = this.config.getInt("gui-settings.dynamic-speed", 3);
        if (this.config.getBoolean("gui-settings.dynamic-gui", false) && dynamicSpeed > 0) {
            this.rosePlugin.getServer().getScheduler().runTaskTimerAsynchronously(this.rosePlugin, task -> {
                if (gui.getInventory().getViewers().isEmpty()) {
                    task.cancel();
                    return;
                }

                this.addTags(gui, player, filter);

                if (this.reloadTitle())
                    this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));

            }, 0, dynamicSpeed);

            return;
        }

        Runnable task = () -> {
            this.addTags(gui, player, filter);

            if (this.reloadTitle())
                this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
        };

        if (this.addPagesAsynchronously()) this.async(task);
        else task.run();
    }

    /**
     * Navigation icons have their own method since they need to be updated
     * every time the GUI is opened or changed.
     *
     * @param gui    The GUI to add the navigation icons to.
     * @param player The player to add the navigation icons to.
     */
    private void addNavigationIcons(PaginatedGui gui, Player player, String finalMenuTitle) {

        boolean hideIfFirstPage = this.config.getBoolean("previous-page.hide-if-first-page", false); // Hide the previous page icon
        boolean hideIfLastPage = this.config.getBoolean("next-page.hide-if-last-page", false); // Hide the next page icon
        int currentPage = gui.getCurrentPageNum();

        MenuItem.create(this.config)
                .path("next-page")
                .player(player)
                .condition(menuItem -> !hideIfLastPage || currentPage < gui.getPagesNum())
                .action(event -> {
                    gui.next();
                    this.addNavigationIcons(gui, player, finalMenuTitle);
                    this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
                })
                .place(gui);

        MenuItem.create(this.config)
                .path("previous-page")
                .player(player)
                .condition(menuItem -> !hideIfFirstPage || currentPage > 1)
                .action(event -> {
                    gui.previous();
                    this.addNavigationIcons(gui, player, finalMenuTitle);
                    this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
                })
                .place(gui);
    }

    /**
     * Add the tags to the GUI
     *
     * @param gui    The GUI to add the tags to
     * @param player The player viewing the GUI
     * @param filter The filter to apply to the tags
     */
    private void addTags(@NotNull BaseGui gui, @NotNull Player player, @Nullable Predicate<Tag> filter) {
        if (gui instanceof PaginatedGui paginatedGui) // Remove all items from the GUI
            paginatedGui.clearPageItems();

        if (gui instanceof ScrollingGui scrollingGui) // Remove all items from the GUI
            scrollingGui.clearPageItems();

        Map<ClickType, List<Action>> tagActions = this.getTagActions();
        this.getTags(player, filter).forEach(tag -> {

            GuiAction<InventoryClickEvent> action = event -> {
                if (!manager.canUseTag(player, tag))
                    return;

                if (tagActions.size() == 0) {
                    if (event.isShiftClick()) {
                        this.toggleFavourite(player, tag);
                        this.addTags(gui, player, filter);
                        return;
                    }

                    this.setTag(player, tag);
                    gui.close(player);
                    return;
                }

                this.runActions(tagActions, event, this.getTagPlaceholders(tag, player));
            };

            // If the tag is already in the cache, use that instead of creating a new one.
            if (Setting.CACHE_GUI_TAGS.getBoolean() && this.tagItems.containsKey(tag)) {
                GuiItem item = this.tagItems.get(tag);
                item.setAction(action);

                gui.addItem(item);
                return;
            }

            // Create the item for the tag and add it to the cache.
            GuiItem item = new GuiItem(this.getTagItem(player, tag), action);

            if (Setting.CACHE_GUI_TAGS.getBoolean())
                this.tagItems.put(tag, item);

            gui.addItem(item);
        });

        gui.update();
    }

    /**
     * Get all the tags that should be displayed in the GUI
     *
     * @param player The player to get the tags for
     * @param filter The filter to apply to the tags
     * @return A list of tags
     */
    private @NotNull List<Tag> getTags(@NotNull Player player, Predicate<Tag> filter) {
        SortType sortType = SortType.match(this.config.getString("gui-settings.sort-type"));
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

        final TagEquipEvent event = new TagEquipEvent(player, tag);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        this.manager.setTag(player.getUniqueId(), tag);
        this.locale.sendMessage(player, "command-set-changed", StringPlaceholders.single("tag", this.manager.getDisplayTag(tag, player)));
    }


    /**
     * Toggle a player's favourite tag
     *
     * @param player The player
     * @param tag    The tag
     */
    private void toggleFavourite(Player player, Tag tag) {
        boolean isFavourite = this.manager.isFavourite(player.getUniqueId(), tag);

        if (isFavourite)
            this.manager.removeFavourite(player.getUniqueId(), tag);
        else
            this.manager.addFavourite(player.getUniqueId(), tag);


        String message = locale.getLocaleMessage(isFavourite ? "command-favorite-off" : "command-favorite-on");
        this.locale.sendMessage(player, "command-favorite-toggled", StringPlaceholders.builder("tag", this.manager.getDisplayTag(tag, player))
                .addPlaceholder("toggled", message)
                .build());
    }

    @Override
    public String getMenuName() {
        return "tags-gui";
    }

}
