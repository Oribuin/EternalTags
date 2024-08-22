package xyz.oribuin.eternaltags.conversion;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ValidPlugin {

    public static final Map<String, ConversionPlugin> PLUGINS = new HashMap<>();

    static {
        register("AlonsoTags", new AlonsoConversion());
        register("CIFYTags", new CIFYConversion());
        register("DeluxeTags", new DeluxeConversion());
    }

    /**
     * Register a plugin to be converted.
     *
     * @param name   The name of the plugin.
     * @param plugin The plugin instance.
     */
    public static void register(String name, ConversionPlugin plugin) {
        PLUGINS.put(name.toLowerCase(), plugin);
    }

    /**
     * Match a plugin name to a plugin converter
     *
     * @param name The name of the plugin.
     *
     * @return The plugin converter.
     */
    public static Optional<ConversionPlugin> match(String name) {
        return Optional.ofNullable(PLUGINS.get(name.toLowerCase()));
    }


}