package dev.oribuin.eternaltags.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.components.ScrollType;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.PaginatedGui;
import dev.triumphteam.gui.guis.ScrollingGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import dev.oribuin.eternaltags.action.Action;
import dev.oribuin.eternaltags.action.PluginAction;
import dev.oribuin.eternaltags.manager.LocaleManager;
import dev.oribuin.eternaltags.manager.TagsManager;
import dev.oribuin.eternaltags.obj.Tag;
import dev.oribuin.eternaltags.util.ItemBuilder;
import dev.oribuin.eternaltags.util.TagsUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class PluginMenu {

    protected final RosePlugin rosePlugin;
    protected CommentedFileConfiguration config;

    public PluginMenu(RosePlugin rosePlugin) {
        this.rosePlugin = rosePlugin;
    }

    /**
     * @return The name of the GUI
     */
    public abstract String getMenuName();

    /**
     * Create the menu file if it doesn't exist and add the default values
     */
    public void load() {
        File menuFile = TagsUtils.createFile(this.rosePlugin, "menus", this.getMenuName() + ".yml");
        this.config = CommentedFileConfiguration.loadConfiguration(menuFile);
    }


    /**
     * Create a paged GUI for the given player
     *
     * @param player The player to create the GUI for
     * @return The created GUI
     */
    protected final @NotNull PaginatedGui createPagedGUI(Player player) {

        int rows = this.config.getInt("gui-settings.rows");
        String preTitle = this.config.getString("gui-settings.pre-title", "EternalTags");

        return Gui.paginated()
                .rows(rows == 0 ? 6 : rows)
                .title(this.format(player, preTitle))
                .disableAllInteractions()
                .create();
    }

    /**
     * Create a GUI for the given player without pages
     *
     * @param player The player to create the GUI for
     * @return The created GUI
     */
    protected final @NotNull Gui createGUI(Player player) {
        int rows = this.config.getInt("gui-settings.rows");
        String preTitle = this.config.getString("gui-settings.pre-title", "EternalTags");

        return Gui.gui()
                .rows(rows == 0 ? 6 : rows)
                .title(this.format(player, preTitle))
                .disableAllInteractions()
                .create();
    }

    /**
     * Scrolling gui for the given player
     *
     * @param player The player to create the GUI for
     * @return The created GUI
     */
    protected final @NotNull ScrollingGui createScrollingGui(Player player, ScrollType scrollType) {

        int rows = this.config.getInt("gui-settings.rows");
        String preTitle = this.config.getString("gui-settings.pre-title", "EternalTags");

        return Gui.scrolling()
                .scrollType(scrollType)
                .rows(rows == 0 ? 6 : rows)
                .pageSize(0)
                .title(this.format(player, preTitle))
                .disableAllInteractions()
                .create();
    }

    /**
     * Create a GUI item for a player, using tag placeholders, Requires different method for the %description% placeholder
     *
     * @param player The player to create the item for
     * @param tag    The tag to create the item for
     * @return The created item
     */
    public ItemStack getTagItem(Player player, Tag tag) {
        StringPlaceholders tagPlaceholders = this.getTagPlaceholders(tag, player);

        ItemStack baseItem = TagsUtils.deserialize(this.config, player, "tag-item", tagPlaceholders);
        if (baseItem == null) return null;

        baseItem = baseItem.clone();

        List<String> configLore = this.config.getStringList("tag-item.lore");
        List<String> lore = new ArrayList<>();

        // im not happy about this but it works
        for (final String line : configLore) {
            if (!line.contains("%description%")) {
                lore.add(TagsUtils.format(player, line, tagPlaceholders));
                continue;
            }

            if (tag.getDescription().isEmpty())
                continue;

            // get the content before the line includes the %description% tag
            String descFormat = config.getString("gui-settings.description-format");
            if (descFormat == null)
                descFormat = line.substring(0, line.indexOf("%description%"));

            lore.add(TagsUtils.format(player, line.replace("%description%", tag.getDescription().get(0))));
            for (int j = 1; j < tag.getDescription().size(); j++) {
                lore.add(descFormat + tag.getDescription().get(j));
            }
        }

        lore = lore.stream()
                .map(line -> TagsUtils.format(player, line, tagPlaceholders))
                .collect(Collectors.toList());

        return new ItemBuilder(baseItem)
                .name(TagsUtils.format(player, this.config.getString("tag-item.name"), tagPlaceholders))
                .lore(lore)
                .build();
    }

    /**
     * Get all the tag icon actions.
     *
     * @return The tag icon actions
     * @since 1.1.7
     */
    protected final @NotNull Map<ClickType, List<Action>> getTagActions() {
        CommentedConfigurationSection customActions = this.config.getConfigurationSection("tag-item.commands");
        if (customActions == null)
            return new HashMap<>();

        Map<ClickType, List<Action>> actions = new HashMap<>();

        for (String key : customActions.getKeys(false)) {
            ClickType clickType = TagsUtils.getEnum(ClickType.class, key.toUpperCase());
            if (clickType == null) {
                this.rosePlugin.getLogger().warning("Invalid click type [" + key + "] in the tag-item.commands section of the [" + this.getMenuName() + "] menu.");
                continue;
            }

            List<Action> actionList = new ArrayList<Action>();
            customActions.getStringList(key)
                    .stream()
                    .map(PluginAction::parse)
                    .filter(Objects::nonNull)
                    .forEach(actionList::add);

            if (actionList.isEmpty())
                continue;

            actions.put(clickType, actionList);
        }

        return actions;
    }

    /**
     * Run the tag icon actions for the given event and placeholder values
     *
     * @param actions      The actions to run
     * @param event        The event to run the actions for
     * @param placeholders The placeholders to use
     */
    public void runActions(@NotNull Map<ClickType, List<Action>> actions, @NotNull InventoryClickEvent event, @NotNull StringPlaceholders placeholders) {
        if (actions.isEmpty())
            return;

        List<Action> newActions = actions.get(event.getClick());
        if (newActions == null || newActions.isEmpty())
            return;

        for (Action action : newActions) {
            action.execute((Player) event.getWhoClicked(), placeholders);
        }
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player The player to format the string for
     * @param text   The string to format
     * @return The formatted string
     */
    protected final Component format(Player player, String text) {
        return Component.text(TagsUtils.format(player, text));
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player       The player to format the string for
     * @param text         The text to format
     * @param placeholders The placeholders to replace
     * @return The formatted string
     */
    protected final Component format(Player player, String text, StringPlaceholders placeholders) {
        return Component.text(TagsUtils.format(player, text, placeholders));
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player The player to format the string for
     * @param text   The text to format
     * @return The formatted string
     */
    protected final String formatString(Player player, String text) {
        return TagsUtils.format(player, text);
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player       The player to format the string for
     * @param text         The text to format
     * @param placeholders The placeholders to replace
     * @return The formatted string
     */
    protected final String formatString(Player player, String text, StringPlaceholders placeholders) {
        return TagsUtils.format(player, text, placeholders);
    }

    /**
     * Get the page placeholders for the gui
     *
     * @param gui The gui
     * @return The page placeholders
     */
    protected StringPlaceholders getPagePlaceholders(PaginatedGui gui) {
        return StringPlaceholders.builder()
                .add("page", gui.getCurrentPageNum())
                .add("total", Math.max(gui.getPagesNum(), 1))
                .add("next", gui.getNextPageNum())
                .add("previous", gui.getPrevPageNum())
                .build();

    }

    /**
     * Get the tag placeholders for the given player
     *
     * @param tag    The tag
     * @param player The player
     * @return The tag placeholders
     */
    protected StringPlaceholders getTagPlaceholders(Tag tag, OfflinePlayer player) {
        return StringPlaceholders.builder()
                .add("tag", this.rosePlugin.getManager(TagsManager.class).getDisplayTag(tag, player))
                .add("tag_stripped", tag.getContent())
                .add("id", tag.getId())
                .add("name", tag.getName())
                .add("description", String.join("\n", tag.getDescription())) // TODO: Delimiter
                .add("permission", tag.getPermission())
                .add("order", tag.getOrder())
                .build();

    }

    /**
     * Change the results of the GUI based on a keyword
     *
     * @param player The player to change the GUI for
     * @param gui    The GUI to change
     */
    public final void searchTags(Player player, BaseGui gui) {
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        this.close(gui, player);

        locale.sendMessage(player, "command-search-start");
        // TODO: Player suggest command
    }

    /**
     * Clear a player's current active tag
     *
     * @param player The player to clear the tag for
     */
    public final void clearTag(Player player) {
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

        manager.clearTag(player.getUniqueId());
        locale.sendMessage(player, "command-clear-cleared");
    }

    /**
     * Close the gui with folia support to prevent UnsupportedOperationException
     *
     * @param gui    The gui to close
     * @param player The player to close the gui for
     */
    public void close(BaseGui gui, Player player) {
        if (TagsUtils.isFolia()) {
            // Recreate the close function since the original one uses BukkitScheduler
            this.sync(player::closeInventory);
            return;
        }

        gui.close(player);
    }

    /**
     * Run a task asynchronously
     *
     * @param runnable The task to run
     */
    public final void async(Runnable runnable) {
        if (TagsUtils.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(this.rosePlugin, scheduledTask -> runnable.run());
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(this.rosePlugin, runnable);
    }

    /**
     * Run a task synchronously
     *
     * @param runnable The task to run
     */
    public final void sync(Runnable runnable) {
        if (TagsUtils.isFolia()) {
            Bukkit.getGlobalRegionScheduler().execute(this.rosePlugin, runnable);
            return;
        }

        Bukkit.getScheduler().runTask(this.rosePlugin, runnable);
    }

    /**
     * @return Whether the title should be updated (Used for page placeholders)
     */
    public boolean reloadTitle() {
        return this.config.getBoolean("gui-settings.update-title", true);
    }

    /**
     * @return Whether the gui should be updated asynchronously
     */
    public boolean addPagesAsynchronously() {
        return this.config.getBoolean("gui-settings.add-pages-asynchronously", true);
    }

}
