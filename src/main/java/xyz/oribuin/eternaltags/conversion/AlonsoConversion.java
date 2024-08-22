package xyz.oribuin.eternaltags.conversion;

import dev.rosewood.rosegarden.RosePlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AlonsoConversion extends ConversionPlugin {

    @Override
    public Map<String, Tag> getPluginTags(RosePlugin plugin) {
        TagsManager manager = plugin.getManager(TagsManager.class);
        Map<String, Tag> convertedTags = new HashMap<>();

        FileConfiguration config = YamlConfiguration.loadConfiguration(this.getTagsFile());
        ConfigurationSection section = config.getConfigurationSection("Tags");

        if (section == null)
            return convertedTags;

        section.getKeys(false)
                .stream()
                .filter(s -> !manager.checkTagExists(s))
                .forEach(key -> {
                    Tag tag = new Tag(key, section.getString("Displayname"), section.getString(key + ".Tag"));

                    if (section.get(key + ".Lore.Unlocked") != null)
                        tag.setDescription(section.getStringList(key + ".Lore.Unlocked"));

                    if (section.get(key + ".Permission") != null)
                        tag.setPermission(section.getString(key + ".Permission"));

                    if (section.get(key + ".Material") != null) {
                        String icon = section.getString(key + ".Material");
                        assert icon != null;
                        if (icon.equalsIgnoreCase("CUSTOM_HEAD"))
                            icon = "PLAYER_HEAD";


                        tag.setIcon(Material.matchMaterial(icon));
                    }

                    convertedTags.put(key, tag);
                });

        return convertedTags;
    }

    @Override
    public String getPluginName() {
        return "AlonsoTags";
    }

    @Override
    public File getTagsFile() {
        return new File(this.getPluginsFolder(), "tags/alonso.yml");
    }

}
