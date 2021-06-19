package xyz.oribuin.eternaltags.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.event.TagEquipEvent;
import xyz.oribuin.eternaltags.hook.PAPI;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.gui.Item;
import xyz.oribuin.gui.PaginatedGui;
import xyz.oribuin.orilibrary.util.HexUtils;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class TagGUI {

    private final EternalTags plugin;
    private final DataManager data;
    private final TagManager tagManager;
    private final Player player;

    public TagGUI(final EternalTags plugin, final Player player) {
        this.plugin = plugin;
        this.data = this.plugin.getManager(DataManager.class);
        this.tagManager = this.plugin.getManager(TagManager.class);
        this.player = player;

    }

    /**
     * Create and open the gui for a player.
     */
    public void createGUI() {

        final List<Integer> pageSlots = new ArrayList<>();
        for (int i = 0; i < 45; i++) pageSlots.add(i);

        final PaginatedGui gui = new PaginatedGui(54, cs(this.plugin.getMenuConfig().getString("menu-name"), player, StringPlaceholders.empty()), pageSlots);
        final StringPlaceholders.Builder pagePlaceholders = StringPlaceholders.builder()
                .addPlaceholder("currentPage", gui.getCurrentPage())
                .addPlaceholder("prevPage", gui.getPrevPage())
                .addPlaceholder("nextPage", gui.getNextPage())
                .addPlaceholder("totalPages", gui.getTotalPages());

        gui.updateTitle(cs(this.plugin.getMenuConfig().getString("menu-name"), player, pagePlaceholders.build()));

        gui.setDefaultClickFunction(event -> {
            ((Player) event.getWhoClicked()).updateInventory();
            gui.update();
        });

        // Get all the border slots;
        final List<Integer> borderSlots = new ArrayList<>();
        for (int i = 45; i < 54; i++) borderSlots.add(i);
        borderSlots.forEach(i -> gui.setItem(i, fillerItem(), event -> {}));

        // Add page items
        gui.setItem(47,this.getGuiItem(gui, "previous-page", null, player), event -> gui.previous(player));

        gui.setItem(51, this.getGuiItem(gui, "next-page", null, player), event -> gui.next(player));

        // Add clear tag item.
        gui.setItem(49, this.getGuiItem(gui, "clear-tag", null, player), event -> {
            this.plugin.getManager(MessageManager.class).send(event.getWhoClicked(), "cleared-tag");
            this.data.updateUser(event.getWhoClicked().getUniqueId(), null);
            event.getWhoClicked().closeInventory();
        });

        // Extra Items
        final ConfigurationSection section = this.plugin.getMenuConfig().getConfigurationSection("extra-items");
        if (section != null) {
            section.getKeys(false).forEach(s -> gui.setItem(section.getInt(s + ".slot"), this.getGuiItem(gui, s, null, player),event -> {}));
        }

//         Add all the tags to the gui.
        this.tagManager.getPlayersTag(player).forEach(tag -> gui.addPageItem(this.getGuiItem(gui, "tag", tag, player), event -> {
            if (!this.tagManager.getTags().contains(tag)) {
                event.getWhoClicked().closeInventory();
                return;
            }

            final TagEquipEvent tagEquipEvent = new TagEquipEvent(player, tag);
            Bukkit.getPluginManager().callEvent(tagEquipEvent);
            if (tagEquipEvent.isCancelled()) return;

            event.getWhoClicked().closeInventory();
            this.data.updateUser(event.getWhoClicked().getUniqueId(), tag);
            this.plugin.getManager(MessageManager.class).send(event.getWhoClicked(), "changed-tag", StringPlaceholders.single("tag", tag.getTag()));
        }));

        gui.open(player, 1);
    }

    /**
     * Create an ItemStack from a configuration path.
     *
     * @param gui    The gui it's being added to (Used for page placeholders)
     * @param path   The path to the item.
     * @param tag    Any tag for tag placeholders.
     * @param player The player for PAPI text
     * @return The ItemStack
     * @since 1.0.5
     */
    private ItemStack getGuiItem(PaginatedGui gui, final String path, final Tag tag, Player player) {
        final FileConfiguration config = this.plugin.getMenuConfig();

        final StringPlaceholders.Builder builder = StringPlaceholders.builder()
                .addPlaceholder("currentPage", gui.getCurrentPage())
                .addPlaceholder("prevPage", gui.getPrevPage())
                .addPlaceholder("nextPage", gui.getNextPage())
                .addPlaceholder("totalPages", gui.getTotalPages());

        if (tag != null) {
            builder.addPlaceholder("tag", tag.getTag());
            builder.addPlaceholder("description", tag.getDescription());
            builder.addPlaceholder("id", tag.getId());
            builder.addPlaceholder("name", tag.getName());
        }

        final StringPlaceholders placeholders = builder.build();
        final List<String> lore = config.getStringList(path + ".lore").stream().map(s -> cs(s, player, placeholders)).collect(Collectors.toList());

        if (config.getString(path + ".material") == null) return new ItemStack(Material.AIR);

        final Material material = Optional.ofNullable(Material.matchMaterial(config.getString(path + ".material"))).orElse(Material.BARREL);

        final Item.Builder itemBuilder = new Item.Builder(material).setName(cs(config.getString(path + ".name"), player, placeholders)).setLore(lore).setAmount(config.getInt(path + ".amount"));

        if (config.getBoolean(path + ".glow")) {
            itemBuilder.glow();
        }


        final String texture = config.getString(path + ".texture");

        if (texture != null) {
            itemBuilder.setTexture(texture);
        }

        final ConfigurationSection nbt = config.getConfigurationSection(path + ".nbt");
        if (nbt != null) {
            for (String s : nbt.getKeys(false)) itemBuilder.setNBT(s, nbt.get(s));
        }

        return itemBuilder.create();
    }

    /**
     * Colorized text
     *
     * @param txt          The message
     * @param player       The player for PAPI Placeholders
     * @param placeholders Any string placeholders.
     * @return txt but colorified
     * @since 1.0.5
     */
    private String cs(String txt, Player player, StringPlaceholders placeholders) {
        return HexUtils.colorify(PAPI.apply(player, placeholders.apply(txt)));
    }

    /**
     * A general filler item for border items
     *
     * @return The GUI Item.
     */
    private ItemStack fillerItem() {
        return new Item.Builder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").create();
    }
}
