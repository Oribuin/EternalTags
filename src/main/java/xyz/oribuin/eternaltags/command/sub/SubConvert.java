package xyz.oribuin.eternaltags.command.sub;

import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.command.SubCommand;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SubCommand.Info(
        names = {"convert"},
        usage = "/tags convert",
        permission = "eternaltags.clear",
        command = CmdTags.class
)
public class SubConvert extends SubCommand {

    private final EternalTags plugin = (EternalTags) this.getOriPlugin();
    private final MessageManager msg = this.plugin.getManager(MessageManager.class);
    private final TagManager tagManager = this.plugin.getManager(TagManager.class);

    public SubConvert(EternalTags plugin, CmdTags command) {
        super(plugin, command);
    }

    @Override
    public void executeArgument(CommandSender sender, String[] args) {
        final List<Tag> tags = this.convertDeluxeTags(sender);
        msg.send(sender, "converted-tags", StringPlaceholders.single("total", tags.size()));

    }

    /**
     * Convert all the tags from DeluxeTags to EternalTags
     * absolutely yoink their tags
     *
     * @param sender The command sender
     * @return A list of converted tags.
     */
    private List<Tag> convertDeluxeTags(CommandSender sender) {
        final Plugin deluxeTags = this.plugin.getServer().getPluginManager().getPlugin("DeluxeTags");

        if (deluxeTags == null || !deluxeTags.isEnabled()) {
            msg.sendRaw(sender, "&c&lError &7| &fPlugin requires DeluxeTags to be enabled to do this.");
            return Collections.emptyList();
        }

        final List<Tag> tags = new ArrayList<>();
        final ConfigurationSection config = deluxeTags.getConfig().getConfigurationSection("deluxetags");
        if (config == null) {
            msg.sendRaw(sender, "&c&lError &7| &fFailed to convert tags from DeluxeTags.");
            return Collections.emptyList();
        }

        CompletableFuture.runAsync(() -> {

            for (String key : config.getKeys(false)) {

                if (tagManager.getTags().stream().anyMatch(tag -> tag.getId().equalsIgnoreCase(key))) {
                    return;
                }

                final Tag tag = new Tag(key, StringUtils.capitalize(key), config.getString(key + ".tag"));
                tag.setDescription(config.getStringList(key + ".description"));
                if (config.get(key + ".permission") != null) {
                    tag.setPermission(config.getString(key + ".permission"));
                }

                tags.add(tag);
            }

        }).thenRunAsync(() -> tagManager.saveTags(tags));

        return tags;
    }
}
