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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static xyz.oribuin.orilibrary.util.HexUtils.colorify;

public class FavouriteGUI {

    private final EternalTags plugin;
    private final DataManager data;
    private final TagManager tagManager;
    private final Player player;

    private final List<Tag> playersTags;

    public FavouriteGUI(final EternalTags plugin, final Player player) {
        this.plugin = plugin;
        this.data = this.plugin.getManager(DataManager.class);
        this.tagManager = this.plugin.getManager(TagManager.class);
        this.player = player;

        this.playersTags = new ArrayList<>();
        data.getFavourites(player).forEach(tag -> {
            final List<String> currentTags = this.playersTags.stream().map(Tag::getId).map(String::toLowerCase).collect(Collectors.toList());
            if (currentTags.contains(tag.getId().toLowerCase()))
                return;

            this.playersTags.add(tag);
        });
        this.sortList(playersTags);
    }

    /**
     * Create and open the gui for a player.
     */
    public void createGUI() {
        final FileConfiguration config = this.plugin.getFavouriteConfig();

        final List<Integer> pageSlots = new ArrayList<>();
        for (int i = 0; i < 45; i++)
            pageSlots.add(i);

        final PaginatedGui gui = new PaginatedGui(54, cs(config.getString("menu-name"), player, StringPlaceholders.empty()), pageSlots);

        //  Add all the tags to the gui.
        playersTags.forEach(tag -> gui.addPageItem(this.getGuiItem("tag", tag, player), event -> {
            if (!this.tagManager.getTags().contains(tag)) {
                event.getWhoClicked().closeInventory();
                return;
            }

            // Apologies bedrock users.
            if (event.isShiftClick()) {
                final UUID uuid = event.getWhoClicked().getUniqueId();
                data.removeFavourite(uuid, tag);
                new FavouriteGUI(this.plugin, player).createGUI();
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

        gui.setPersonalClickAction(e -> gui.getPersonalClickAction().accept(e));

        // Get all the border slots;
        final List<Integer> borderSlots = new ArrayList<>();
        for (int i = 45; i < 54; i++)
            borderSlots.add(i);

        gui.setItems(borderSlots, fillerItem(), event -> {
        });

        // Add previous page item
        gui.setItem(47, this.getGuiItem("previous-page", null, player), event -> {
            gui.previous(player);
            gui.updateTitle(cs(config.getString("menu-name"), player, this.getPages(gui).build()));
        });

        // Add next page item
        gui.setItem(51, this.getGuiItem("next-page", null, player), event -> {
            gui.next(player);
            gui.updateTitle(cs(config.getString("menu-name"), player, this.getPages(gui).build()));
        });

        if (config.getBoolean("go-back.enabled")) {
            gui.setItem(config.getInt("go-back.slot"), this.getGuiItem("go-back", null, player), event ->
                    new TagGUI(this.plugin, (Player) event.getWhoClicked(), null).createGUI());
        }


        // Extra Items
        final ConfigurationSection section = config.getConfigurationSection("extra-items");
        if (section != null) {
            section.getKeys(false).forEach(s -> gui.setItem(section.getInt(s + ".slot"), this.getGuiItem(s, null, player), event -> {
            }));
        }

        gui.open(player, 1);
        gui.updateTitle(cs(config.getString("menu-name"), player, this.getPages(gui).build()));

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
        final FileConfiguration config = this.plugin.getFavouriteConfig();

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
            // Reject humanity, Become GriefPreventions developer
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

        Material material = Optional.ofNullable(Material.matchMaterial(config.getString(path + ".material"))).orElse(Material.BARREL);

        if (tag != null && tag.getIcon() != null)
            material = tag.getIcon();

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
            for (String s : nbt.getKeys(false)) {

                // else if ladders are painful
                if (nbt.get(s) instanceof String)
                    itemBuilder.setNBT(plugin, s, nbt.getString(s));
                else if (nbt.get(s) instanceof Integer)
                    itemBuilder.setNBT(plugin, s, nbt.getInt(s));
                else if (nbt.get(s) instanceof Double)
                    itemBuilder.setNBT(plugin, s, nbt.getString(s));
                else
                    itemBuilder.setNBT(plugin, s, nbt.getString(s));

            }
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
     * Sort the list of tags in the gui
     *
     * @param tags The list of plugin tags.
     */
    private void sortList(List<Tag> tags) {
        final String sortTypeOption = this.plugin.getFavouriteConfig().getString("sort-type");

        if (sortTypeOption != null && sortTypeOption.equalsIgnoreCase("NONE")) {
            return;
        }

        if (sortTypeOption != null && sortTypeOption.equalsIgnoreCase("CUSTOM")) {
            tags.sort(Comparator.comparing(Tag::getOrder));
            return;
        }

        if (sortTypeOption != null && sortTypeOption.equalsIgnoreCase("RANDOM")) {
            Collections.shuffle(tags);
            return;
        }

        tags.sort(Comparator.comparing(Tag::getName));
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
