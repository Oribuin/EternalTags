package dev.oribuin.eternaltags.command.impl;

import dev.oribuin.eternaltags.manager.TagsManager;
import dev.oribuin.eternaltags.obj.Tag;
import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConvertCommand extends BaseRoseCommand {

    public ConvertCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context) {
        TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        CommandSender sender = context.getSender();

        File original = new File(this.rosePlugin.getDataFolder(), "tags.yml");
        File target = new File(TagsManager.TAGS_FOLDER.toFile(), "converted.yml");

        try {
            if (!target.exists()) target.createNewFile();
        } catch (IOException ignored) {
        }

        CommentedFileConfiguration originalConfig = CommentedFileConfiguration.loadConfiguration(original);
        CommentedConfigurationSection tagSection = originalConfig.getConfigurationSection("tags");
        if (tagSection == null) {
            sender.sendMessage("no tags section in original tags.yml");
            return;
        }

        LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand();
        MiniMessage miniMessage = MiniMessage.miniMessage();

        Map<String, Tag> result = new HashMap<>();
        for (String key : tagSection.getKeys(false)) {
            String name = tagSection.getString(key + ".name");
            String content = tagSection.getString(key + ".tag");
            String permission = tagSection.getString(key + ".permission");
            if (name == null || content == null) continue;

            // okay lets convert the content
            TextComponent legacySerialized = serializer.deserialize(content);
            String miniMessageSerialized = miniMessage.serialize(legacySerialized);

            Tag tag = new Tag(target.toPath(), key, name, miniMessageSerialized);
            tag.setPermission(permission);
            result.put(key, tag);
        }
        
        manager.getCachedTags().putAll(result);
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("convert")
                .descriptionKey("command-convert-description")
                .permission("eternaltags.convert")
                .build();
    }

}
