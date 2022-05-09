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
     * Convert a plugin into EternalTags
     *
     * @param plugin The plugin
     * @return The converted tags.
     */
    public Map<String, Tag> convertPlugin(ConversionPlugin conversionPlugin) {
        if (conversionPlugin == null)
            return new HashMap<>();

        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        final Map<String, Tag> tags = conversionPlugin.getPluginTags(this.rosePlugin);

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
