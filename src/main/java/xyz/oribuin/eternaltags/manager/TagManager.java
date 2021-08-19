package xyz.oribuin.eternaltags.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.manager.Manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TagManager extends Manager {

    private final EternalTags plugin = (EternalTags) this.getPlugin();
    private final List<Tag> tags = new ArrayList<>();

    private FileConfiguration config;
    private ConfigurationSection section;

    public TagManager(final EternalTags plugin) {
        super(plugin);
    }

    @Override
    public void enable() {
        this.config = YamlConfiguration.loadConfiguration(this.getDataFile());
        this.section = this.config.getConfigurationSection("tags");

        this.cacheTags();
    }

    public void cacheTags() {
        this.plugin.reloadConfig();

        if (section == null) {
            this.section = this.config.createSection("tags");
            this.saveData();
        }

        // Cache all the plugin tags
        this.tags.clear();

        for (String key : section.getKeys(false)) {
            final Tag tag = new Tag(key, section.getString(key + ".name"), section.getString(key + ".tag"));
            tag.setDescription(section.get(key + ".description") instanceof String ?
                    Collections.singletonList(section.getString(key + ".description"))
                    : section.getStringList(key + ".description"));

            if (section.get(key + ".permission") != null)
                tag.setPermission(section.getString(key + ".permission"));

            if (section.get(key + ".order") != null) {
                tag.setOrder(section.getInt(key + ".order"));
            }

            this.tags.add(tag);
        }

    }

    /**
     * Create and save a tag into the configuration file.
     *
     * @param tag The tag.
     */
    public void createTag(Tag tag) {
        final String id = tag.getId().toLowerCase();

        if (section == null) {
            this.section = this.config.createSection("tags");
            this.saveData();
        }

        this.section.set(id + ".name", tag.getName());
        this.section.set(id + ".tag", tag.getTag());
        this.section.set(id + ".description", tag.getDescription());
        this.section.set(id + ".permission", tag.getPermission());
        this.section.set(id + ".order", tag.getOrder());
        this.saveData();

        this.getTags().add(tag);
    }

    /**
     * Delete a tag from the configuration file
     *
     * @param tag The tag id.
     */
    public void deleteTag(Tag tag) {
        if (section == null) {
            this.section = this.config.createSection("tags");
            this.saveData();
        }

        this.section.set(tag.getId().toLowerCase(), null);
        this.saveData();

        this.tags.removeIf(x -> x.getId().equalsIgnoreCase(tag.getId()));
    }

    /**
     * Save a list of tags into the configuration file.
     *
     * @param tags The tag list
     */
    public void saveTags(List<Tag> tags) {
        if (section == null) {
            this.section = this.config.createSection("tags");
            this.saveData();
        }

        CompletableFuture.runAsync(() -> tags.forEach(tag -> {
            this.section.set(tag.getId().toLowerCase() + ".name", tag.getName());
            this.section.set(tag.getId().toLowerCase() + ".tag", tag.getTag());
            this.section.set(tag.getId().toLowerCase() + ".description", tag.getDescription());
            this.section.set(tag.getId().toLowerCase() + ".permission", tag.getPermission());
            this.section.set(tag.getId().toLowerCase() + ".order", tag.getOrder());
        })).thenRun(() -> {
            saveData();
            this.getTags().addAll(tags);
        });
    }

    /**
     * Get a list of all the tags a player has access to.
     *
     * @param player The player
     * @return A list of tags.
     */
    public List<Tag> getPlayersTag(final Player player) {

        return this.getTags()
                .stream()
                .filter(tag -> player.hasPermission(tag.getPermission()))
                .collect(Collectors.toList());

    }

    @Override
    public void disable() {

    }

    public List<Tag> getTags() {
        return tags;
    }

    private void saveData() {
        try {
            this.config.save(this.getDataFile());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private File getDataFile() {
        return new File(this.plugin.getDataFolder(), "config.yml");
    }
}
