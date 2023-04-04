package xyz.oribuin.eternaltags.util;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.util.nms.SkullUtils;

import java.util.Arrays;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
    }

    public ItemBuilder setMaterial(Material material) {
        this.item.setType(material);
        return this;
    }

    /**
     * Set the ItemStack's Display Name.
     *
     * @param text The text.
     * @return Item.Builder.
     */
    @SuppressWarnings("deprecation")
    public ItemBuilder setName(@Nullable String text) {
        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null || text == null)
            return this;

        meta.setDisplayName(text);
        item.setItemMeta(meta);

        return this;
    }

    /**
     * Set the ItemStack's Lore
     *
     * @param lore The lore
     * @return Item.Builder.
     */
    @SuppressWarnings("deprecation")
    public ItemBuilder setLore(@Nullable List<String> lore) {
        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null || lore == null)
            return this;

        meta.setLore(lore);
        item.setItemMeta(meta);

        return this;
    }

    /**
     * Set the ItemStack's Lore
     *
     * @param lore The lore
     * @return Item.Builder.
     */
    @SuppressWarnings("deprecation")
    public ItemBuilder setLore(@Nullable String... lore) {
        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null || lore == null)
            return this;

        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);

        return this;
    }

    /**
     * Set the ItemStack amount.
     *
     * @param amount The amount of items.
     * @return Item.Builder
     */
    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    /**
     * Add an enchantment to an item.
     *
     * @param ench  The enchantment.
     * @param level The level of the enchantment
     * @return Item.Builder
     */
    public ItemBuilder addEnchant(Enchantment ench, int level) {
        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null)
            return this;

        meta.addEnchant(ench, level, true);
        item.setItemMeta(meta);

        return this;
    }

    /**
     * Remove an enchantment from an Item
     *
     * @param ench The enchantment.
     * @return Item.Builder
     */
    public ItemBuilder removeEnchant(Enchantment ench) {
        item.removeEnchantment(ench);
        return this;
    }

    /**
     * Remove and reset the ItemStack's Flags
     *
     * @param flags The ItemFlags.
     * @return Item.Builder
     */
    public ItemBuilder setFlags(ItemFlag[] flags) {
        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null)
            return this;

        meta.removeItemFlags(ItemFlag.values());
        meta.addItemFlags(flags);
        item.setItemMeta(meta);

        return this;
    }


    /**
     * Change the item's unbreakable status.
     *
     * @param unbreakable true if unbreakable
     * @return Item.Builder
     */
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null)
            return this;

        meta.setUnbreakable(unbreakable);
        return this;
    }

    /**
     * Set an item to glow.
     *
     * @return Item.Builder
     */
    public ItemBuilder glow(boolean b) {
        if (!b)
            return this;

        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null)
            return this;

        meta.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);

        return this;
    }

    /**
     * Set an item's NBT Values
     *
     * @param key   The key to the nbt
     * @param value The value of the nbt
     * @return Item.Builder
     */
    public ItemBuilder setNBT(Plugin plugin, String key, String value) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return this;

        final PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, key), PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return this;
    }

    /**
     * Set an item's NBT Values
     *
     * @param key   The key to the nbt
     * @param value The value of the nbt
     * @return Item.Builder
     */
    public ItemBuilder setNBT(Plugin plugin, String key, int value) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return this;

        final PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, key), PersistentDataType.INTEGER, value);
        item.setItemMeta(meta);
        return this;
    }

    /**
     * Set an item's NBT Values
     *
     * @param key   The key to the nbt
     * @param value The value of the nbt
     * @return Item.Builder
     */
    public ItemBuilder setNBT(Plugin plugin, String key, double value) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return this;

        final PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(new NamespacedKey(plugin, key), PersistentDataType.DOUBLE, value);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setTexture(@Nullable String texture) {
        if (item.getType() != Material.PLAYER_HEAD || texture == null)
            return this;

        final SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        if (skullMeta == null)
            return this;

        SkullUtils.setSkullTexture(skullMeta, texture);
        item.setItemMeta(skullMeta);

        return this;
    }

    public ItemBuilder setOwner(OfflinePlayer owner) {
        if (item.getType() != Material.PLAYER_HEAD)
            return this;

        if (owner == null)
            return this;

        final SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        if (skullMeta == null)
            return this;

        skullMeta.setOwningPlayer(owner);
        item.setItemMeta(skullMeta);
        return this;
    }

    public ItemBuilder setModel(int model) {
        ItemMeta meta = this.item.getItemMeta();
        if (meta == null || model == -1)
            return this;

        meta.setCustomModelData(model);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder addPotionEffect(PotionEffectType effectType, int duration, int amp) {
        if (!(this.item.getItemMeta() instanceof PotionMeta meta))
            return this;

        meta.addCustomEffect(new PotionEffect(effectType, duration, amp), true);
        item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setPotionColor(Color color) {
        if (!(this.item.getItemMeta() instanceof PotionMeta meta))
            return this;

        meta.setColor(color);
        item.setItemMeta(meta);
        return this;
    }

    /**
     * Finalize the Item Builder and create the stack.
     *
     * @return The ItemStack
     */
    public ItemStack create() {
        return item;
    }

}
