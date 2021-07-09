package xyz.oribuin.eternaltags.command.sub;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.command.SubCommand;

import xyz.oribuin.orilibrary.util.StringPlaceholders;

import java.io.File;
import java.util.ArrayList;
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

    public SubConvert(EternalTags plugin, CmdTags command) {
        super(plugin, command);
    }

    @Override
    public void executeArgument( CommandSender sender,  String[] args) {

        final MessageManager msg = this.plugin.getManager(MessageManager.class);

        final Plugin deluxeTags = Bukkit.getPluginManager().getPlugin("DeluxeTags");

        if (deluxeTags == null) {
            msg.sendRaw(sender, "&c&lError &7| &fPlugin requires DeluxeTags to be enabled to do this.");
            return;
        }

        final FileConfiguration config = YamlConfiguration.loadConfiguration(new File(deluxeTags.getDataFolder(), "config.yml"));
        final ConfigurationSection section = config.getConfigurationSection("deluxetags");
        if (section == null) {
            msg.send(sender, "&c&lError &7| &fFailed to convert tags from DeluxeTags.");
            return;
        }

        final List<Tag> convertedTags = new ArrayList<>();

        CompletableFuture.runAsync(() -> {

            for (String key : section.getKeys(false)) {

                if (this.plugin.getManager(TagManager.class).getTags().stream().anyMatch(x -> x.getId().equalsIgnoreCase(key))) {
                    continue;
                }

                final Tag tag = new Tag(key, StringUtils.capitalize(key), section.getString(key + ".tag"));
                tag.setDescription(section.getStringList(key + ".description"));
                if (section.get(key + ".permission") != null) {
                    tag.setPermission(section.getString(key + ".permission"));
                }

                convertedTags.add(tag);
            }

        }).thenRunAsync(() -> {
            this.plugin.getManager(TagManager.class).saveTags(convertedTags);
            msg.send(sender, "converted-tags", StringPlaceholders.single("total", convertedTags.size()));
        });

    }

}
