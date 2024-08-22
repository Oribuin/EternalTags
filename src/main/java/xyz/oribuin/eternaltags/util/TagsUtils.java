package xyz.oribuin.eternaltags.util;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.utils.HexUtils;
import dev.rosewood.rosegarden.utils.NMSUtil;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.manager.LocaleManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class TagsUtils {

    private static MiniMessage MINIMESSAGE;
    private static LegacyComponentSerializer LEGACY;

    private TagsUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Get the display name of a tag
     *
     * @param text The text to format
     *
     * @return The formatted text
     */
    public static String colorAsString(String text) {
        if (text == null) return "";

        if (Setting.TAG_FORMATTING.getString().equalsIgnoreCase("mini_message")) {
            if (MINIMESSAGE == null) MINIMESSAGE = MiniMessage.miniMessage();
            if (LEGACY == null) LEGACY = LegacyComponentSerializer.legacySection();

            return LEGACY.serialize(MINIMESSAGE.deserialize(text));
        }

        return HexUtils.colorify(text);
    }

    /**
     * Convert a location to the center of the block
     *
     * @param location The location to convert
     *
     * @return The center of the block
     */
    public static Location center(Location location) {
        Location loc = location.getBlock().getLocation().clone();
        loc.add(0.5, 0.5, 0.5);
        loc.setYaw(180f);
        loc.setPitch(0f);
        return loc;
    }

    /**
     * Get a bukkit color from a hex code
     *
     * @param hex The hex code
     *
     * @return The bukkit color
     */
    public static Color fromHex(String hex) {
        try {
            if (hex == null || hex.isEmpty())
                return Color.WHITE;

            java.awt.Color decoded = java.awt.Color.decode(hex);
            return Color.fromRGB(decoded.getRed(), decoded.getGreen(), decoded.getBlue());
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }

    /**
     * Get the total number of spare slots in a player's inventory
     *
     * @param player The player
     *
     * @return The amount of empty slots.
     */
    public static int getSpareSlots(Player player) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 36; i++)
            slots.add(i);

        return (int) slots.stream().map(integer -> player.getInventory().getItem(integer))
                .filter(itemStack -> itemStack == null || itemStack.getType() == Material.AIR)
                .count();
    }

    /**
     * Gets a location as a string key
     *
     * @param location The location
     *
     * @return the location as a string key
     *
     * @author Esophose
     */
    public static String locationAsKey(Location location) {
        return String.format("%s;%.2f;%.2f;%.2f", location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    /**
     * Get a location from a string key
     *
     * @param key The key
     *
     * @return The location
     */
    public static Location locationFromKey(String key) {
        if (key == null || key.isEmpty())
            return null;

        // format is world;x;y;z
        String[] split = key.split(";");
        if (split.length != 4)
            return null;

        return new Location(Bukkit.getWorld(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]));
    }

    /**
     * Format a material name through this long method
     *
     * @param material The material
     *
     * @return The material name.
     */
    public static String format(Material material) {
        return WordUtils.capitalizeFully(material.name().toLowerCase().replace("_", " "));
    }

    /**
     * Deserialize an ItemStack from a CommentedConfigurationSection with placeholders
     *
     * @param section      The section to deserialize from
     * @param sender       The CommandSender to apply placeholders from
     * @param key          The key to deserialize from
     * @param placeholders The placeholders to apply
     *
     * @return The deserialized ItemStack
     */
    @Nullable
    public static ItemStack deserialize(
            CommentedConfigurationSection section,
            CommandSender sender,
            String key,
            StringPlaceholders placeholders
    ) {
        LocaleManager locale = EternalTags.getInstance().getManager(LocaleManager.class);
        Material material = Material.getMaterial(locale.format(sender, section.getString(key + ".material"), placeholders), false);
        if (material == null) return null;

        // Load enchantments
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        ConfigurationSection enchantmentSection = section.getConfigurationSection(key + ".enchantments");
        if (enchantmentSection != null) {
            for (String enchantmentKey : enchantmentSection.getKeys(false)) {
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantmentKey.toLowerCase()));
                if (enchantment == null) continue;

                enchantments.put(enchantment, enchantmentSection.getInt(enchantmentKey, 1));
            }
        }

        // Load potion item flags
        ItemFlag[] flags = section.getStringList(key + ".flags").stream()
                .map(ItemFlag::valueOf)
                .toArray(ItemFlag[]::new);

        // Load offline player texture
        String owner = section.getString(key + ".owner");
        OfflinePlayer offlinePlayer = null;
        if (owner != null) {
            if (owner.equalsIgnoreCase("self") && sender instanceof Player player) {
                offlinePlayer = player;
            } else {
                offlinePlayer = NMSUtil.isPaper()
                        ? Bukkit.getOfflinePlayerIfCached(owner)
                        : Bukkit.getOfflinePlayer(owner);
            }
        }

        return new ItemBuilder(material)
                .name(locale.format(sender, section.getString(key + ".name"), placeholders))
                .amount(Math.min(1, section.getInt(key + ".amount", 1)))
                .lore(locale.format(sender, section.getStringList(key + ".lore"), placeholders))
                .flags(flags)
                .glow(section.getBoolean(key + ".glow", false))
                .unbreakable(section.getBoolean(key + ".unbreakable", false))
                .model(toInt(locale.format(sender, section.getString(key + ".model-data", "0"), placeholders)))
                .enchant(enchantments)
                .texture(locale.format(sender, section.getString(key + ".texture"), placeholders))
                .color(fromHex(section.getString(key + ".potion-color")))
                .owner(offlinePlayer)
                .build();
    }

    /**
     * Deserialize an ItemStack from a CommentedConfigurationSection
     *
     * @param section The section to deserialize from
     * @param key     The key to deserialize from
     *
     * @return The deserialized ItemStack
     */
    @Nullable
    public static ItemStack deserialize(CommentedConfigurationSection section, String key) {
        return deserialize(section, null, key, StringPlaceholders.empty());
    }

    /**
     * Deserialize an ItemStack from a CommentedConfigurationSection with placeholders
     *
     * @param section The section to deserialize from
     * @param sender  The CommandSender to apply placeholders from
     * @param key     The key to deserialize from
     *
     * @return The deserialized ItemStack
     */
    @Nullable
    public static ItemStack deserialize(CommentedConfigurationSection section, CommandSender sender, String key) {
        return deserialize(section, sender, key, StringPlaceholders.empty());
    }

    /**
     * Parse an integer from an object safely
     *
     * @param object The object
     *
     * @return The integer
     */
    private static int toInt(String object) {
        try {
            return Integer.parseInt(object);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player The player to format the string for
     * @param text   The string to format
     *
     * @return The formatted string
     */
    public static String format(Player player, String text) {
        return format(player, text, StringPlaceholders.empty());
    }

    /**
     * Format a string with placeholders and color codes
     *
     * @param player       The player to format the string for
     * @param text         The text to format
     * @param placeholders The placeholders to replace
     *
     * @return The formatted string
     */
    public static String format(Player player, String text, StringPlaceholders placeholders) {
        if (text == null)
            return null;

        return HexUtils.colorify(PlaceholderAPI.setPlaceholders(player, placeholders.apply(text)));
    }

    /**
     * Parse a list of strings from 1-1 to a stringlist
     *
     * @param list The list to parse
     *
     * @return The parsed list
     */
    @SuppressWarnings("unchecked")
    public static List<Integer> parseList(List<String> list) {
        List<Integer> newList = new ArrayList<>();
        for (String s : list) {
            String[] split = s.split("-");
            if (split.length != 2) {
                continue;
            }

            newList.addAll(getNumberRange(Integer.parseInt(split[0]), Integer.parseInt(split[1])));
        }

        return newList;
    }

    /**
     * Get a range of numbers as a list
     *
     * @param start The start of the range
     * @param end   The end of the range
     *
     * @return A list of numbers
     */
    public static List<Integer> getNumberRange(int start, int end) {
        if (start == end) {
            return List.of(start);
        }

        List<Integer> list = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            list.add(i);
        }

        return list;
    }

    /**
     * Format a string list into a single string.
     *
     * @param list      The strings being converted
     * @param delimiter The delimiter between each string
     *
     * @return the converted string.
     */
    public static String formatList(List<String> list, String delimiter) {
        return String.join(delimiter, list);
    }

    /**
     * Get an enum from a string value
     *
     * @param enumClass The enum class
     * @param name      The name of the enum
     * @param <T>       The enum type
     *
     * @return The enum
     */
    public static <T extends Enum<T>> T getEnum(Class<T> enumClass, String name) {
        if (name == null)
            return null;

        try {
            return Enum.valueOf(enumClass, name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }

        return null;
    }

    /**
     * Get an enum from a string value
     *
     * @param enumClass The enum class
     * @param name      The name of the enum
     * @param def       The default enum
     * @param <T>       The enum type
     *
     * @return The enum
     */
    public static <T extends Enum<T>> T getEnum(Class<T> enumClass, String name, T def) {
        if (name == null)
            return def;

        try {
            return Enum.valueOf(enumClass, name.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }

        return def;
    }

    public static byte[] serializeItem(ItemStack itemStack) {
        if (itemStack == null)
            return new byte[0];

        byte[] data = new byte[0];
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(stream)) {
            oos.writeObject(itemStack);
            data = stream.toByteArray();
        } catch (IOException ignored) {
        }

        return data;
    }

    @Nullable
    public static ItemStack deserializeItem(byte[] data) {
        if (data == null || data.length == 0)
            return null;

        ItemStack itemStack = null;
        try (ByteArrayInputStream stream = new ByteArrayInputStream(data);
             BukkitObjectInputStream ois = new BukkitObjectInputStream(stream)) {
            itemStack = (ItemStack) ois.readObject();
        } catch (IOException | ClassNotFoundException ignored) {
        }

        return itemStack;
    }

    /**
     * Load the icon type from the configuration file to determine how it is serialized
     *
     * @param section The configuration section
     * @param key     The key to the item
     *
     * @return The item stack
     */
    public static ItemStack getMultiDeserializedItem(CommentedConfigurationSection section, String key) {
        Object icon = section.get(key + ".icon");
        if (icon != null) {
            // Read the material from the string
            if (icon instanceof String iconString) {
                Material material = Material.matchMaterial(iconString);
                return material != null ? new ItemStack(material) : new ItemStack(Material.STONE);
            }

            // Read from a configuration section
            CommentedConfigurationSection iconSection = section.getConfigurationSection(key + ".icon");
            if (iconSection != null && !iconSection.getKeys(false).isEmpty()) {
                return TagsUtils.deserialize(section, key + ".icon");
            }

            // Read from a byte array
            if (icon instanceof byte[] iconBytes) {
                return TagsUtils.deserializeItem(iconBytes);
            }
        }

        return new ItemStack(Material.STONE);
    }

    /**
     * Create a file in a folder from the plugin's resources
     *
     * @param rosePlugin The plugin
     * @param folders    The folders
     *
     * @return The file
     */
    @NotNull
    public static File createFile(RosePlugin rosePlugin, String... folders) {
        File file = new File(rosePlugin.getDataFolder(), String.join("/", folders)); // Create the file
        if (file.exists())
            return file;

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        String path = String.join("/", folders);
        try (InputStream stream = rosePlugin.getResource(path)) {
            if (stream == null) {
                file.createNewFile();
                return file;
            }

            Files.copy(stream, Paths.get(file.getAbsolutePath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return file;
    }

    /**
     * Relocate a file to a new folder and delete the original
     *
     * @param original  The original file
     * @param newFolder The new folder
     */
    public static void relocateFile(File original, File newFolder) {
        if (!newFolder.exists()) {
            newFolder.mkdirs();
        }

        File newFile = new File(newFolder, original.getName());
        if (newFile.exists()) {
            newFile.delete();
        }

        try {
            Files.copy(original.toPath(), newFile.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        original.delete();
    }

    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
