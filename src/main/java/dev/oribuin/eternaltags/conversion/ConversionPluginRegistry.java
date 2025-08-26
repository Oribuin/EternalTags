package dev.oribuin.eternaltags.conversion;

import dev.oribuin.eternaltags.conversion.impl.AlonsoConversion;
import dev.oribuin.eternaltags.conversion.impl.CIFYConversion;
import dev.oribuin.eternaltags.conversion.impl.DeluxeConversion;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class ConversionPluginRegistry {

    public static final Map<String, Supplier<ConversionPlugin>> PLUGINS = new HashMap<>();

    static {
        register(AlonsoConversion::new);
        register(CIFYConversion::new);
        register(DeluxeConversion::new);
    }

    /**
     * Register a plugin as possible to be converted. This is used to convert tags from other plugins.
     *
     * @param pluginSupplier The conversion supplier
     */
    public static void register(Supplier<ConversionPlugin> pluginSupplier) {
        PLUGINS.put(pluginSupplier.get().getPluginName().toLowerCase(), pluginSupplier);
    }

    /**
     * Match a plugin by its name and return the supplier.
     *
     * @param name The name of the plugin
     * @return The plugin supplier
     */
    public static ConversionPlugin match(String name) {
        Supplier<ConversionPlugin> plugin = PLUGINS.get(name.toLowerCase());
        return plugin != null ? plugin.get() : null;
    }


}