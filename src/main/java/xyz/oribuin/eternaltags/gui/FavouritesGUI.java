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
import org.bukkit.event.inventory.ClickType;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.event.TagEquipEvent;
import xyz.oribuin.eternaltags.event.TagUnequipEvent;
import xyz.oribuin.eternaltags.gui.enums.SortType;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.MenuManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FavouritesGUI extends PluginMenu {

    private final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
    private final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

    public FavouritesGUI(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    public void open(@NotNull Player player) {

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
                .path("main-menu")
                .player(player)
                .action(event -> this.rosePlugin.getManager(MenuManager.class).get(TagsGUI.class).open(player, null))
                .place(gui);

        MenuItem.create(this.config)
                .path("reset-favourites")
                .player(player)
                .action(event -> {
                    if (event.getClick() == ClickType.DOUBLE_CLICK)
                        this.clearFavourites(player, gui);
                })
                .place(gui);


        gui.open(player);
        var dynamicSpeed = this.config.getInt("gui-settings.dynamic-speed", 3);
        if (this.config.getBoolean("gui-settings.dynamic-gui", false) && dynamicSpeed > 0) {
            this.rosePlugin.getServer().getScheduler().runTaskTimerAsynchronously(this.rosePlugin, task -> {
                if (gui.getInventory().getViewers().isEmpty()) {
                    task.cancel();
                    return;
                }

                this.addTags(gui, player);
            }, 0, dynamicSpeed);
        } else {
            this.async(() -> this.addTags(gui, player));
        }

        gui.updateTitle(this.formatString(player, this.config.getString("gui-settings.title"), this.getPagePlaceholders(gui)));
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
     * Add the tags to the GUI
     *
     * @param gui    The GUI to add the tags to
     * @param player The player viewing the GUI
     */
    private void addTags(@NotNull BaseGui gui, @NotNull Player player) {
        if (gui instanceof PaginatedGui paginatedGui) // Remove all items from the GUI
            paginatedGui.clearPageItems();

        if (gui instanceof ScrollingGui scrollingGui) // Remove all items from the GUI
            scrollingGui.clearPageItems();


        var tagActions = this.getTagActions();
        this.getTags(player).forEach(tag -> {
            var item = this.getTagItem(player, tag);

            gui.addItem(new GuiItem(item, event -> {
                if (!player.hasPermission(tag.getPermission()))
                    return;

                if (tagActions.size() == 0) {
                    if (event.isShiftClick()) {
                        this.toggleFavourite(player, tag);
                        this.addTags(gui, player);
                        return;
                    }

                    this.setTag(player, tag);
                    gui.close(player);
                    return;
                }

                this.runActions(tagActions, event, this.getTagPlaceholders(tag, player));
            }));
        });

        gui.update();
    }

    /**
     * Get all the tags that should be displayed in the GUI
     *
     * @param player The player to get the tags for
     * @return A list of tags
     */
    private @NotNull List<Tag> getTags(@NotNull Player player) {
        var sortType = SortType.match(this.config.getString("gui-settings.sort-type"));
        if (sortType == null)
            sortType = SortType.ALPHABETICAL;

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

    private void clearFavourites(Player player, BaseGui gui) {
        this.manager.clearFavourites(player.getUniqueId());
        this.locale.sendMessage(player, "command-favorite-cleared");
        gui.close(player);
    }


    @Override
    public Map<String, Object> getDefaultValues() {
        return new LinkedHashMap<>() {{

            this.put("#0", "title - The title of the GUI");
            this.put("#1", "rows - The amount of rows in the GUI");
            this.put("#2", "sort-type - The type of sorting to use in the GUI [ALPHABETICAL, CUSTOM, NONE, RANDOM]");
            this.put("#3", "dynamic-gui - Whether the gui should update repeatedly (This is useful for tags that change over time)");
            this.put("#4", "dynamic-speed - The interval in seconds to update the GUI");
            this.put("#5", "description-format - The format of the %description% placeholder");
            this.put("#6", "scrolling-gui - Whether to use a scrolling GUI");
            this.put("#7", "scrolling-type - The type of scrolling for the GUI [HORIZONTAL, VERTICAL] ");

            // General options to configure the rewards.
            this.put("#8", "The general options for the customising itemstacks.");
            this.put("#9", " ");
            this.put("#10", "material - The material of the reward.");
            this.put("#11", "amount - The amount of the reward.");
            this.put("#12", "chance - The chance of the reward.");
            this.put("#13", "name - The name of the reward.");
            this.put("#14", "lore - The lore of the reward.");
            this.put("#15", "glow - Whether the reward item should glow.");
            this.put("#16", "texture - The base64 texture of the reward item (Only for skulls)");
            this.put("#17", "potion-color - The color of the potion reward. (Only for potions)");
            this.put("#18", "model-data - The model data of the reward item. (Requires texture packs)");
            this.put("#19", "owner - The uuid of the player for the reward item (Only for skulls)");
            this.put("#20", "flags - The item flags for the reward item.");
            this.put("#21", "enchants - The enchantments for the reward item.");

            // Commands Options
            this.put("#22", " ");
            this.put("#23", "Icon Actions");
            this.put("#24", " ");
            this.put("#25", "Actions is an optional configuration option that can replace an item's functionality with a new one.");
            this.put("#26", "Available Actions: [BROADCAST, CLOSE, CONSOLE, MESSAGE, PLAYER, SOUND]");
            this.put("#27", "These actions can be defined in the `commands` section of the item, They require a ClickType to be defined.");
            this.put("#28", "Available ClickTypes: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/inventory/ClickType.html");
            this.put("#29", "Here is an example of how to use actions:");
            this.put("#30", "commands:");
            this.put("#31", " LEFT:");
            this.put("#32", "   - '[message] You clicked the left button!'");
            this.put("#33", " RIGHT:");
            this.put("#34", "   - '[message] You clicked the right button!'");
            this.put("#35", " MIDDLE:");
            this.put("#36", "   - '[console] ban %player_name%'");

            // GUI Settings
            this.put("#37", "GUI Settings");
            this.put("gui-settings.title", "Favourite Tags | %page%/%total%");
            this.put("gui-settings.rows", 5);
            this.put("gui-settings.sort-type", "ALPHABETICAL");
            this.put("gui-settings.dynamic-gui", false);
            this.put("gui-settings.dynamic-speed", 3);
            this.put("gui-settings.description-format", " &f| &7");
            this.put("gui-settings.scrolling-gui", false);
            this.put("gui-settings.scrolling-type", "HORIZONTAL");

            // Tag Item
            this.put("#38", "Tag Item - The item that represents each tag in the GUI");
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
            this.put("#39", "Next Page Item - Changes the current page to the next page");
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
            this.put("#40", "Previous Page Item - Changes the current page to the previous page");
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
            this.put("#41", "Clear Tag Item - Clears the player's active tag");
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

            // Favourites Tag Item
            this.put("#42", "Main Menu Item - Goes back to the main menu");
            this.put("main-menu.enabled", true);
            this.put("main-menu.material", Material.BARRIER.name());
            this.put("main-menu.name", "#00B4DB&lMain Menu");
            this.put("main-menu.lore", Arrays.asList(
                    " ",
                    " &f| &#00B4DBLeft-Click &7to go back",
                    " &f| &7to the #00B4DBmain-menu.",
                    " "
            ));
            this.put("main-menu.slot", 4);

            this.put("#43", "Reset Favourites - Allows the player to remove all of their favourite tags");
            this.put("reset-favourites.enabled", true);
            this.put("reset-favourites.material", Material.OAK_SIGN.name());
            this.put("reset-favourites.name", "#00B4DB&lReset Favourites");
            this.put("reset-favourites.lore", Arrays.asList(
                    "",
                    " &f| #00B4DBDouble-Click &7to reset",
                    " &f| &7all of your favourite tags.",
                    " &f| &7",
                    " &f| &7You #00B4DBCANNOT &7undo this action.",
                    ""
            ));
            this.put("reset-favourites.slot", 5);

            this.put("#44", "Extra Items - Allows you to add extra items to the GUI [These are placed in the gui first]");
            this.put("extra-items.border-item.enabled", true);
            this.put("extra-items.border-item.material", Material.GRAY_STAINED_GLASS_PANE.name());
            this.put("extra-items.border-item.name", " ");
            this.put("extra-items.border-item.slots", List.of("0-8", "36-44"));
        }};
    }

    @Override
    public String getMenuName() {
        return "favorites-gui";
    }


}
