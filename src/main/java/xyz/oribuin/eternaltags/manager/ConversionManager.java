package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.oribuin.eternaltags.obj.Tag;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConversionManager {

    private final RosePlugin rosePlugin;
    private FileConfiguration oldConfig;
    private Map<String, Tag> loadedTags;
    private Map<String, Object> configOptions;

    public ConversionManager(RosePlugin rosePlugin) {
        this.rosePlugin = rosePlugin;
    }

    public void enable() {
        if (this.hasConverted())
            return;

        this.rosePlugin.getLogger().warning("Converting old EternalTags configs");
        File file = new File(this.rosePlugin.getDataFolder(), "config.yml");
        if (file.exists()) {
            this.rosePlugin.getLogger().info("Moved old config.yml to old-config.yml");
            final File newFile = new File(this.rosePlugin.getDataFolder(), "old-config.yml");
            file.renameTo(newFile);

            file = newFile;
        }

        this.oldConfig = YamlConfiguration.loadConfiguration(file);
        this.loadedTags = new HashMap<>();
        this.configOptions = new HashMap<>();
        this.loadConfiguration();

        final ConfigurationManager manager = this.rosePlugin.getManager(ConfigurationManager.class);
        final CommentedFileConfiguration newConfig = manager.getConfig();
        this.configOptions.forEach(newConfig::set);
        newConfig.save();
    }

    private void loadConfiguration() {

        // Load all the old options, so we're not fucking people over.
        this.oldConfig.getKeys(false).stream()
                .filter(s -> !s.startsWith("tags"))
                .forEach(s -> this.configOptions.put(s, this.oldConfig.get(s)));

        final ConfigurationSection section = this.oldConfig.getConfigurationSection("tags");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            final Tag tag = new Tag(key, section.getString(key + ".name"), section.getString(key + ".tag"));
            List<String> description = section.get(key + ".description") instanceof String
                    ? Collections.singletonList(section.getString(key + ".description"))
                    : section.getStringList(key + ".description");

            tag.setDescription(description);
            if (section.getString(key + ".permission") != null)
                tag.setPermission(section.getString(key + ".permission"));

            if (section.getString(key + ".order") != null)
                tag.setOrder(section.getInt(key + ".order"));

            if (section.getString(key + ".icon") != null)
                tag.setIcon(Material.matchMaterial(Objects.requireNonNull(section.getString(key + ".icon"))));

            this.loadedTags.put(key, tag);
        }
    }

    public Map<String, String> getRemappedOptions() {
        return new LinkedHashMap<String, String>() {{
            this.put("", "");
        }};
    }

    /**
     * Check if the plugin has converted into v1.1.0
     *
     * @return If the plugin has converted or not.
     */
    public boolean hasConverted() {
        final File file = new File(this.rosePlugin.getDataFolder(), "tags.yml");

        return file.exists() && this.rosePlugin.getConfig().get("tags") == null;
    }

    public Map<String, Tag> getLoadedTags() {
        return loadedTags;
    }

    public Map<String, Object> getConfigOptions() {
        return configOptions;
    }

}
