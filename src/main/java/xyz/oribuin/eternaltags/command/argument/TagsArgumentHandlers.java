package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentHandler;
import org.bukkit.Material;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.conversion.ConversionPlugin;
import xyz.oribuin.eternaltags.obj.Category;
import xyz.oribuin.eternaltags.obj.Tag;

public final class TagsArgumentHandlers {

    private static final RosePlugin ROSE_PLUGIN = EternalTags.getInstance();
    public static final ArgumentHandler<Category> CATEGORY = new CategoryArgumentHandler(ROSE_PLUGIN);
    public static final ArgumentHandler<Material> MATERIAL = new MaterialArgumentHandler(ROSE_PLUGIN);
    public static final ArgumentHandler<ConversionPlugin> CONVERSION_PLUGIN = new PluginArgumentHandler(ROSE_PLUGIN);
    public static final ArgumentHandler<Tag> TAG = new TagsArgumentHandler(ROSE_PLUGIN);

    private TagsArgumentHandlers() { }

}