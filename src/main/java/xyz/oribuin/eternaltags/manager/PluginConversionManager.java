package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import xyz.oribuin.eternaltags.conversion.AlonsoConversion;
import xyz.oribuin.eternaltags.conversion.CIFYConversion;
import xyz.oribuin.eternaltags.conversion.ConversionPlugin;
import xyz.oribuin.eternaltags.conversion.DeluxeConversion;
import xyz.oribuin.eternaltags.conversion.ValidPlugin;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.HashMap;
import java.util.Map;

public class PluginConversionManager extends Manager {

    public PluginConversionManager(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public void reload() {
        // Unused
    }

    /**
     * Get a conversion plugin from ValidPlugin.
     *
     * @param plugin The plugin
     * @return The conversion function.
     */
    public ConversionPlugin getPlugin(ValidPlugin plugin) {
        return switch (plugin) {
            case DELUXETAGS -> new DeluxeConversion(this.rosePlugin);
            case CIFYTAGS -> new CIFYConversion(this.rosePlugin);
            case ALONSOTAGS -> new AlonsoConversion(this.rosePlugin);
        };
    }

    /**
     * Convert a plugin into EternalTags
     *
     * @param plugin The plugin
     * @return The converted tags.
     */
    public Map<String, Tag> convertPlugin(ValidPlugin plugin) {
        ConversionPlugin conversionPlugin = this.getPlugin(plugin);
        if (conversionPlugin == null)
            return new HashMap<>();

        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        final Map<String, Tag> tags = conversionPlugin.getPluginTags();

        // filter out tags that have name or tag null
        tags.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue().getName() == null);
        manager.saveTags(tags);
        return tags;
    }

    @Override
    public void disable() {
        // Unused
    }
}
