package xyz.oribuin.eternaltags.conversion;

import java.util.Arrays;
import java.util.Optional;

public enum ValidPlugin {
    ALFONSOTAGS("AlfonsoTags", AlfonsoConversion.class),
    CIFYTAGS("CIFYTags", CIFYConversion.class),
    DELUXETAGS("DeluxeTags", DeluxeConversion.class);

    private final String display;
    private final Class<? extends ConversionPlugin> conversionClass;

    ValidPlugin(String display, Class<? extends ConversionPlugin> conversionClass) {
        this.display = display;
        this.conversionClass = conversionClass;
    }

    public String getDisplay() {
        return display;
    }

    public Class<? extends ConversionPlugin> getConversionClass() {
        return conversionClass;
    }

    /**
     * Match a valid plugin by the name
     *
     * @param name The name of the plugin
     * @return The enum if present.
     */
    public static Optional<ValidPlugin> match(String name) {
        return Arrays.stream(ValidPlugin.values()).filter(validPlugin -> validPlugin.name().equalsIgnoreCase(name)).findFirst();
    }
}