package dev.oribuin.eternaltags.util;

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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "deprecation"})
public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    /**
     * Create a new Item Builder with a Material.
     *
     * @param material The Material.
     */
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = this.item.getItemMeta();
    }

    /**
     * Create a new Item Builder with an existing ItemStack.
     *
     * @param item The ItemStack.
     */
    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    /**
     * Set the ItemStack's Material.
     *
     * @param material The Material.
     * @return Item.Builder.
     */
    public ItemBuilder material(Material material) {
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
        this.meta.setDisplayName(text);
        return this;
    }

    /**
     * Set the ItemStack's Lore
     *
     * @param lore The lore
     * @return Item.Builder.
     */
    public ItemBuilder lore(@Nullable List<String> lore) {
        this.meta.setLore(lore);
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
        this.meta.addEnchant(ench, level, true);
        return this;
    }

    /**
     * Add multiple enchantments to an item.
     *
     * @param enchantments The enchantments.
     * @return Item.Builder
     */

    public ItemBuilder enchant(Map<Enchantment, Integer> enchantments) {
        enchantments.forEach(this::enchant);
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
        this.meta.addItemFlags(ItemFlag.values());
        return this;
    }


    /**
     * Change the item's unbreakable status.
     *
     * @param unbreakable true if unbreakable
     * @return Item.Builder
     */
    public ItemBuilder unbreakable(boolean unbreakable) {
        this.meta.setUnbreakable(unbreakable);
        return this;
    }

    /**
     * Set an item to glow.
     *
     * @return Item.Builder
     */
    public ItemBuilder glow(boolean b) {
        if (!b) return this;

        this.meta.setEnchantmentGlintOverride(true);
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

        if (!(this.meta instanceof SkullMeta skullMeta)) return this;
        
        // TODO: SkullUtils.setSkullTexture(skullMeta, texture);
        return this;
    }

    /**
     * Set the owner of a skull.
     *
     * @param owner The owner.
     * @return Item.Builder
     */
    public ItemBuilder owner(OfflinePlayer owner) {
        if (item.getType() != Material.PLAYER_HEAD) return this;
        if (owner == null) return this;
        if (!(this.meta instanceof SkullMeta skullMeta)) return this;

        skullMeta.setOwningPlayer(owner);
        return this;
    }

    public ItemBuilder model(int model) {
        this.meta.setCustomModelData(model);
        return this;
    }

    public ItemBuilder potion(PotionEffectType effectType, int duration, int amp) {
        if (!(this.meta instanceof PotionMeta potionMeta)) return this;

        potionMeta.addCustomEffect(new PotionEffect(effectType, duration, amp), true);
        return this;
    }

    public ItemBuilder color(Color color) {
        if (this.meta instanceof PotionMeta potionMeta) {
            potionMeta.setColor(color);
        }

        if (this.item.getItemMeta() instanceof LeatherArmorMeta leatherArmorMeta) {
            leatherArmorMeta.setColor(color);
        }

        return this;
    }

    /**
     * Finalize the Item Builder and create the stack.
     *
     * @return The ItemStack
     */
    public ItemStack build() {
        this.item.setItemMeta(this.meta);
        return this.item;
    }

}