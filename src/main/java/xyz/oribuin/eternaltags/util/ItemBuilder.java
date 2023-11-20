package xyz.oribuin.eternaltags.util;


import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.util.nms.SkullUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "deprecation"})
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
    public ItemBuilder name(@Nullable String text) {
        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null || text == null)
            return this;

        meta.setDisplayName(text);
        this.item.setItemMeta(meta);

        return this;
    }

    /**
     * Set the ItemStack's Lore
     *
     * @param lore The lore
     * @return Item.Builder.
     */
    public ItemBuilder lore(@Nullable List<String> lore) {
        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null || lore == null)
            return this;

        meta.setLore(lore);
        this.item.setItemMeta(meta);
        return this;
    }

    /**
     * Set the ItemStack's Lore
     *
     * @param lore The lore
     * @return Item.Builder.
     */
    public ItemBuilder lore(@Nullable String... lore) {
        return this.lore(Arrays.asList(lore));
    }

    /**
     * Set the ItemStack amount.
     *
     * @param amount The amount of items.
     * @return Item.Builder
     */
    public ItemBuilder amount(int amount) {
        this.item.setAmount(amount);
        return this;
    }

    /**
     * Add an enchantment to an item.
     *
     * @param ench  The enchantment.
     * @param level The level of the enchantment
     * @return Item.Builder
     */
    public ItemBuilder enchant(Enchantment ench, int level) {
        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null) return this;

        meta.addEnchant(ench, level, true);
        this.item.setItemMeta(meta);

        return this;
    }

    /**
     * Add multiple enchantments to an item.
     *
     * @param enchantments The enchantments.
     * @return Item.Builder
     */

    public ItemBuilder enchant(Map<Enchantment, Integer> enchantments) {
        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null) return this;

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }

        this.item.setItemMeta(meta);
        return this;
    }

    /**
     * Remove an enchantment from an Item
     *
     * @param ench The enchantment.
     * @return Item.Builder
     */
    public ItemBuilder remove(Enchantment ench) {
        this.item.removeEnchantment(ench);
        return this;
    }

    /**
     * Remove and reset the ItemStack's Flags
     *
     * @param flags The ItemFlags.
     * @return Item.Builder
     */
    public ItemBuilder flags(ItemFlag[] flags) {
        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null) return this;

        meta.removeItemFlags(ItemFlag.values());
        meta.addItemFlags(flags);
        this.item.setItemMeta(meta);

        return this;
    }


    /**
     * Change the item's unbreakable status.
     *
     * @param unbreakable true if unbreakable
     * @return Item.Builder
     */
    public ItemBuilder unbreakable(boolean unbreakable) {
        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null) return this;

        meta.setUnbreakable(unbreakable);
        item.setItemMeta(meta);
        return this;
    }

    /**
     * Set an item to glow.
     *
     * @return Item.Builder
     */
    public ItemBuilder glow(boolean b) {
        if (!b) return this;

        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null) return this;

        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        this.item.setItemMeta(meta);

        return this;
    }

    /**
     * Apply a texture to a skull.
     *
     * @param texture The texture.
     * @return Item.Builder
     */
    public ItemBuilder texture(@Nullable String texture) {
        if (item.getType() != Material.PLAYER_HEAD || texture == null)
            return this;

        final SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        if (skullMeta == null)
            return this;

        SkullUtils.setSkullTexture(skullMeta, texture);
        this.item.setItemMeta(skullMeta);
        return this;
    }

    /**
     * Set the owner of a skull.
     *
     * @param owner The owner.
     * @return Item.Builder
     */
    public ItemBuilder owner(OfflinePlayer owner) {
        if (item.getType() != Material.PLAYER_HEAD)
            return this;

        final SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
        if (skullMeta == null || owner == null || skullMeta.getOwningPlayer() != null)
            return this;

        System.out.println("Setting owner to " + owner.getName());

        skullMeta.setOwningPlayer(owner);
        this.item.setItemMeta(skullMeta);
        return this;
    }

    public ItemBuilder model(int model) {
        final ItemMeta meta = this.item.getItemMeta();
        if (meta == null || model <= 0)
            return this;

        meta.setCustomModelData(model);
        this.item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder potion(PotionEffectType effectType, int duration, int amp) {
        if (!(this.item.getItemMeta() instanceof PotionMeta meta))
            return this;

        meta.addCustomEffect(new PotionEffect(effectType, duration, amp), true);
        this.item.setItemMeta(meta);
        return this;
    }

    public ItemBuilder color(Color color) {
        if (this.item.getItemMeta() instanceof PotionMeta meta) {
            meta.setColor(color);
            this.item.setItemMeta(meta);
        }

        if (this.item.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            this.item.setItemMeta(meta);
        }

        return this;
    }

    /**
     * Finalize the Item Builder and create the stack.
     *
     * @return The ItemStack
     */
    public ItemStack build() {
        return this.item;
    }

}