package dev.oribuin.eternaltags.config;

import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.RoseSetting;
import dev.rosewood.rosegarden.config.RoseSettingSerializer;
import dev.rosewood.rosegarden.config.RoseSettingSerializers;
import dev.oribuin.eternaltags.EternalTags;

import java.util.ArrayList;
import java.util.List;

import static dev.rosewood.rosegarden.config.RoseSettingSerializers.*;

/**
 * The general settings for the plugin.
 */
public class Setting {
    
    private static final List<RoseSetting<?>> KEYS = new ArrayList<>();

    /**
     * The option for checking if the plugin should remove a tag if the player doesn't have permission to use it.
     */
    public static RoseSetting<Boolean> REMOVE_INACCESSIBLE = create(
            "remove-inaccessible-tags", RoseSettingSerializers.BOOLEAN, false,
            "Should a tag be automatically removed if the player doesn't have permission to use it?",
            " ",
            "This is recommended if you are giving players temporary tags. May have a slight performance cost"
    );

    /**
     * The option for checking if the plugin should apply PlaceholderAPI placeholders to the chat format.
     */
    public static RoseSetting<Boolean> CHAT_PLACEHOLDERS = create(
            "chat-placeholders", RoseSettingSerializers.BOOLEAN, false,
            "Should the plugin change the Chat Format to allow PlaceholderAPI placeholders to be used?",
            "It is recommended to enable this if you are using EssentialsXChat or a chat plugin that does not support PlaceholderAPI.",
            "It's not recommended for you to enable this if your chat plugin already supports PlaceholderAPI (Most do)."
    );

    /**
     * The option for checking if the plugin should clear the player's tag when they re-equip the same tag.
     */
    public static RoseSetting<String> FORMATTED_PLACEHOLDER = create(
            "formatted-placeholder", RoseSettingSerializers.STRING, "None",
            "The placeholder that will show when the player has no active tag."
    );

    /**
     * The option for checking if the plugin should clear the player's tag when they re-equip the same tag.
     */
    public static RoseSetting<String> STYLE_FORMAT = create(
            "style-format", STRING, "MINI_MESSAGE",
            "Whether the plugin should use LEGACY or MINI_MESSAGE formatting for tags.",
            "LEGACY: Uses the traditional '&' color codes for tags.",
            "MINI_MESSAGE: Uses the MiniMessage library for tags (Recommended)."
    );

    /**
     * The option for checking if the plugin should clear the player's tag when they re-equip the same tag.
     */
    public static RoseSetting<String> TAG_FORMATTING = create(
            "tag-formatting", STRING, "%tag%",
            "The formatting that will be used when displaying tags by the plugin.",
            "",
            "Use this to establish a prefix and suffix for the tag in the placeholder.",
            "Example: 'tag-formatting: '%tag% <white><reset>' to add a space and white color on the end of the tag."
    );
    
    // TODO: save-data-sql
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
        RoseSetting<T> setting = RoseSetting.backed(EternalTags.get(), key, serializer, defaultValue, comments);
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
        RoseSetting<CommentedConfigurationSection> setting = RoseSetting.backedSection(EternalTags.get(), key, comments);
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

}
