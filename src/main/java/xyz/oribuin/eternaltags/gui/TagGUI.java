package xyz.oribuin.eternaltags.gui;

public class TagGUI {
//
//    private final RosePlugin plugin;
//    private final DataManager data;
//    private final TagsManager tagManager;
//    private final Player player;
//    private final String keyword;
//
//    private final List<Tag> playersTags;
//
//
//    // This isn't a mess, I promise.
//    public TagGUI(final RosePlugin plugin, final Player player, String keyword) {
//        this.plugin = plugin;
//        this.data = this.plugin.getManager(DataManager.class);
//        this.tagManager = this.plugin.getManager(TagManager.class);
//        this.player = player;
//
//        // Prioritise the favourite tags first :)
//        if (this.plugin.getMenuConfig().getBoolean("favourites-first")) {
//            this.playersTags = new ArrayList<>(this.data.loadFavourites(player));
//            // Sort the favourites correctly.
//            this.sortList(playersTags);
//        } else {
//            this.playersTags = new ArrayList<>();
//        }
//
//        boolean addAllTags = this.plugin.getMenuConfig().getBoolean("show-all-tags");
//
//        List<Tag> otherTags = this.tagManager.getTags().stream()
//                .filter(tag -> !this.playersTags.contains(tag))
//                .collect(Collectors.toList());
//
//        // if we're not showing all tags, remove any tag the player doesnt own
//        if (!addAllTags) {
//            List<Tag> temp = this.tagManager.getPlayersTag(player);
//            otherTags.removeIf(temp::contains);
//        }
//
//        this.sortList(otherTags);
//        this.playersTags.addAll(otherTags);
//        this.keyword = keyword;
//
//        if (keyword != null) {
//            playersTags.removeIf(tag -> !tag.getName().toLowerCase().contains(keyword.toLowerCase()));
//        }
//    }
//
//    /**
//     * Create and open the gui for a player.
//     */
//    public void createGUI() {
//
//        final FileConfiguration config = this.plugin.getMenuConfig();
//        final List<Integer> pageSlots = new ArrayList<>();
//        for (int i = 0; i < 45; i++)
//            pageSlots.add(i);
//
//        final PaginatedGui gui = new PaginatedGui(54, cs(config.getString("menu-name"), player, StringPlaceholders.empty()), pageSlots);
//
//        //  Add all the tags to the gui.
//        playersTags.forEach(tag -> gui.addPageItem(this.getGuiItem("tag", tag, player), event -> {
//            if (!this.tagManager.getTags().contains(tag)) {
//                event.getWhoClicked().closeInventory();
//                return;
//            }
//
//            if (!this.tagManager.getPlayersTag(((Player) event.getWhoClicked())).contains(tag))
//                return;
//
//            // Apologies bedrock users.
//            if (event.isShiftClick()) {
//                final UUID uuid = event.getWhoClicked().getUniqueId();
//                if (data.loadFavourites(uuid).stream().map(Tag::getId).collect(Collectors.toList()).contains(tag.getId())) {
//                    data.removeFavourite(uuid, tag);
//                } else {
//                    data.addFavourite(uuid, tag);
//                }
//
//                return;
//            }
//
//            final TagEquipEvent tagEquipEvent = new TagEquipEvent(player, tag);
//            Bukkit.getPluginManager().callEvent(tagEquipEvent);
//            if (tagEquipEvent.isCancelled())
//                return;
//
//            event.getWhoClicked().closeInventory();
//            this.data.saveUser(event.getWhoClicked().getUniqueId(), tag);
//            this.plugin.getManager(MessageManager.class).send(event.getWhoClicked(), "changed-tag",
//                    StringPlaceholders.single("tag", colorify(tag.getTag())));
//        }));
//
//        gui.setDefaultClickFunction(event -> {
//            ((Player) event.getWhoClicked()).updateInventory();
//            event.setCancelled(true);
//            event.setResult(Event.Result.DENY);
//        });
//
//        gui.setPersonalClickAction(event -> gui.getDefaultClickFunction().accept(event));
//
//        // Get all the border slots;
//        final List<Integer> borderSlots = new ArrayList<>();
//        for (int i = 45; i < 54; i++)
//            borderSlots.add(i);
//
//        gui.setItems(borderSlots, fillerItem(), event -> {
//        });
//
//        // Add previous page item
//        gui.setItem(47, this.getGuiItem("previous-page", null, player), event -> {
//            gui.previous(player);
//            gui.updateTitle(cs(config.getString("menu-name"), player, this.getPages(gui).build()));
//        });
//
//        // Add next page item
//        gui.setItem(51, this.getGuiItem("next-page", null, player), event -> {
//            gui.next(player);
//            gui.updateTitle(cs(config.getString("menu-name"), player, this.getPages(gui).build()));
//        });
//
//        // Add clear tag item.
//        if (config.getBoolean("clear-tag.enabled")) {
//
//            gui.setItem(49, this.getGuiItem("clear-tag", null, player), event -> {
//                this.plugin.getManager(MessageManager.class).send(event.getWhoClicked(), "cleared-tag");
//                this.data.saveUser(event.getWhoClicked().getUniqueId(), null);
//                event.getWhoClicked().closeInventory();
//            });
//        }
//
//        if (config.getBoolean("favourite-tags.enabled")) {
//            gui.setItem(config.getInt("favourite-tags.slot"), this.getGuiItem("favourite-tags", null, player), event ->
//                    new FavouriteGUI(this.plugin, (Player) event.getWhoClicked()).createGUI());
//        }
//
//        // Extra Items
//        final ConfigurationSection section = config.getConfigurationSection("extra-items");
//        if (section != null) {
//            section.getKeys(false).forEach(s -> gui.setItem(section.getInt(s + ".slot"), this.getGuiItem(s, null, player), event -> {
//            }));
//        }
//
//        gui.open(player, 1);
//        gui.updateTitle(cs(config.getString("menu-name"), player, this.getPages(gui).build()));
//    }
//
//    /**
//     * Create an ItemStack from a configuration path.
//     *
//     * @param path   The path to the item.
//     * @param tag    Any tag for tag placeholders.
//     * @param player The player for PAPI text
//     * @return The ItemStack
//     * @since 1.0.5
//     */
//    private ItemStack getGuiItem(final String path, final Tag tag, Player player) {
//        final FileConfiguration config = this.plugin.getMenuConfig();
//
//        final StringPlaceholders.Builder builder = StringPlaceholders.builder();
//
//        if (tag != null) {
//            builder.addPlaceholder("tag", colorify(tag.getTag()));
//            builder.addPlaceholder("id", tag.getId());
//            builder.addPlaceholder("name", tag.getName());
//        }
//
//        final StringPlaceholders placeholders = builder.build();
//
//        List<String> lore = config.getStringList(path + ".lore").stream()
//                .map(s -> cs(s, player, placeholders))
//                .collect(Collectors.toList());
//
//
//        if (tag != null) {
//            // I am aware this code is awful, I do not like it either, but it is the only solution I could come up with
//            // Reject humanity, Become GriefPreventions developer
//            for (int i = 0; i < lore.size(); i++) {
//                String index = lore.get(i);
//
//                if (!index.toLowerCase().contains("%description%"))
//                    continue;
//
//                final List<String> desc = new ArrayList<>(tag.getDescription());
//
//                if (desc.size() == 0) {
//                    lore.set(i, index.replace("%description%", "None"));
//                    break;
//                }
//
//                lore.set(i, index.replace("%description%", cs(desc.get(0), player, placeholders)));
//                desc.remove(desc.size() > i ? i : desc.size() - 1);
//
//                AtomicInteger integer = new AtomicInteger(i + 1);
//                desc.forEach(s -> {
//                    final String color = ChatColor.getLastColors(index);
//                    lore.add(integer.getAndIncrement(), color + cs(s, player, placeholders));
//                });
//
//                break;
//            }
//
//        }
//
//        if (config.getString(path + ".material") == null)
//            return new ItemStack(Material.AIR);
//
//        Material material = Optional.ofNullable(Material.matchMaterial(config.getString(path + ".material"))).orElse(Material.BARREL);
//
//        if (tag != null && tag.getIcon() != null)
//            material = tag.getIcon();
//
//        final Item.Builder itemBuilder = new Item.Builder(material)
//                .setName(cs(config.getString(path + ".name"), player, placeholders))
//                .setLore(lore)
//                .setAmount(config.getInt(path + ".amount"))
//                .glow(config.getBoolean(path + ".glow"));
//
//        final String texture = config.getString(path + ".texture");
//
//        if (texture != null) {
//            itemBuilder.setTexture(texture);
//        }
//
//        final ConfigurationSection nbt = config.getConfigurationSection(path + ".nbt");
//        if (nbt != null) {
//            for (String s : nbt.getKeys(false)) {
//
//                // else if ladders are painful
//                if (nbt.get(s) instanceof String)
//                    itemBuilder.setNBT(plugin, s, nbt.getString(s));
//                else if (nbt.get(s) instanceof Integer)
//                    itemBuilder.setNBT(plugin, s, nbt.getInt(s));
//                else if (nbt.get(s) instanceof Double)
//                    itemBuilder.setNBT(plugin, s, nbt.getString(s));
//                else
//                    itemBuilder.setNBT(plugin, s, nbt.getString(s));
//
//            }
//        }
//
//        return itemBuilder.create();
//    }
//
//    /**
//     * Colorized text
//     *
//     * @param txt          The message
//     * @param player       The player for PAPI Placeholders
//     * @param placeholders Any string placeholders.
//     * @return txt but colorified
//     * @since 1.0.5
//     */
//    private String cs(String txt, Player player, StringPlaceholders placeholders) {
//        return colorify(PAPI.apply(player, placeholders.apply(txt)));
//    }
//
//    /**
//     * Sort the list of tags in the gui
//     *
//     * @param tags The list of plugin tags.
//     */
//    private void sortList(List<Tag> tags) {
//        final String sortTypeOption = this.plugin.getMenuConfig().getString("sort-type");
//
//        if (sortTypeOption != null && sortTypeOption.equalsIgnoreCase("NONE")) {
//            return;
//        }
//
//        if (sortTypeOption != null && sortTypeOption.equalsIgnoreCase("CUSTOM")) {
//            tags.sort(Comparator.comparing(Tag::getOrder));
//            return;
//        }
//
//        if (sortTypeOption != null && sortTypeOption.equalsIgnoreCase("RANDOM")) {
//            Collections.shuffle(tags);
//            return;
//        }
//
//        tags.sort(Comparator.comparing(Tag::getName));
//    }
//
//    /**
//     * A general filler item for border items
//     *
//     * @return The GUI Item.
//     */
//    private ItemStack fillerItem() {
//        return new Item.Builder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").create();
//    }
//
//    private StringPlaceholders.Builder getPages(PaginatedGui gui) {
//        return StringPlaceholders.builder()
//                .addPlaceholder("currentPage", gui.getPage())
//                .addPlaceholder("prevPage", gui.getPrevPage())
//                .addPlaceholder("nextPage", gui.getNextPage())
//                .addPlaceholder("totalPages", gui.getTotalPages());
//    }

}
