package xyz.oribuin.eternaltags.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.triumphteam.gui.guis.GuiItem;
import dev.triumphteam.gui.guis.PaginatedGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.event.TagEquipEvent;
import xyz.oribuin.eternaltags.event.TagUnequipEvent;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.MenuManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FavouritesGUI extends PluginGUI {

    private final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
    private final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);

    public FavouritesGUI(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    public void open(Player player) {
        PaginatedGui gui = this.createPagedGUI(player);

        this.put(gui, "border-item", player);
        this.put(gui, "next-page", player, event -> {
            gui.next();
            gui.updateTitle(this.formatString(player, this.get("menu-name"), this.getPagePlaceholders(gui)));
        });

        this.put(gui, "previous-page", player, event -> {
            gui.previous();
            gui.updateTitle(this.formatString(player, this.get("menu-name"), this.getPagePlaceholders(gui)));
        });

        this.put(gui, "clear-tag", player, event -> {
            final TagUnequipEvent tagUnequipEvent = new TagUnequipEvent(player);
            Bukkit.getPluginManager().callEvent(tagUnequipEvent);
            if (tagUnequipEvent.isCancelled())
                return;

            this.manager.clearTag(event.getWhoClicked().getUniqueId());
            this.locale.sendMessage(event.getWhoClicked(), "command-clear-cleared");
            gui.close(player);
        });

        this.put(gui, "main-menu", player, event -> {
            final MenuManager manager = this.rosePlugin.getManager(MenuManager.class);
            manager.get(TagsGUI.class).open(player, null);
        });

        gui.open(player);
        gui.updateTitle(this.formatString(player, this.get("menu-name"), this.getPagePlaceholders(gui)));
        final List<Tag> tags = this.getTags(player);

        int dynamicSpeed = this.get("dynamic-speed", 3);
        if (this.get("dynamic-gui", false)) {
            this.rosePlugin.getServer().getScheduler().runTaskTimer(this.rosePlugin, task -> {
                if (gui.getInventory().getViewers().isEmpty()) {
                    task.cancel();
                    return;
                }

                this.addTags(gui, player, tags);
            }, 0, dynamicSpeed);
        } else {
            this.addTags(gui, player, tags);
        }

    }

    private void addTags(PaginatedGui gui, Player player, List<Tag> tags) {
        gui.clearPageItems();

        tags.stream().map(tag -> new GuiItem(TagsUtils.getItemStack(this.config, "tag-item", player, this.getTagPlaceholders(tag, player)), event -> {
            if (!event.getWhoClicked().hasPermission(tag.getPermission()))
                return;

            if (event.isShiftClick()) {
                this.toggleFavourite(player, tag);
                this.addTags(gui, player, this.getTags(player));
                return;
            }

            this.setTag(player, tag);
            gui.close(player);
        })).forEach(gui::addItem);

        gui.update();
    }


    /**
     * Get a list of tags that should be added to the GUI
     *
     * @param player The player
     * @return The list of tags
     */
    private List<Tag> getTags(Player player) {
        List<Tag> tags = new ArrayList<>(manager.getUsersFavourites(player.getUniqueId()).values());
        this.sortTags(tags);
        return tags;
    }

    /**
     * Automagically sort all the tags by type.
     *
     * @param tags The tags to be sorted.
     */
    private void sortTags(List<Tag> tags) {
        SortType sortType = SortType.match(this.get("sort-type", null)).orElse(SortType.ALPHABETICAL);
        switch (sortType) {
            case ALPHABETICAL -> tags.sort(Comparator.comparing(Tag::getName));
            case CUSTOM -> tags.sort(Comparator.comparing(Tag::getOrder));
            case RANDOM -> Collections.shuffle(tags);
        }
    }

    @Override
    public @NotNull Map<String, Object> getDefaultValues() {
        return new LinkedHashMap<>() {{
            this.put("#0", "Configure the name at the top of the gui.");
            this.put("menu-name", "EternalTags | %page%/%total%");
            this.put("#1", "Available Options: ALPHABETICAL, CUSTOM, NONE, RANDOM");
            this.put("sort-type", SortType.ALPHABETICAL.name());
            this.put("#2", "Should favourite tags be put at the start of the gui?");
            this.put("favorites-first", true);
            this.put("#3", "Should all tags be added to the gui?");
            this.put("add-all-tags", false);
            this.put("#5", "Should the gui update frequently for animated tags?");
            this.put("dynamic-gui", false);
            this.put("#6", "The speed (in ticks) that the dynamic gui updates in.");
            this.put("dynamic-speed", 3);
            this.put("#7", "The text before any new lines when using %description% placeholder.");
            this.put("description-format", " &f| &7");

            // Tag Item
            this.put("#8", "The display item for tags");
            this.put("tag-item.material", Material.NAME_TAG.name());
            this.put("tag-item.amount", 1);
            this.put("tag-item.name", "%tag%");
            this.put("tag-item.lore", Arrays.asList(
                    " &f| &7Click to change your",
                    " &f| &7active tag to %name%",
                    " &f| &7Shift-Click to set as favorite",
                    " &f|",
                    " &f| &7%description%"
            ));
            this.put("tag-item.glow", true);

            // Next Page Item
            this.put("#9", "The display item for the next page button");
            this.put("next-page.material", Material.PAPER.name());
            this.put("next-page.name", "#00B4DB&lNext Page");
            this.put("next-page.slot", 52);

            // Previous Page Item
            this.put("#10", "The display item for the next page button");
            this.put("previous-page.material", Material.PAPER.name());
            this.put("previous-page.name", "#00B4DB&lPrevious Page");
            this.put("previous-page.slot", 46);

            // Clear Tag Item
            this.put("#11", "The display item for clearing active tag.");
            this.put("clear-tag.enabled", true);
            this.put("clear-tag.slot", 50);
            this.put("clear-tag.material", Material.PLAYER_HEAD.name());
            this.put("clear-tag.name", "#00B4DB&lClear Tag");
            this.put("clear-tag.lore", Arrays.asList(
                    " &f| &7Click to clear your",
                    " &f| &7current active tag.",
                    " &f| &7",
                    " &f| &7Current Tag: #00B4DB%eternaltags_tag_formatted%"
            ));
            this.put("clear-tag.texture", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTljZGI5YWYzOGNmNDFkYWE1M2JjOGNkYTc2NjVjNTA5NjMyZDE0ZTY3OGYwZjE5ZjI2M2Y0NmU1NDFkOGEzMCJ9fX0=");

            // Favourites Tag Item
            this.put("#12", "The display item for viewing favourite tags.");
            this.put("favorite-tags.enabled", true);
            this.put("favorite-tags.slot", 48);
            this.put("favorite-tags.material", Material.PLAYER_HEAD.name());
            this.put("favorite-tags.name", "#00B4DB&lFavorite Tags");
            this.put("favorite-tags.lore", Arrays.asList(
                    " &f| &7Click to view all your",
                    " &f| &7favorite tags in one menu."
            ));
            this.put("favorite-tags.texture", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDVjNmRjMmJiZjUxYzM2Y2ZjNzcxNDU4NWE2YTU2ODNlZjJiMTRkNDdkOGZmNzE0NjU0YTg5M2Y1ZGE2MjIifX19");

            this.put("#13", "The border item at the bottom of the gui.");
            this.put("border-item.enabled", true);
            this.put("border-item.material", Material.GRAY_STAINED_GLASS_PANE.name());
            this.put("border-item.name", " ");
            this.put("border-item.slots", List.of("45-53"));
        }};
    }

    @Override
    public @NotNull String getMenuName() {
        return "favorites-gui";
    }

    private enum SortType {
        ALPHABETICAL, CUSTOM, NONE, RANDOM;

        /**
         * Match a sort type by their name.
         *
         * @param name The name of the sort type
         * @return A matching type if present.
         */
        public static Optional<SortType> match(String name) {
            if (name == null)
                return Optional.empty();

            return Arrays.stream(SortType.values()).filter(sortType -> sortType.name().equalsIgnoreCase(name)).findFirst();
        }
    }


    /**
     * Set a player's active tag
     *
     * @param player The player
     * @param tag    The tag
     */
    private void setTag(Player player, Tag tag) {
        final TagEquipEvent event = new TagEquipEvent(player, tag);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        this.rosePlugin.getManager(TagsManager.class).setTag(player.getUniqueId(), tag);
        this.rosePlugin.getManager(LocaleManager.class).sendMessage(player, "command-set-changed", StringPlaceholders.single("tag", this.manager.getDisplayTag(tag, player)));
    }

    /**
     * Toggle a player's favourite tag
     *
     * @param player The player
     * @param tag    The tag
     */
    private void toggleFavourite(Player player, Tag tag) {
        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        boolean isFavourite = manager.isFavourite(player.getUniqueId(), tag);

        if (isFavourite)
            manager.removeFavourite(player.getUniqueId(), tag);
        else
            manager.addFavourite(player.getUniqueId(), tag);

        String on = locale.getLocaleMessage("command-favorite-on");
        String off = locale.getLocaleMessage("command-favorite-off");

        locale.sendMessage(player, "command-favorite-toggled", StringPlaceholders.builder("tag", manager.getDisplayTag(tag, player)).addPlaceholder("toggled", !isFavourite ? on : off).build());
    }

    public StringPlaceholders getTagPlaceholders(Tag tag, OfflinePlayer player) {
        return StringPlaceholders.builder()
                .addPlaceholder("tag", this.manager.getDisplayTag(tag, player))
                .addPlaceholder("id", tag.getId())
                .addPlaceholder("name", tag.getName())
                .addPlaceholder("description", String.join(", ", tag.getDescription()))
                .addPlaceholder("permission", tag.getPermission())
                .addPlaceholder("order", tag.getOrder())
                .build();
    }

    @Override
    protected int rows() {
        return 6;
    }
}
