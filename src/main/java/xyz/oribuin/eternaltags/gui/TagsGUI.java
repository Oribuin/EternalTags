package xyz.oribuin.eternaltags.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import dev.triumphteam.gui.guis.ScrollingGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.event.TagEquipEvent;
import xyz.oribuin.eternaltags.event.TagUnequipEvent;
import xyz.oribuin.eternaltags.gui.enums.SortType;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.MenuManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class TagsGUI extends PluginMenu {

    private final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
    private final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

    private final Map<Tag, GuiItem> tagItems = new LinkedHashMap<>(); // Cache the tag items so we don't have to create them every time.

    public TagsGUI(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public void load() {
        super.load();

        this.tagItems.clear(); // Clear the cache so we don't have any old items.
    }

    public void open(@NotNull Player player, @Nullable String keyword) {

        var menuTitle = this.config.getString("gui-settings.title");
        if (menuTitle == null)
            menuTitle = "EternalTags | %page%/%total%";

        var finalMenuTitle = menuTitle;

        var scrollingGui = this.config.getBoolean("gui-settings.scrolling-gui", false);
        var scrollingType = this.match(this.config.getString("gui-settings.scrolling-type"));

        var gui = (scrollingGui && scrollingType != null) ? this.createScrollingGui(player, scrollingType) : this.createPagedGUI(player);

        final CommentedConfigurationSection extraItems = this.config.getConfigurationSection("extra-items");
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
                    gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui)));
                })
                .player(player)
                .place(gui);

        MenuItem.create(this.config)
                .path("previous-page")
                .player(player)
                .action(event -> {
                    gui.previous();
                    gui.updateTitle(this.formatString(player, finalMenuTitle, this.getPagePlaceholders(gui)));
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
                .action(event -> this.rosePlugin.getManager(MenuManager.class).get(FavouritesGUI.class).open(player))
                .place(gui);

        MenuItem.create(this.config)
                .path("search")
                .player(player)
                .action(event -> this.searchTags(player, gui))
                .place(gui);


        gui.open(player);
        var dynamicSpeed = this.config.getInt("gui-settings.dynamic-speed", 3);
        if (this.config.getBoolean("gui-settings.dynamic-gui", false) && dynamicSpeed > 0) {
            this.rosePlugin.getServer().getScheduler().runTaskTimerAsynchronously(this.rosePlugin, task -> {
                if (gui.getInventory().getViewers().isEmpty()) {
                    task.cancel();
                    return;
                }

                this.addTags(gui, player, keyword);
                this.sync(() -> gui.updateTitle(this.formatString(player, this.config.getString("gui-settings.title"), this.getPagePlaceholders(gui))));
            }, 0, dynamicSpeed);
        } else {
            this.async(() -> {
                this.addTags(gui, player, keyword);
                this.sync(() -> gui.updateTitle(this.formatString(player, this.config.getString("gui-settings.title"), this.getPagePlaceholders(gui))));
            });
        }
    }

    /**
     * Clear a player's current active tag
     *
     * @param player The player to clear the tag for
     */
    private void clearTag(Player player) {
        var tag = this.manager.getUserTag(player.getUniqueId());

        var event = new TagUnequipEvent(player, tag);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        this.manager.clearTag(player.getUniqueId());
        this.locale.sendMessage(player, "command-clear-cleared");
    }

    /**
     * Change the results of the GUI based on a keyword
     *
     * @param player The player to change the GUI for
     * @param gui    The GUI to change
     */
    @SuppressWarnings("deprecation")
    private void searchTags(Player player, BaseGui gui) {
        gui.close(player);

        this.locale.sendMessage(player, "command-search-start");
        EternalTags.getEventWaiter().waitForEvent(AsyncPlayerChatEvent.class,
                event -> event.getPlayer().getUniqueId().equals(player.getUniqueId()),
                event -> {
                    event.setCancelled(true);
                    this.sync(() -> this.open(player, event.getMessage()));
                },
                60,
                TimeUnit.SECONDS,
                () -> this.locale.sendMessage(player, "command-search-timeout")
        );
    }

    /**
     * Add the tags to the GUI
     *
     * @param gui     The GUI to add the tags to
     * @param player  The player viewing the GUI
     * @param keyword The keyword to search for
     */
    private void addTags(@NotNull BaseGui gui, @NotNull Player player, @Nullable String keyword) {
        if (gui instanceof PaginatedGui paginatedGui) // Remove all items from the GUI
            paginatedGui.clearPageItems();

        if (gui instanceof ScrollingGui scrollingGui) // Remove all items from the GUI
            scrollingGui.clearPageItems();

        var tagActions = this.getTagActions();
        this.getTags(player, keyword).forEach(tag -> {

            // If the tag is already in the cache, use that instead of creating a new one.
            if (Setting.CACHE_GUI_TAGS.getBoolean() && this.tagItems.containsKey(tag)) {
                gui.addItem(this.tagItems.get(tag));
                return;
            }

            // Create the item for the tag and add it to the cache.
            var item = new GuiItem(this.getTagItem(player, tag), event -> {
                if (!player.hasPermission(tag.getPermission()))
                    return;

                if (tagActions.size() == 0) {
                    if (event.isShiftClick()) {
                        this.toggleFavourite(player, tag);
                        this.addTags(gui, player, keyword);
                        return;
                    }

                    this.setTag(player, tag);
                    gui.close(player);
                    return;
                }

                this.runActions(tagActions, event, this.getTagPlaceholders(tag, player));
            });


            if (Setting.CACHE_GUI_TAGS.getBoolean())
                this.tagItems.put(tag, item);

            gui.addItem(item);
        });

        gui.update();
    }

    /**
     * Get all the tags that should be displayed in the GUI
     *
     * @param player  The player to get the tags for
     * @param keyword The keyword to search for
     * @return A list of tags
     */
    private @NotNull List<Tag> getTags(@NotNull Player player, @Nullable String keyword) {
        var sortType = SortType.match(this.config.getString("gui-settings.sort-type"));
        if (sortType == null)
            sortType = SortType.ALPHABETICAL;

        List<Tag> tags = new ArrayList<>();

        if (this.config.getBoolean("gui-settings.favourite-first")) {
            tags = new ArrayList<>(this.manager.getUsersFavourites(player.getUniqueId()).values());
            sortType.sort(tags);
        }

        var playerTags = new ArrayList<>(this.manager.getPlayerTags(player)); // Get the player's tags
        sortType.sort(playerTags); // Individually sort the player's tags
        tags.addAll(playerTags); // Add all the list of tags

        // We're adding all the remaining tags to the list if the option is enabled
        if (this.config.getBoolean("gui-settings.add-all-tags")) {
            var allTags = new ArrayList<>(this.manager.getCachedTags().values());
            sortType.sort(allTags);
            tags.addAll(allTags);
        }

        // If the keyword is not null, filter the list of tags
        if (keyword != null)
            tags = tags.stream().filter(tag -> tag.getName().toLowerCase().contains(keyword.toLowerCase())).toList();

        return tags.stream().distinct().filter(Objects::nonNull).toList();
    }

    /**
     * Change a player's active tag, and send the message to the player.
     *
     * @param player The player
     * @param tag    The tag
     */
    private void setTag(Player player, Tag tag) {
        var activeTag = this.manager.getUserTag(player);
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


        var message = locale.getLocaleMessage(isFavourite ? "command-favorite-off" : "command-favorite-on");
        this.locale.sendMessage(player, "command-favorite-toggled", StringPlaceholders.builder("tag", this.manager.getDisplayTag(tag, player))
                .addPlaceholder("toggled", message)
                .build());
    }


    @Override
    public Map<String, Object> getDefaultValues() {
        return new LinkedHashMap<>() {{
            this.put("#0", "title - The title of the GUI");
            this.put("#1", "rows - The amount of rows in the GUI");
            this.put("#2", "sort-type - The type of sorting to use in the GUI [ALPHABETICAL, CUSTOM, NONE, RANDOM]");
            this.put("#3", "favourite-first - Whether to show favourite tags first");
            this.put("#4", "add-all-tags - Whether to add all tags to the GUI (Favourite tags will be shown first, Then the player's tags, Then the remaining tags)");
            this.put("#5", "dynamic-gui - Whether the gui should update repeatedly (This is useful for tags that change over time)");
            this.put("#6", "dynamic-speed - The interval in seconds to update the GUI");
            this.put("#7", "description-format - The format of the %description% placeholder");
            this.put("#8", "scrolling-gui - Whether to use a scrolling GUI");
            this.put("#9", "scrolling-type - The type of scrolling for the GUI [HORIZONTAL, VERTICAL] ");

            // General options to configure the rewards.
            this.put("#10", "The general options for the customising itemstacks.");
            this.put("#11", " ");
            this.put("#12", "material - The material of the reward.");
            this.put("#13", "amount - The amount of the reward.");
            this.put("#14", "chance - The chance of the reward.");
            this.put("#15", "name - The name of the reward.");
            this.put("#16", "lore - The lore of the reward.");
            this.put("#17", "glow - Whether the reward item should glow.");
            this.put("#18", "texture - The base64 texture of the reward item (Only for skulls)");
            this.put("#19", "potion-color - The color of the potion reward. (Only for potions)");
            this.put("#20", "model-data - The model data of the reward item. (Requires texture packs)");
            this.put("#21", "owner - The uuid of the player for the reward item (Only for skulls)");
            this.put("#22", "flags - The item flags for the reward item.");
            this.put("#23", "enchants - The enchantments for the reward item.");

            // Commands Options
            this.put("#24", " ");
            this.put("#25", "Icon Actions");
            this.put("#26", " ");
            this.put("#27", "Actions is an optional configuration option that can replace an item's functionality with a new one.");
            this.put("#28", "Available Actions: [BROADCAST, CLOSE, CONSOLE, MESSAGE, PLAYER, SOUND]");
            this.put("#29", "These actions can be defined in the `commands` section of the item, They require a ClickType to be defined.");
            this.put("#30", "Available ClickTypes: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/inventory/ClickType.html");
            this.put("#31", "Here is an example of how to use actions:");
            this.put("#32", "commands:");
            this.put("#33", " LEFT:");
            this.put("#34", "   - '[message] You clicked the left button!'");
            this.put("#35", " RIGHT:");
            this.put("#36", "   - '[message] You clicked the right button!'");
            this.put("#37", " MIDDLE:");
            this.put("#38", "   - '[console] ban %player_name%'");

            // GUI Settings
            this.put("#39", "GUI Settings");
            this.put("gui-settings.title", "EternalTags | %page%/%total%");
            this.put("gui-settings.rows", 5);
            this.put("gui-settings.sort-type", "ALPHABETICAL");
            this.put("gui-settings.favourite-first", true);
            this.put("gui-settings.add-all-tags", false);
            this.put("gui-settings.dynamic-gui", false);
            this.put("gui-settings.dynamic-speed", 3);
            this.put("gui-settings.description-format", " &f| &7");
            this.put("gui-settings.scrolling-gui", false);
            this.put("gui-settings.scrolling-type", "HORIZONTAL");

            // Tag Item
            this.put("#40", "Tag Item - The item that represents each tag in the GUI");
            this.put("tag-item.material", Material.NAME_TAG.name());
            this.put("tag-item.amount", 1);
            this.put("tag-item.name", "%tag%");
            this.put("tag-item.lore", Arrays.asList(
                    "",
                    "&f| #00B4DBLeft-Click &7on this",
                    "&f| &7icon to change your",
                    "&f| &7active tag!",
                    "&f| ",
                    "&f| #00B4DBShift-Click &7to add",
                    "&f| &7this tag to your favorites",
                    ""
            ));
            this.put("tag-item.glow", true);

            // Next Page Item
            this.put("#41", "Next Page Item - Changes the current page to the next page ");
            this.put("next-page.material", Material.PAPER.name());
            this.put("next-page.name", "#00B4DB&lNext Page");
            this.put("next-page.lore", Arrays.asList(
                    "",
                    "&f| #00B4DBLeft-Click &7to change ",
                    "&f| &7to the next page",
                    ""
            ));
            this.put("next-page.slot", 7);

            // Previous Page Item
            this.put("#42", "Previous Page Item - Changes the current page to the previous page");
            this.put("previous-page.material", Material.PAPER.name());
            this.put("previous-page.name", "#00B4DB&lPrevious Page");
            this.put("previous-page.lore", Arrays.asList(
                    "",
                    "&f| #00B4DBLeft-Click &7to change ",
                    "&f| &7to the previous page",
                    ""
            ));
            this.put("previous-page.slot", 1);
            // Clear Tag Item
            this.put("#43", "Clear Tag Item - Clears the player's active tag");
            this.put("clear-tag.enabled", true);
            this.put("clear-tag.material", Material.PLAYER_HEAD.name());
            this.put("clear-tag.name", "#00B4DB&lClear Tag");
            this.put("clear-tag.lore", Arrays.asList(
                    "",
                    " &f| #00B4DBLeft-Click &7to clear your",
                    " &f| &7current active tag.",
                    " &f| &7",
                    " &f| &7Current Tag: #00B4DB%eternaltags_tag%",
                    ""
            ));
            this.put("clear-tag.texture", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTljZGI5YWYzOGNmNDFkYWE1M2JjOGNkYTc2NjVjNTA5NjMyZDE0ZTY3OGYwZjE5ZjI2M2Y0NmU1NDFkOGEzMCJ9fX0=");
            this.put("clear-tag.slot", 3);


            // Search Function
            this.put("#44", "Search Item - Allows the player to search for tags");
            this.put("search.enabled", true);
            this.put("search.material", Material.OAK_SIGN.name());
            this.put("search.name", "#00B4DB&lSearch");
            this.put("search.lore", Arrays.asList(
                    "",
                    " &f| #00B4DBLeft-Click &7to search",
                    " &f| &7for a new tag in the menu.",
                    ""
            ));
            this.put("search.slot", 4);

            // Favourites Tag Item
            this.put("#45", "Favourites Tag Item - Shows the player's favourite tags");
            this.put("favorite-tags.enabled", true);
            this.put("favorite-tags.material", Material.PLAYER_HEAD.name());
            this.put("favorite-tags.name", "#00B4DB&lFavourite Tags");
            this.put("favorite-tags.lore", Arrays.asList(
                    " ",
                    " &f| &#00B4DBLeft-Click &7to view",
                    " &f| &7your #00B4DBfavourite tags&7.",
                    " "
            ));
            this.put("favorite-tags.texture", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDVjNmRjMmJiZjUxYzM2Y2ZjNzcxNDU4NWE2YTU2ODNlZjJiMTRkNDdkOGZmNzE0NjU0YTg5M2Y1ZGE2MjIifX19");
            this.put("favorite-tags.slot", 5);


            this.put("#46", "Extra Items - Allows you to add extra items to the GUI [These are placed in the gui first] ");
            this.put("extra-items.border-item.enabled", true);
            this.put("extra-items.border-item.material", Material.GRAY_STAINED_GLASS_PANE.name());
            this.put("extra-items.border-item.name", " ");
            this.put("extra-items.border-item.slots", List.of("0-8", "36-44"));
        }};
    }

    @Override
    public String getMenuName() {
        return "tags-gui";
    }


    private void sync(Runnable runnable) {
        Bukkit.getScheduler().runTask(this.rosePlugin, runnable);
    }

}
