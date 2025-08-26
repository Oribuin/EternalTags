package dev.oribuin.eternaltags.conversion;

import dev.oribuin.eternaltags.EternalTags;
import dev.oribuin.eternaltags.manager.TagsManager;
import dev.rosewood.rosegarden.RosePlugin;
import org.bukkit.Bukkit;
import dev.oribuin.eternaltags.obj.Tag;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class ConversionPlugin {

    /**
     * Convert a plugin into EternalTags from this plugin.
     */
    public final Map<String, Tag> convert() {
        EternalTags rosePlugin = EternalTags.get();
        TagsManager manager = rosePlugin.getManager(TagsManager.class);
        Map<String, Tag> tags = this.getPluginTags(rosePlugin);

        // filter out tags that have name or tag null
        tags.entrySet().removeIf(entry -> entry.getKey() == null);
        manager.saveTags(tags);
        return tags;
    }

    /**
     * All the tags found in the file.
     *
     * @return A map of converted tags.
     */
    public abstract Map<String, Tag> getPluginTags(RosePlugin plugin);

    /**
     * The name of the plugin being converted
     *
     * @return The plugin name.
     */
    public abstract String getPluginName();

    /**
     * Get the file where all the tags are stored.
     *
     * @return The file.
     */
    public abstract File getTagsFile();

    /**
     * Get the main 'plugins' folder.
     *
     * @return The folder.
     */
    public File getPluginsFolder() {
        return new File(Bukkit.getServer().getUpdateFolderFile().getParentFile(), this.getPluginName());
    }

    /**
     * The generated tags file where the tags will be stored.
     *
     * @return The file.
     */
    public File generateFolder() {
        try {
            File source = TagsManager.TAGS_FOLDER.resolve(this.getPluginName() + ".yml").toFile();
            if (!source.exists()) source.createNewFile();

            return source;
        } catch (IOException ex) {
            EternalTags.get().getLogger().severe("Failed to create tags file for " + this.getPluginName());
            return null;
        }
    }


}
