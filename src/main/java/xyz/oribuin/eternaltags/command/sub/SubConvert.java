package xyz.oribuin.eternaltags.command.sub;

import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.command.SubCommand;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@SubCommand.Info(
        names = {"convert"},
        usage = "/tags convert",
        permission = "eternaltags.convert"
)
public class SubConvert extends SubCommand {

    private final EternalTags plugin;
    private final MessageManager msg;
    private final TagManager tagManager;

    public SubConvert(EternalTags plugin) {
        this.plugin = plugin;
        this.msg = this.plugin.getManager(MessageManager.class);
        this.tagManager = this.plugin.getManager(TagManager.class);
    }

    @Override
    public void executeArgument(CommandSender sender, String[] args) {

        if (args.length != 2) {
            this.msg.send(sender, "invalid-arguments", StringPlaceholders.single("usage", this.getInfo().usage()));
            return;
        }

        final Optional<ConvertablePlugin> pluginOptional = Arrays.stream(ConvertablePlugin.values())
                .filter(pl -> pl.name().equalsIgnoreCase(args[1]))
                .findAny();

        if (!pluginOptional.isPresent()) {
            this.msg.send(sender, "invalid-plugin");
            return;
        }

        final List<Tag> tags = new ArrayList<>();
        switch (pluginOptional.get()) {
            case DELUXETAGS:
                tags.addAll(this.convertDeluxeTags());
                break;
            case CIFYTAGS:
                tags.addAll(this.convertCIFYTags());
                break;
        }

        msg.send(sender, "converted-tags", StringPlaceholders.single("total", tags.size()));
    }

    /**
     * Convert all the tags from DeluxeTags to EternalTags
     * absolutely yoink their tags
     *
     * @return A list of converted tags.
     */
    private List<Tag> convertDeluxeTags() {
        final File file = new File(new File(this.plugin.getDataFolder().getParentFile(), "DeluxeTags"), "config.yml");

        final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        final ConfigurationSection section = config.getConfigurationSection("deluxetags");

        if (section == null) {
            return new ArrayList<>();
        }
        final List<Tag> tags = section.getKeys(false)
                .stream()
                .filter(key -> tagManager.getTags().stream().noneMatch(tag -> tag.getId().equalsIgnoreCase(key)))
                .map(key -> {
                    final Tag tag = new Tag(key, StringUtils.capitalize(key), section.getString(key + ".tag"));

                    tag.setDescription(Collections.singletonList(Optional.ofNullable(section.getString(key + ".description")).orElse("")));
                    if (section.getString(key + ".permission") != null) {
                        tag.setPermission(section.getString(key + ".permission"));
                    }

                    if (section.get(key + ".order") != null) {
                        tag.setOrder(section.getInt(key + ".order"));
                    }

                    return tag;
                })
                .collect(Collectors.toList());

        tagManager.saveTags(tags);
        return tags;
    }

    /**
     * Convert all the tags from CIFYTags to EternalTags
     * absolutely yoink their tags
     *
     * @return A list of converted tags.
     */
    private List<Tag> convertCIFYTags() {
        final File file = new File(new File(this.plugin.getDataFolder().getParentFile(), "CIFYTags"), "config.yml");

        final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        final ConfigurationSection section = config.getConfigurationSection("tags");

        if (section == null) {
            return new ArrayList<>();
        }

        final List<Tag> tags = section.getKeys(false)
                .stream()
                .filter(key -> tagManager.getTags().stream().noneMatch(tag -> tag.getId().equalsIgnoreCase(key)))
                .map(key -> {
                    final Tag tag = new Tag(key, StringUtils.capitalize(key), section.getString(key + ".prefix"));

                    tag.setDescription(Collections.singletonList(Optional.ofNullable(section.getString(key + ".description")).orElse("")));
                    if (section.getBoolean(key + ".permission")) {
                        tag.setPermission("cifytags.use." + key.toLowerCase());
                    }

                    return tag;
                })
                .collect(Collectors.toList());

        tagManager.saveTags(tags);
        return tags;
    }

    private enum ConvertablePlugin {
        DELUXETAGS, CIFYTAGS
    }

}
