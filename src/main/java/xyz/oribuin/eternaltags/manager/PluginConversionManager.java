package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import org.checkerframework.checker.units.qual.C;
import xyz.oribuin.eternaltags.conversion.ConversionPlugin;
import xyz.oribuin.eternaltags.obj.Category;
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
     * Convert a plugin into EternalTags and create a category for it.
     *
     * @param conversionPlugin The plugin to convert
     */
    public Map<String, Tag> convertPlugin(ConversionPlugin conversionPlugin) {
        if (conversionPlugin == null)
            return new HashMap<>();

        final TagsManager manager = this.rosePlugin.getManager(TagsManager.class);
        final Map<String, Tag> tags = conversionPlugin.getPluginTags(this.rosePlugin);

        // Create a category for the plugin
        Category category = new Category(conversionPlugin.getPluginName().toLowerCase());
        category.setDisplayName(conversionPlugin.getPluginName());
        category.setPermission("eternaltags.category." + category.getId());
        manager.saveCategory(category);

        // Set the category for each tag
        tags.values().forEach(tag -> tag.setCategory(category.getId()));

        // filter out tags that have name or tag null
        tags.entrySet().removeIf(entry -> entry.getKey() == null);
        manager.saveTags(tags);


        return tags;
    }

    @Override
    public void disable() {
        // Unused
    }
}
