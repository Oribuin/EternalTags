package xyz.oribuin.eternaltags.manager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.manager.Manager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TagManager extends Manager {

    private final EternalTags plugin = (EternalTags) this.getPlugin();
    private final List<Tag> tags = new ArrayList<>();
    private final ConfigurationSection section = this.plugin.getConfig().getConfigurationSection("tags");

    public TagManager(final EternalTags plugin) {
        super(plugin);
    }

    @Override
    public void enable() {
        this.cacheTags();
    }

    public void cacheTags() {
        if (section == null) return;

        // Cache all the plugin tags
        this.tags.clear();

        for (String key : section.getKeys(false)) {
            final Tag tag = new Tag(key, section.getString(key + ".name"), section.getString(key + ".tag"));
            tag.setDescription(section.getString(key + ".description"));

            this.tags.add(tag);
        }

    }

    /**
     * Get a list of all the tags a player has access to.
     *
     * @param player The player
     * @return A list of tags.
     */
    public List<Tag> getPlayersTag(final Player player) {

        return this.plugin.getManager(TagManager.class).getTags()
                .stream()
                .filter(tag -> player.hasPermission("eternaltags.tag." + tag.getId()))
                .collect(Collectors.toList());

    }

    @Override
    public void disable() {

    }

    public List<Tag> getTags() {
        return tags;
    }
}
