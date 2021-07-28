package xyz.oribuin.eternaltags.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
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
import xyz.oribuin.orilibrary.util.StringPlaceholders;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static xyz.oribuin.orilibrary.util.HexUtils.colorify;

public class TagGUI {

    private final EternalTags plugin;
    private final DataManager data;
    private final TagManager tagManager;
    private final Player player;
    private final String keyword;

    public TagGUI(final EternalTags plugin, final Player player) {
        this.plugin = plugin;
        this.data = this.plugin.getManager(DataManager.class);
        this.tagManager = this.plugin.getManager(TagManager.class);
        this.player = player;
        this.keyword = null;
    }

    // This isnt a mess, I promise.
    public TagGUI(final EternalTags plugin, final Player player, String keyword) {
        this.plugin = plugin;
        this.data = this.plugin.getManager(DataManager.class);
        this.tagManager = this.plugin.getManager(TagManager.class);
        this.player = player;
        this.keyword = keyword;
    }

    /**
     * Create and open the gui for a player.
     */
    public void createGUI() {

        final List<Integer> pageSlots = new ArrayList<>();
        for (int i = 0; i < 45; i++)
            pageSlots.add(i);

        final PaginatedGui gui = new PaginatedGui(54, cs(this.plugin.getMenuConfig().getString("menu-name"), player, StringPlaceholders.empty()), pageSlots);

        final List<Tag> playersTag = new ArrayList<>(this.tagManager.getPlayersTag(player));
        playersTag.sort(Comparator.comparing(Tag::getName));

        if (keyword != null) {
            playersTag.removeIf(tag -> !tag.getName().toLowerCase().contains(keyword.toLowerCase()));
        }

        //  Add all the tags to the gui.
        playersTag.forEach(tag -> gui.addPageItem(this.getGuiItem("tag", tag, player), event -> {
            if (!this.tagManager.getTags().contains(tag)) {
                event.getWhoClicked().closeInventory();
                return;
            }

            final TagEquipEvent tagEquipEvent = new TagEquipEvent(player, tag);
            Bukkit.getPluginManager().callEvent(tagEquipEvent);
            if (tagEquipEvent.isCancelled())
                return;

            event.getWhoClicked().closeInventory();
            this.data.updateUser(event.getWhoClicked().getUniqueId(), tag);
            this.plugin.getManager(MessageManager.class).send(event.getWhoClicked(), "changed-tag",
                    StringPlaceholders.single("tag", colorify(tag.getTag())));
        }));

        gui.setDefaultClickFunction(event -> {
            ((Player) event.getWhoClicked()).updateInventory();
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        });

        // Get all the border slots;
        final List<Integer> borderSlots = new ArrayList<>();
        for (int i = 45; i < 54; i++)
            borderSlots.add(i);

        gui.setItems(borderSlots, fillerItem(), event -> { });

        // Add previous page item
        gui.setItem(47, this.getGuiItem("previous-page", null, player), event -> {
            gui.previous(player);
            gui.updateTitle(cs(this.plugin.getMenuConfig().getString("menu-name"), player, this.getPages(gui).build()));
        });

        // Add next page item
        gui.setItem(51, this.getGuiItem("next-page", null, player), event -> {
            gui.next(player);
            gui.updateTitle(cs(this.plugin.getMenuConfig().getString("menu-name"), player, this.getPages(gui).build()));
        });

        // Add clear tag item.
        if (this.plugin.getMenuConfig().getBoolean("clear-tag.enabled")) {

            gui.setItem(49, this.getGuiItem("clear-tag", null, player), event -> {
                this.plugin.getManager(MessageManager.class).send(event.getWhoClicked(), "cleared-tag");
                this.data.updateUser(event.getWhoClicked().getUniqueId(), null);
                event.getWhoClicked().closeInventory();
            });

        }
        // Extra Items
        final ConfigurationSection section = this.plugin.getMenuConfig().getConfigurationSection("extra-items");
        if (section != null) {
            section.getKeys(false).forEach(s -> gui.setItem(section.getInt(s + ".slot"), this.getGuiItem(s, null, player), event -> {}));
        }

        gui.open(player, 1);
        gui.updateTitle(cs(this.plugin.getMenuConfig().getString("menu-name"), player, this.getPages(gui).build()));

    }

    /**
     * Create an ItemStack from a configuration path.
     *
     * @param path   The path to the item.
     * @param tag    Any tag for tag placeholders.
     * @param player The player for PAPI text
     * @return The ItemStack
     * @since 1.0.5
     */
    private ItemStack getGuiItem(final String path, final Tag tag, Player player) {
        final FileConfiguration config = this.plugin.getMenuConfig();

        final StringPlaceholders.Builder builder = StringPlaceholders.builder();

        if (tag != null) {
            builder.addPlaceholder("tag", colorify(tag.getTag()));
            builder.addPlaceholder("id", tag.getId());
            builder.addPlaceholder("name", tag.getName());
        }

        final StringPlaceholders placeholders = builder.build();

        List<String> lore = config.getStringList(path + ".lore").stream()
                .map(s -> cs(s, player, placeholders))
                .collect(Collectors.toList());


        if (tag != null) {
            // I am aware this code is awful, I do not like it either but it is the only solution i could come up with
            for (int i = 0; i < lore.size(); i++) {
                String index = lore.get(i);

                if (!index.toLowerCase().contains("%description%"))
                    continue;

                final List<String> desc = new ArrayList<>(tag.getDescription());

                if (desc.size() == 0) {
                    lore.set(i, index.replace("%description%", "None"));
                    break;
                }

                lore.set(i, index.replace("%description%", cs(desc.get(0), player, placeholders)));
                desc.remove(desc.size() > i ? i : desc.size() - 1);

                AtomicInteger integer = new AtomicInteger(i + 1);
                desc.forEach(s -> {
                    final String color = ChatColor.getLastColors(index);
                    lore.add(integer.getAndIncrement(), color + cs(s, player, placeholders));
                });

                break;
            }

        }

        if (config.getString(path + ".material") == null)
            return new ItemStack(Material.AIR);

        final Material material = Optional.ofNullable(Material.matchMaterial(config.getString(path + ".material"))).orElse(Material.BARREL);

        final Item.Builder itemBuilder = new Item.Builder(material)
                .setName(cs(config.getString(path + ".name"), player, placeholders))
                .setLore(lore)
                .setAmount(config.getInt(path + ".amount"))
                .glow(config.getBoolean(path + ".glow"));

        final String texture = config.getString(path + ".texture");

        if (texture != null) {
            itemBuilder.setTexture(texture);
        }

        final ConfigurationSection nbt = config.getConfigurationSection(path + ".nbt");
        if (nbt != null) {
            for (String s : nbt.getKeys(false))
                itemBuilder.setNBT(s, nbt.get(s));
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
        return colorify(PAPI.apply(player, placeholders.apply(txt)));
    }

    /**
     * A general filler item for border items
     *
     * @return The GUI Item.
     */
    private ItemStack fillerItem() {
        return new Item.Builder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").create();
    }

    private StringPlaceholders.Builder getPages(PaginatedGui gui) {
        return StringPlaceholders.builder()
                .addPlaceholder("currentPage", gui.getPage())
                .addPlaceholder("prevPage", gui.getPrevPage())
                .addPlaceholder("nextPage", gui.getNextPage())
                .addPlaceholder("totalPages", gui.getTotalPages());
    }

}
