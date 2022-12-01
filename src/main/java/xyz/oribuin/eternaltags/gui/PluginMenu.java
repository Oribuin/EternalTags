package xyz.oribuin.eternaltags.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.components.ScrollType;
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
import xyz.oribuin.eternaltags.action.Action;
import xyz.oribuin.eternaltags.action.PluginAction;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.util.ItemBuilder;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.io.File;
import java.io.IOException;
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
     * Get the default config values for the GUI
     *
     * @return The default config values
     */
    public abstract Map<String, Object> getDefaultValues();

    /**
     * @return The name of the GUI
     */
    public abstract String getMenuName();

    /**
     * Create the menu file if it doesn't exist and add the default values
     */
    public final void load() {
        final var folder = new File(this.rosePlugin.getDataFolder(), "menus");
        var newFile = false;
        if (!folder.exists()) {
            folder.mkdirs();
        }

        final var file = new File(folder, this.getMenuName() + ".yml");
        try {
            if (!file.exists()) {
                file.createNewFile();
                newFile = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.config = CommentedFileConfiguration.loadConfiguration(file);

        // Move the old configs into a different folder.
        if (config.get("menu-name") != null && !newFile) {
            var oldGuis = new File(folder, "old-guis");
            if (!oldGuis.exists()) {
                oldGuis.mkdirs();
            }

            this.rosePlugin.getLogger().warning("We have detected that you are using an old version of EternalTags GUIs. We will now move file [" + file.getName() + "] to the old-guis folder and create a new one for you.");
            file.renameTo(new File(oldGuis, file.getName()));
            this.load();
            return;
        }

        if (newFile) {
            this.getDefaultValues().forEach((path, object) -> {
                if (path.startsWith("#")) {
                    this.config.addPathedComments(path, (String) object);
                } else {
                    this.config.set(path, object);
                }
            });
        }


        this.config.save();
    }


    /**
     * Create a paged GUI for the given player
     *
     * @param player The player to create the GUI for
     * @return The created GUI
     */
    protected final @NotNull PaginatedGui createPagedGUI(Player player) {

        final var rows = this.config.getInt("gui-settings.rows");
        final var title = this.config.getString("gui-settings.title");

        return Gui.paginated()
                .rows(rows == 0 ? 6 : rows)
                .title(this.format(player, title == null ? "Missing Title" : title))
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
        final var rows = this.config.getInt("gui-settings.rows");
        final var title = this.config.getString("gui-settings.title");

        return Gui.gui()
                .rows(rows == 0 ? 6 : rows)
                .title(this.format(player, title == null ? "Missing Title" : title))
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

        final var rows = this.config.getInt("gui-settings.rows");
        final var title = this.config.getString("gui-settings.title");
        return Gui.scrolling()
                .scrollType(scrollType)
                .rows(rows == 0 ? 6 : rows)
                .pageSize(0)
                .title(this.format(player, title == null ? "Missing Title" : title))
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
        var baseItem = TagsUtils.getItemStack(this.config, "tag-item", player, this.getTagPlaceholders(tag, player));

        var configLore = this.config.getStringList("tag-item.lore");
        List<String> lore = new ArrayList<>();

        // im not happy about this but it works
        for (final String line : configLore) {
            if (!line.contains("%description%")) {
                lore.add(TagsUtils.format(player, line, this.getTagPlaceholders(tag, player)));
                continue;
            }

            if (tag.getDescription().isEmpty())
                continue;

            // get the content before the line includes the %description% tag
            String before = line.substring(0, line.indexOf("%description%"));

            lore.add(TagsUtils.format(player, line.replace("%description%", tag.getDescription().get(0))));
            for (int j = 1; j < tag.getDescription().size(); j++) {
                lore.add(before + tag.getDescription().get(j));
            }
        }

        lore = lore.stream().map(line -> TagsUtils.format(player, line, this.getTagPlaceholders(tag, player))).collect(Collectors.toList());

        var item = new ItemBuilder(baseItem).setLore(lore);
        if (tag.getIcon() != null) {
            item.setMaterial(tag.getIcon());
        }


        return new ItemBuilder(baseItem)
                .setLore(lore)
                .create();
    }

    /**
     * Get all the tag icon actions.
     *
     * @return The tag icon actions
     * @since 1.1.7
     */
    protected final Map<ClickType, List<Action>> getTagActions() {
        var customActions = this.config.getConfigurationSection("tag-item.commands");
        if (customActions == null)
            return null;

        var actions = new HashMap<ClickType, List<Action>>();

        for (var key : customActions.getKeys(false)) {
            var clickType = TagsUtils.getEnum(ClickType.class, key.toUpperCase());
            if (clickType == null) {
                this.rosePlugin.getLogger().warning("Invalid click type [" + key + "] in the tag-item.commands section of the [" + this.getMenuName() + "] menu.");
                continue;
            }

            var actionList = new ArrayList<Action>();
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
     * @return The true if the actions were run, false if not
     */
    public boolean runActions(@NotNull Map<ClickType, List<Action>> actions, @NotNull InventoryClickEvent event, @NotNull StringPlaceholders placeholders) {

        if (actions.isEmpty())
            return false;

        actions.forEach((clickType, x) -> {
            if (event.getClick() == clickType) {
                x.forEach(action -> action.execute((Player) event.getWhoClicked(), placeholders));
            }
        });

        return true;
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
                .addPlaceholder("page", gui.getCurrentPageNum())
                .addPlaceholder("total", Math.max(gui.getPagesNum(), 1))
                .addPlaceholder("next", gui.getNextPageNum())
                .addPlaceholder("previous", gui.getPrevPageNum())
                .build();

    }

    /**
     * Get the page placeholders for the gui
     *
     * @param gui The gui
     * @return The page placeholders
     */
    public StringPlaceholders getPagePlaceholders(ScrollingGui gui) {
        return StringPlaceholders.builder()
                .addPlaceholder("page", gui.getCurrentPageNum())
                .addPlaceholder("total", Math.max(gui.getPagesNum(), 1))
                .addPlaceholder("next", gui.getNextPageNum())
                .addPlaceholder("previous", gui.getPrevPageNum())
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
                .addPlaceholder("tag", this.rosePlugin.getManager(TagsManager.class).getDisplayTag(tag, player))
                .addPlaceholder("id", tag.getId())
                .addPlaceholder("name", tag.getName())
                .addPlaceholder("description", String.join(Setting.DESCRIPTION_DELIMITER.getString(), tag.getDescription()))
                .addPlaceholder("permission", tag.getPermission())
                .addPlaceholder("order", tag.getOrder())
                .build();

    }

    public final void async(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(this.rosePlugin, runnable);
    }

    public ScrollType match(String name) {
        for (ScrollType scrollType : ScrollType.values()) {
            if (scrollType.name().equalsIgnoreCase(name)) {
                return scrollType;
            }
        }

        return null;
    }

}
