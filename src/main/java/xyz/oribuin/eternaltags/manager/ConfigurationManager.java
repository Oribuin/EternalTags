package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.config.RoseSetting;
import dev.rosewood.rosegarden.manager.AbstractConfigurationManager;
import xyz.oribuin.eternaltags.EternalTags;

public class ConfigurationManager extends AbstractConfigurationManager {

    public ConfigurationManager(RosePlugin rosePlugin) {
        super(rosePlugin, Setting.class);
    }

    @Override
    protected String[] getHeader() {
        return new String[]{
                "___________ __                             ._____________",
                "\\_   _____//  |_  ___________  ____ _____  |  \\__    ___/____     ____  ______",
                " |    __)_\\   __\\/ __ \\_  __ \\/    \\\\__  \\ |  | |    |  \\__  \\   / ___\\/  ___/",
                " |        \\|  | \\  ___/|  | \\/   |  \\/ __ \\|  |_|    |   / __ \\_/ /_/  >___ \\ ",
                "/_______  /|__|  \\___  >__|  |___|  (____  /____/____|  (____  /\\___  /____  >",
                "        \\/           \\/           \\/     \\/                  \\//_____/     \\/ "
        };
    }

    public enum Setting implements RoseSetting {
        // Default Tag Options
        DEFAULT_TAG("default-tag", "none", "The tag that will show when player does not have an active tag.", "Set to 'none' to disable.", "Set to 'random' to apply a random tag"),
        DEFAULT_TAG_GROUPS("default-tag-groups", null, "The groups that will be applied to the player when they join the server.", "Set to 'none' to disable.", "Set to 'random' to apply a random tag", "This requires vault and a vault supported permission plugin."),
        DEFAULT_TAG_GROUP_DEFAULT("default-tag-groups.default", "none"),
        REMOVE_TAGS("remove-inaccessible-tags", false, "Should a tag be automatically removed if the player doesn't have permission to use it?"),

        // Formatting
        CHAT_PLACEHOLDERS("chat-placeholders", false, "Should the plugin change the Chat Format to allow PlaceholderAPI placeholders to be used?",
                "It is recommended to enable this if you are using EssentialsXChat or a chat plugin that does not support PlaceholderAPI.",
                "It's not recommended for you to enable this if your chat plugin already supports PlaceholderAPI (Most do)."),
        FORMATTED_PLACEHOLDER("formatted-placeholder", "None", "The placeholder that will show when the player has no active tag."),
        TAG_UNLOCKED_FORMAT("tag-unlocked-format", "&a&lUnlocked", "The format that will show when the player has the tag unlocked."),
        TAG_LOCKED_FORMAT("tag-locked-format", "&c&lLocked", "The format that will show when the player has the tag locked."),
        TAG_PREFIX("tag-prefix", "", "The prefix that will be added in front of the tag in the placeholder"),
        TAG_SUFFIX("tag-suffix", "", "The suffix that will be added after the tag in the placeholder"),
        TAG_FORMATTING("tag-formatting", "LEGACY", "The formatting that will be used when displaying tags by the plugin.", "Available options: LEGACY, MINI_MESSAGE"),
        DESCRIPTION_DELIMITER("description-delimiter", "\n", "The delimiter that will be used for %eternaltags_tag_description%"),

        // Other Options
        RE_EQUIP_CLEAR("reequip-clear", false, "Should the player's tag be cleared when they re-equip the same tag?"),
        CACHE_GUI_TAGS("cache-gui-tags", true, "Should the tag items be cached? (Keeps the items in memory instead of creating them every time the GUI is opened)",
                "This will reduce the amount of lag when opening the GUI, but will use more memory.",
                "This will also make tags with placeholders not update until the plugin is reloaded."
        ),

        CACHE_GUI_CATEGORIES("cache-gui-categories", false, "Should the category items be cached? (Keeps the items in memory instead of creating them every time the GUI is opened)",
                "This will reduce the amount of lag when opening the GUI, but will use more memory.",
                "This will also make categories with placeholders not update until the plugin is reloaded."
        ),
        OPEN_CATEGORY_GUI_FIRST("open-category-gui-first", false, "Should the category GUI be opened first when a player types /tags?"),

        // Data Systems
        MYSQL_TAGDATA("save-tagdata-sql", false, "Should the tag data be stored in a MySQL/SQLite database? (Tags that would be saved in tags.yml)"),
        PLUGIN_MESSAGES("plugin-messaging", null, "Should the plugin use plugin messaging to communicate between servers? (Requires BungeeCord)"),
        PLUGIN_MESSAGING_RELOAD("plugin-messaging.reload", false, "Should /tags reload run on all servers? (Requires BungeeCord)"),

        ; // End of settings

        private final String key;
        private final Object defaultValue;
        private final String[] comments;
        private Object value = null;

        Setting(String key, Object defaultValue, String... comments) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.comments = comments != null ? comments : new String[0];
        }

        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public Object getDefaultValue() {
            return this.defaultValue;
        }

        @Override
        public String[] getComments() {
            return this.comments;
        }

        @Override
        public Object getCachedValue() {
            return this.value;
        }

        @Override
        public void setCachedValue(Object value) {
            this.value = value;
        }

        @Override
        public CommentedFileConfiguration getBaseConfig() {
            return EternalTags.getInstance().getManager(ConfigurationManager.class).getConfig();
        }
    }

}
