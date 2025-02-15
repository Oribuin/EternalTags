package xyz.oribuin.eternaltags.config;

import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.RoseSetting;
import dev.rosewood.rosegarden.config.RoseSettingSerializer;
import dev.rosewood.rosegarden.config.RoseSettingSerializers;
import xyz.oribuin.eternaltags.EternalTags;

import javax.print.DocFlavor;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;

import static dev.rosewood.rosegarden.config.RoseSettingSerializers.*;

/**
 * The general settings for the plugin.
 */
public class Setting {

    private static final List<RoseSetting<?>> KEYS = new ArrayList<>();

    public static final RoseSetting<String> DEFAULT_TAG = create("default-tag", STRING, "none", "The tag that will show when player does not have an active tag.", "Set to 'none' to disable.", "Set to 'random' to apply a random tag");

    public static final RoseSetting<CommentedConfigurationSection> DEFAULT_TAG_GROUPS = create("default-tag-groups", "The groups that will be applied to the player when they join the server.", "Set to 'none' to disable.", "Set to 'random' to apply a random tag", "This requires vault and a vault supported permission plugin.");
    private static final RoseSetting<String> DEFAULT_TAG_GROUP_DEFAULT = create("default-tag-groups.default", STRING, "none");

    public static final RoseSetting<Boolean> REMOVE_TAGS = create("remove-inaccessible-tags", BOOLEAN, false, "Should a tag be automatically removed if the player doesn't have permission to use it?");

    // Formatting
    public static final RoseSetting<Boolean> CHAT_PLACEHOLDERS = create("chat-placeholders", BOOLEAN, false, "Should the plugin change the Chat Format to allow PlaceholderAPI placeholders to be used?",
            "It is recommended to enable this if you are using EssentialsXChat or a chat plugin that does not support PlaceholderAPI.",
            "It's not recommended for you to enable this if your chat plugin already supports PlaceholderAPI (Most do).");

    public static final RoseSetting<String> FORMATTED_PLACEHOLDER = create("formatted-placeholder", STRING, "None", "The placeholder that will show when the player has no active tag.");

    public static final RoseSetting<String> TAG_UNLOCKED_FORMAT = create("tag-unlocked-format", STRING, "&a&lUnlocked", "The format that will show when the player has the tag unlocked.");
    public static final RoseSetting<String> TAG_LOCKED_FORMAT = create("tag-locked-format", STRING, "&c&lLocked", "The format that will show when the player has the tag locked.");
    public static final RoseSetting<String> TAG_PREFIX = create("tag-prefix", STRING, "", "The prefix that will be added in front of the tag in the placeholder");
    public static final RoseSetting<String> TAG_SUFFIX = create("tag-suffix", STRING, "", "The suffix that will be added after the tag in the placeholder");
    public static final RoseSetting<String> DESCRIPTION_DELIMITER = create("description-delimiter", STRING, "\n", "The delimiter that will be used for %eternaltags_tag_description%");

    // Other Options
    public static final RoseSetting<Boolean> RE_EQUIP_CLEAR = create("reequip-clear", BOOLEAN, false, "Should the player's tag be cleared when they re-equip the same tag?");
    public static final RoseSetting<Boolean> CACHE_GUI_TAGS = create("cache-gui-tags", BOOLEAN, true, "Should the tag items be cached? (Keeps the items in memory instead of creating them every time the GUI is opened)",
            "This will reduce the amount of lag when opening the GUI, but will use more memory.",
            "This will also make tags with placeholders not update until the plugin is reloaded."
    );
    public static final RoseSetting<Boolean> OPEN_CATEGORY_GUI_FIRST = create("open-category-gui-first", BOOLEAN, false, "Should the category GUI be opened first when a player types /tags?");
    public static final RoseSetting<Boolean> CACHE_GUI_CATEGORIES = create("cache-gui-categories", BOOLEAN, false, "Should the category items be cached? (Keeps the items in memory instead of creating them every time the GUI is opened)",
            "This will reduce the amount of lag when opening the GUI, but will use more memory.",
            "This will also make categories with placeholders not update until the plugin is reloaded.");


    // Data Systems
    public static final RoseSetting<Boolean> MYSQL_TAGDATA = create("save-tagdata-sql", BOOLEAN, false, "Should the tag data be stored in a MySQL/SQLite database? (Tags that would be saved in tags.yml)");
    private static final RoseSetting<CommentedConfigurationSection> PLUGIN_MESSAGING = create("plugin-messaging", "Should the plugin use plugin messaging to communicate between servers? (Requires BungeeCord)");
    public static final RoseSetting<Boolean> PLUGIN_MESSAGING_RELOAD = create("plugin-messaging.reload", BOOLEAN, false, "Should /tags reload run on all servers? (Requires BungeeCord)");

    /**
     * Establishes a configuration setting for the plugin which will be generated on reload.
     *
     * @param key          The key (path) of the setting
     * @param serializer   The {@link dev.rosewood.rosegarden.config.RoseSettingSerializers} for the setting
     * @param defaultValue The default value of the setting
     * @param comments     The comments for the setting
     * @param <T>          The type of the setting
     * @return The generated {@link dev.rosewood.rosegarden.config.RoseSetting}
     */
    private static <T> RoseSetting<T> create(String key, RoseSettingSerializer<T> serializer, T defaultValue, String... comments) {
        RoseSetting<T> setting = RoseSetting.backed(EternalTags.getInstance(), key, serializer, defaultValue, comments);
        KEYS.add(setting);
        return setting;
    }

    /**
     * Establishes a configuration setting for the plugin which will be generated on reload.
     *
     * @param key      The key (path) of the setting
     * @param comments The comments for the setting
     * @return The generated {@link dev.rosewood.rosegarden.config.RoseSetting}
     */
    private static RoseSetting<CommentedConfigurationSection> create(String key, String... comments) {
        RoseSetting<CommentedConfigurationSection> setting = RoseSetting.backedSection(EternalTags.getInstance(), key, comments);
        KEYS.add(setting);
        return setting;
    }

    /**
     * All the general settings for the plugin.
     *
     * @return The generated {@link dev.rosewood.rosegarden.config.RoseSetting} for the plugin.
     */
    public static List<RoseSetting<?>> getKeys() {
        return KEYS;
    }

    /**
     * Get the header for the configuration file.
     *
     * @return The header for the configuration file.\
     */
    public static String[] getHeader() {
        return new String[]{
                "___________ __                             ._____________",
                "\\_   _____//  |_  ___________  ____ _____  |  \\__    ___/____     ____  ______",
                " |    __)_\\   __\\/ __ \\_  __ \\/    \\\\__  \\ |  | |    |  \\__  \\   / ___\\/  ___/",
                " |        \\|  | \\  ___/|  | \\/   |  \\/ __ \\|  |_|    |   / __ \\_/ /_/  >___ \\ ",
                "/_______  /|__|  \\___  >__|  |___|  (____  /____/____|  (____  /\\___  /____  >",
                "        \\/           \\/           \\/     \\/                  \\//_____/     \\/ "
        };
    }
}