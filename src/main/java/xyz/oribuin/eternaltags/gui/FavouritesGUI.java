package xyz.oribuin.eternaltags.gui;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.event.TagEquipEvent;
import xyz.oribuin.eternaltags.event.TagUnequipEvent;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.MenuManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.gui.PaginatedGui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FavouritesGUI extends OriGUI {

    public FavouritesGUI(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public void createGUI(Player player, @Nullable String keyword) {
        final List<Tag> tags = this.getTags(player);
        PaginatedGui gui = this.createBaseGUI(player);

        // Add the border items.
        if (this.get("border-item.enabled", true)) {
            final ItemStack item = this.recreateItem("border-item", player);
            if (item == null)
                return;

            for (int i = 45; i < 54; i++)
                this.put(gui, i, item);
        }

        // Add page items.
        this.put(gui, "next-page", player, event -> {
            gui.next(player);
            gui.updateTitle(this.format(player, this.get("menu-name", null), this.getPages(gui)));
        });

        this.put(gui, "previous-page", player, event -> {
            gui.previous(player);
            gui.updateTitle(this.format(player, this.get("menu-name", null), this.getPages(gui)));
        });

        // Add clear tags option
        // yes there's potential for it to completely fuck over if the user changes their commands? yes but do i care? god fuckin no

        if (this.get("clear-tag.enabled", true)) {
            this.put(gui, "clear-tag", player, event -> {
                final TagUnequipEvent tagUnequipEvent = new TagUnequipEvent((Player) event.getWhoClicked());
                Bukkit.getPluginManager().callEvent(tagUnequipEvent);
                if (event.isCancelled())
                    return;

                this.rosePlugin.getManager(TagsManager.class).clearTag(event.getWhoClicked().getUniqueId());
                this.rosePlugin.getManager(LocaleManager.class).sendMessage(event.getWhoClicked(), "command-clear-cleared");

                event.getWhoClicked().closeInventory();
            });
        }

        // Add favourites tag option
        if (this.get("main-menu.enabled", true)) {
            this.put(gui, "main-menu", player, event -> {
                final MenuManager manager = this.rosePlugin.getManager(MenuManager.class);
                manager.matchMenu("tags-gui").ifPresent(menu -> menu.createGUI(player, keyword));
            });
        }

        gui.open(player);
        gui.updateTitle(this.format(player, this.get("menu-name", null), this.getPages(gui)));

        int dynamicSpeed = this.get("dynamic-speed", 3);
        if (this.get("dynamic-gui", false)) {
            this.rosePlugin.getServer().getScheduler().runTaskTimer(this.rosePlugin, task -> {
                if (gui.getInv().getViewers().isEmpty()) {
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
        gui.getPageItems().clear();
        // Add all the tags to the gui
        tags.forEach(tag -> gui.addPageItem(this.createTagItem(tag, "tag-item", player), event -> {
            if (event.isShiftClick()) {
                this.toggleFavourite(player, tag);
                this.addTags(gui, player, this.getTags(player));
                return;
            }

            this.setTag(player, tag);
            event.getWhoClicked().closeInventory();
        }));

        gui.update();
    }


    /**
     * Get a list of tags that should be added to the GUI
     *
     * @param player The player
     * @return The list of tags
     */
    private List<Tag> getTags(Player player) {
        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);

        List<Tag> tags = manager.getPlayersTags(player).stream()
                .filter(tag -> manager.isFavourite(player.getUniqueId(), tag))
                .collect(Collectors.toList());

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
            this.put("menu-name", "Favorite Tags | %page%/%total%");
            this.put("#1", "Available Options: ALPHABETICAL, CUSTOM, NONE, RANDOM");
            this.put("sort-type", SortType.ALPHABETICAL.name());
            this.put("#2", "Should the gui update frequently for animated tags?");
            this.put("dynamic-gui", false);
            this.put("#3", "The speed (in ticks) that the dynamic gui updates in.");
            this.put("dynamic-speed", 3);
            this.put("#4", "The text before any new lines when using %description% placeholder.");
            this.put("description-format", " &f| &7");

            // Tag Item
            this.put("#5", "The display item for tags");
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
            this.put("#6", "The display item for the next page button");
            this.put("next-page.material", Material.PAPER.name());
            this.put("next-page.name", "#00B4DB&lNext Page");
            this.put("next-page.slot", 52);

            // Previous Page Item
            this.put("#7", "The display item for the next page button");
            this.put("previous-page.material", Material.PAPER.name());
            this.put("previous-page.name", "#00B4DB&lPrevious Page");
            this.put("previous-page.slot", 46);

            // Clear Tag Item
            this.put("#8", "The display item for clearing active tag.");
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
            this.put("#9", "The display item for viewing favourite tags.");
            this.put("main-menu.enabled", true);
            this.put("main-menu.slot", 48);
            this.put("main-menu.material", Material.PLAYER_HEAD.name());
            this.put("main-menu.name", "#00B4DB&lMain Menu");
            this.put("main-menu.lore", Arrays.asList(
                    " &f| &7Click to go back to",
                    " &f| &7the main tags menu."
            ));
            this.put("main-menu.texture", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==");

            this.put("#10", "The border item at the bottom of the gui.");
            this.put("border-item.enabled", true);
            this.put("border-item.material", Material.GRAY_STAINED_GLASS_PANE.name());
            this.put("border-item.name", " ");
        }};
    }

    @Override
    public int getRows() {
        return 6;
    }

    @Override
    public @NotNull String getMenuName() {
        return "favorites-gui";
    }

    @Override
    public @NotNull List<Integer> getPageSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 45; i++)
            slots.add(i);

        return slots;
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
        this.rosePlugin.getManager(LocaleManager.class).sendMessage(player, "command-set-changed", StringPlaceholders.single("tag", tag.getTag()));
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

        locale.sendMessage(player, "command-favorite-toggled", StringPlaceholders.builder("tag", tag.getTag()).addPlaceholder("toggled", !isFavourite ? on : off).build());
    }
}
