package xyz.oribuin.eternaltags.conversion;

import dev.rosewood.rosegarden.RosePlugin;
import xyz.oribuin.eternaltags.obj.Tag;

import java.io.File;
import java.util.Map;

public abstract class ConversionPlugin {

    private final RosePlugin plugin;

    public ConversionPlugin(RosePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * All the tags found in the file.
     *
     * @return A map of converted tags.
     */
    public abstract Map<String, Tag> getPluginTags();

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
        return new File(this.plugin.getDataFolder().getParentFile(), this.getPluginName());
    }

    public RosePlugin getPlugin() {
        return this.plugin;
    }

}
