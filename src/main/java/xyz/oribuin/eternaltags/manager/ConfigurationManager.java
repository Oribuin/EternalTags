package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.config.RoseSetting;
import dev.rosewood.rosegarden.manager.AbstractConfigurationManager;
import xyz.oribuin.eternaltags.EternalTags;

public class ConfigurationManager extends AbstractConfigurationManager {

    public enum Setting implements RoseSetting {
        // Tag Settings
        DEFAULT_TAG("default-tag", "none", "The tag that will show when player does not have an active tag.", "Set to 'none' to disable.", "Set to 'random' to apply a random tag"),
        FORMATTED_PLACEHOLDER("formatted-placeholder", "None", "The placeholder that will show when the player has no active tag."),
        TAG_UNLOCKED_FORMAT("tag-unlocked-format", "&a&lUnlocked", "The format that will show when the player has the tag unlocked."),
        TAG_LOCKED_FORMAT("tag-locked-format", "&c&lLocked", "The format that will show when the player has the tag locked."),
        REMOVE_TAGS("remove-inaccessible-tags", false, "Should a tag be automatically removed if the player doesn't have permission to use it?"),
        TAG_PREFIX("tag-prefix", "", "The prefix that will be added in front of the tag in the placeholder"),
        TAG_SUFFIX("tag-suffix", "", "The suffix that will be added after the tag in the placeholder"),
        MYSQL_TAGDATA("save-tagdata-sql", false, "Should the tag data be stored in a MySQL/SQLite database? (Tags that would be saved in tags.yml)");

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

}
