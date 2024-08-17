package xyz.oribuin.eternaltags.gui.menu;

import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
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
    private final List<Integer> tagSlots = new ArrayList<>(); // Store the slots for tag items

    public TagsGUI() {
        super(EternalTags.getInstance());
    }

    @Override
    public void load() {
        super.load();

        this.tagItems.clear(); // Clear the cache, so we don't have any old items.
        this.loadTagSlots(); // Load the tag slots from the configuration
    }

    private void loadTagSlots() {
        this.tagSlots.clear();
        List<String> slotsConfig = this.config.getStringList("tag-item.slots");
        if (slotsConfig.isEmpty()) {
            // If no slots are specified, use all available slots
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

    public void open(@NotNull Player player) {
        this.open(player, null);
    }

    public void open(@NotNull Player player, @Nullable Predicate<Tag> filter) {
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

        // Set the page size and calculate the number of pages
        int pageSize = this.tagSlots.size();
        gui.setPageSize(pageSize);

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

        // Add tags to the GUI
        this.addTagsToGui(gui, player, filter);

        this.sync(() -> gui.open(player));

        if (this.reloadTitle()) {
            this.sync(() -> gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui))));
        }

        this.addNavigationIcons(gui, player, finalMenuTitle);
    }

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
    }

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

    private void addTags(@NotNull BaseGui gui, @NotNull Player player, @Nullable Predicate<Tag> filter) {
        if (gui instanceof PaginatedGui paginatedGui) {
            paginatedGui.clearPageItems();

            Map<ClickType, List<Action>> tagActions = this.getTagActions();
            Sound tagSound = TagsUtils.getEnum(Sound.class, this.config.getString("tag-item.sound", ""));

            List<Tag> tags = this.getTags(player, filter);
            int slotsPerPage = this.tagSlots.size();
            int totalPages = paginatedGui.getPagesNum();

            for (int i = 0; i < tags.size(); i++) {
                int page = i / slotsPerPage;
                if (page >= totalPages) break; // Stop if we've exceeded the total number of pages

                int slotIndex = i % slotsPerPage;
                if (slotIndex >= this.tagSlots.size()) continue; // Skip if we've run out of configured slots

                int slot = this.tagSlots.get(slotIndex);
                Tag tag = tags.get(i);

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

                try {
                    paginatedGui.setItem(page, slot, item);
                } catch (Exception e) {
                    this.rosePlugin.getLogger().warning("Failed to set item in GUI. Page: " + page + ", Slot: " + slot);
                }
            }

            paginatedGui.update();
        }
    }

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
                this.addTags(gui, player, filter);
                return;
            }

            this.setTag(player, tag);
            gui.close(player);
        };
    }

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
     * Toggle a player's favourite tag
     *
     * @param player The player
     * @param tag    The tag
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

    @Override
    public String getMenuName() {
        return "tags-gui";
    }
}