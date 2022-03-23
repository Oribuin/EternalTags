package xyz.oribuin.eternaltags.conversion;

import java.util.Arrays;
import java.util.Optional;

public enum ValidPlugin {
    ALONSOTAGS("AlonsoTags"),
    CIFYTAGS("CIFYTags"),
    DELUXETAGS("DeluxeTags");

    private final String display;

    ValidPlugin(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
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