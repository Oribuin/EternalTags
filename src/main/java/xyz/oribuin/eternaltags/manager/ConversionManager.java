package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.manager.Manager;
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

public class ConversionManager extends Manager {

    private FileConfiguration oldConfig;
    private Map<String, Tag> loadedTags;
    private Map<String, Object> configOptions;

    public ConversionManager(RosePlugin rosePlugin) {
        super(rosePlugin);

    }

    public void reload() {
        if (!this.shouldConvert()) {
            this.rosePlugin.getLogger().info("Plugin files are up to date!");
            return;
        }

        this.rosePlugin.getLogger().warning("Converting old EternalTags configs");
        File file = new File(this.rosePlugin.getDataFolder(), "config.yml");
        this.oldConfig = YamlConfiguration.loadConfiguration(file);
        this.loadedTags = new HashMap<>();
        this.configOptions = new HashMap<>();
        this.loadConfiguration();

        // Delete the folder.
        file.delete();

        final ConfigurationManager manager = this.rosePlugin.getManager(ConfigurationManager.class);
        manager.reload();
        final CommentedFileConfiguration newConfig = manager.getConfig();
        this.configOptions.forEach(newConfig::set);
        newConfig.save();

    }

    @Override
    public void disable() {

    }

    private void loadConfiguration() {
        // Load all the old options, so we're not fucking people over.
        final CommentedFileConfiguration newConfig = this.rosePlugin.getManager(ConfigurationManager.class).getConfig();

        // load all the old config options.
        for (String path : this.oldConfig.getKeys(false)) {
            final String remappedPath = this.getRemappedOptions().get(path);
            if (remappedPath != null)
                this.configOptions.put(remappedPath, this.oldConfig.get(path));
        }

        // Transfer over mysql settings.
        final ConfigurationSection mysqlSection = this.oldConfig.getConfigurationSection("mysql");
        if (mysqlSection != null) {
            for (String path : mysqlSection.getKeys(false)) {
                final String remappedPath = this.getRemappedOptions().get("mysql." + path);
                if (remappedPath != null) {
                    this.configOptions.put(remappedPath, mysqlSection.get(path));
                }
            }
        }

        this.getRemappedOptions().forEach((s, s2) -> newConfig.set(s2, this.configOptions.get(s)));
        newConfig.save();

        // Convert tags.
        final ConfigurationSection section = this.oldConfig.getConfigurationSection("tags");
        if (section == null)
            return;

        this.rosePlugin.getLogger().warning("Converting Tags into tags.yml");
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

        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        manager.wipeTags();
        manager.saveTags(this.loadedTags);
    }

    public Map<String, String> getRemappedOptions() {
        return new LinkedHashMap<>() {{
            this.put("default-tag", "default-tag");
            this.put("formatted_placeholder", "formatted-placeholder");
            this.put("remove-inaccessible-tags", "remove-inaccessible-tags");
            this.put("mysql.enabled", "mysql-settings.enabled");
            this.put("mysql.host", "mysql-settings.hostname");
            this.put("mysql.port", "mysql-settings.port");
            this.put("mysql.dbname", "mysql-settings.database-name");
            this.put("mysql.username", "mysql-settings.user-name");
            this.put("mysql.password", "mysql-settings.user-password");
            this.put("mysql.ssl", "mysql-settings.use-ssl");
        }};
    }

    /**
     * Check if the plugin needs to convert into v1.1.0
     *
     * @return If the plugin has converted or not.
     */
    public boolean shouldConvert() {
        final File file = new File(this.rosePlugin.getDataFolder(), "tags.yml");

        return !file.exists() && this.rosePlugin.getConfig().get("tags") != null;
    }

}
