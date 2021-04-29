package xyz.oribuin.eternaltags.gui;

import me.mattstudios.mfgui.gui.components.ItemBuilder;
import me.mattstudios.mfgui.gui.guis.GuiItem;
import me.mattstudios.mfgui.gui.guis.PaginatedGui;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.hook.PAPI;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.util.HexUtils;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TagGUI {

    private final EternalTags plugin;
    private final DataManager data;
    private final TagManager tagManager;

    private final PaginatedGui gui;

    public TagGUI(final EternalTags plugin) {
        this.plugin = plugin;
        this.data = this.plugin.getManager(DataManager.class);
        this.tagManager = this.plugin.getManager(TagManager.class);


        this.gui = new PaginatedGui(6, this.plugin.getMenuConfig().getString("menu-name"));
        this.gui.setUpdating(true);
        this.gui.setDefaultClickAction(event -> {
            event.setResult(Event.Result.DENY);
            event.setCancelled(true);
            ((Player) event.getWhoClicked()).updateInventory();
        });

    }

    /**
     * Create and open the GUI for the player
     *
     * @param player The player
     */
    public void createGUI(final Player player, @Nullable final String keyword) {
        this.clearItems(player);

        // Add the border slots
        final List<Integer> borderSlots = new ArrayList<>();
        for (int i = 45; i < 54; i++) borderSlots.add(i);
        borderSlots.forEach(integer -> gui.setItem(integer, fillerItem()));

        gui.setItem(47, ItemBuilder.from(this.getGUIItem("previous-page", null, player)).asGuiItem(event -> gui.previous()));
        gui.setItem(51, ItemBuilder.from(this.getGUIItem("next-page", null, player)).asGuiItem(event -> gui.next()));

        final List<Tag> tags = this.tagManager.getPlayersTag(player);

        if (keyword != null)
            tags.removeIf(tag -> tag.getId().equalsIgnoreCase(keyword));


        tags.forEach(tag -> gui.addItem(ItemBuilder.from(this.getGUIItem("tag", tag, player))
                .asGuiItem(event -> {

                    if (!this.tagManager.getTags().contains(tag)) {
                        this.createGUI(player, keyword);
                        return;
                    }

                    event.getWhoClicked().closeInventory();
                    this.data.updateUser(event.getWhoClicked().getUniqueId(), tag);
                    this.plugin.getManager(MessageManager.class).send(event.getWhoClicked(), "changed-tag", StringPlaceholders.single("tag", tag.getTag()));
                })));

        gui.open(player);
    }

    /**
     * Get an itemstack based on a configuration path.
     *
     * @param path   The config path
     * @param tag    A potential tag for StringPlaceholders
     * @param player The player
     * @return The itemstack formed from the gui
     */
    private ItemStack getGUIItem(final String path, final @Nullable Tag tag, Player player) {
        final FileConfiguration config = this.plugin.getMenuConfig();

        StringPlaceholders placeholders = StringPlaceholders.empty();

        if (tag != null) {
            placeholders = StringPlaceholders.builder("tag", tag.getTag())
                    .addPlaceholder("description", tag.getDescription())
                    .addPlaceholder("id", tag.getId())
                    .addPlaceholder("name", tag.getName())
                    .build();
        }

        final List<String> lore = new ArrayList<>();
        StringPlaceholders finalPlaceholders = placeholders;
        config.getStringList(path + ".lore").forEach(s -> lore.add(format(s, player, finalPlaceholders)));

        return ItemBuilder.from(Material.valueOf(config.getString(path + ".material")))
                .setName(format(config.getString(path + ".name"), player, finalPlaceholders))
                .setLore(lore)
                .setAmount(config.getInt(path + ".amount"))
                .glow(config.getBoolean(path + ".glow"))
                .build();
    }

    private String format(String string, Player player, StringPlaceholders placeholders) {
        return HexUtils.colorify(PAPI.apply(player, placeholders.apply(string)));
    }

    /**
     * Clear all the items inside a gui then reopen it.
     *
     * @param player The player.
     */
    public void clearItems(final Player player) {
        final List<Integer> slots = new ArrayList<>();
        for (int i = 0; i <= 52; i++) slots.add(i);
        gui.setItem(slots, ItemBuilder.from(Material.AIR).asGuiItem());
        gui.open(player);
    }

    private GuiItem fillerItem() {
        return ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE).setName(HexUtils.colorify("&a")).asGuiItem();
    }

}
