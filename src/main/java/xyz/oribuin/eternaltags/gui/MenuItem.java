package xyz.oribuin.eternaltags.gui;

import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.guis.BaseGui;
import dev.triumphteam.gui.guis.GuiItem;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.action.Action;
import xyz.oribuin.eternaltags.action.PluginAction;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class MenuItem {

    private CommentedFileConfiguration config; // The config file for the menu
    private ItemStack customItem; // The item to be displayed
    private String itemPath; // The path to the item in the config
    private StringPlaceholders placeholders; // The custom placeholders for the item
    private Player player; //  The player who is viewing the menu, this is used for placeholders
    private Consumer<InventoryClickEvent> action; // The action to be performed when the item is clicked
    private List<Integer> slots; // The slots the item should be placed in
    private boolean condition; // The condition for the item to be displayed
    private final Map<ClickType, List<Action>> customActions; // The actions to be performed when the item is clicked

    public MenuItem() {
        throw new UnsupportedOperationException("This class cannot be instantiated, Use MenuItem.create() instead.");
    }

    private MenuItem(CommentedFileConfiguration config) {
        this.config = config;
        this.customItem = null;
        this.itemPath = null;
        this.placeholders = StringPlaceholders.empty();
        this.player = null;
        this.action = inventoryClickEvent -> {}; // Do nothing
        this.slots = new ArrayList<>();
        this.condition = true;
        this.customActions = new HashMap<>();
    }

    /**
     * Create a new MenuItem instance
     *
     * @param config The config file for the menu
     * @return A new MenuItem instance
     */
    public static MenuItem create(CommentedFileConfiguration config) {
        return new MenuItem(config);
    }

    public final void place(BaseGui gui) {

        // Make sure there is a config file
        if (this.config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }

        // Make sure the item is not null
        if (this.customItem == null && this.itemPath == null) {
            throw new IllegalArgumentException("Item path and custom item cannot both be null");
        }

        // Check if the item path is null
        if (this.config.get(this.itemPath) == null && this.customItem == null) {
            throw new IllegalArgumentException("Item path does not exist in config (" + this.itemPath + ")");
        }

        // Check if the item is enabled
        if (!this.config.getBoolean(this.itemPath + ".enabled", true)) {
            return;
        }

        if (!this.isConditional())
            return;

        // Add any slots that were not added
        if (this.slots.isEmpty()) {
            // We check for the singular slot first
            var slot = (Integer) this.config.get(this.itemPath + ".slot");
            if (slot != null) {
                this.slot(slot);
            }

            // Then we check for the multiple slots
            var hasMultiSlots = this.config.get(this.itemPath + ".slots") != null;
            if (hasMultiSlots) {
                this.slots(TagsUtils.parseList(this.config.getStringList(this.itemPath + ".slots")));
            }
        }

        var item = this.customItem != null
                ? this.customItem
                : TagsUtils.getItemStack(this.config, this.itemPath, this.player, this.placeholders);


        this.addActions();
        this.slots.forEach(slot -> gui.setItem(slot, new GuiItem(item, this.action::accept)));
        gui.update();

    }


    /**
     * Add all the custom actions to the item
     *
     * @since 1.1.7
     */
    private void addActions() {
        var customActions = this.config.getConfigurationSection(this.itemPath + ".commands");
        if (customActions == null)
            return;

        for (var key : customActions.getKeys(false)) {
            var clickType = TagsUtils.getEnum(ClickType.class, key.toUpperCase());
            if (clickType == null) {
                EternalTags.getInstance().getLogger().warning("Invalid click type [" + key + "] in the " + this.itemPath + ".commands section of the [" + this.config.getName() + "] menu.");
                continue;
            }

            var actionList = new ArrayList<Action>();
            this.config.getStringList(this.itemPath + ".commands." + key)
                    .stream()
                    .map(PluginAction::parse)
                    .filter(Objects::nonNull)
                    .forEach(actionList::add);

            this.customActions.put(clickType, actionList);
        }

        if (this.customActions.isEmpty())
            return;

        this.action = event -> {
            List<Action> actions = this.customActions.get(event.getClick());
            if (actions == null)
                return;

            actions.forEach(action -> action.execute((Player) event.getWhoClicked(), this.placeholders));
        };
    }

    /**
     * Get the path to the item in the config
     *
     * @return The path
     */
    public String getItemPath() {
        return itemPath;
    }

    /**
     * Set the path to the item in the config
     *
     * @param path The path
     * @return The MenuItem
     */
    public final MenuItem path(String path) {
        this.itemPath = path;
        return this;
    }

    public ItemStack getCustomItem() {
        return customItem;
    }

    public final MenuItem item(ItemStack item) {
        this.customItem = item;
        return this;
    }

    public Player getPlayer() {
        return player;
    }

    public final MenuItem player(Player player) {
        this.player = player;
        return this;
    }

    public StringPlaceholders getPlaceholders() {
        return placeholders;
    }

    public final MenuItem placeholders(StringPlaceholders placeholders) {
        this.placeholders = placeholders;
        return this;
    }

    public Consumer<InventoryClickEvent> getAction() {
        return action;
    }

    public final MenuItem action(Consumer<InventoryClickEvent> action) {
        this.action = action;
        return this;
    }

    public List<Integer> getSlots() {
        return slots;
    }

    public final MenuItem slots(List<Integer> slots) {
        this.slots = slots;
        return this;
    }

    public final MenuItem slot(int slot) {
        this.slots = List.of(slot);
        return this;
    }

    public CommentedFileConfiguration getConfig() {
        return config;
    }

    public final MenuItem config(CommentedFileConfiguration config) {
        this.config = config;
        return this;
    }

    public boolean isConditional() {
        return this.condition;
    }

    public MenuItem conditional(boolean condition) {
        this.condition = condition;
        return this;
    }

    public Map<ClickType, List<Action>> getCustomActions() {
        return customActions;
    }

}
