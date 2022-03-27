package xyz.oribuin.eternaltags.obj;

import org.bukkit.Color;

import java.util.Arrays;
import java.util.Optional;

/**
 * thank you spigot for your awful everything making getting colours by name awful
 * and also you named them incorrectly.
 * why is pink named purple
 */
public enum BukkitColour {
    WHITE(Color.WHITE),
    LIGHT_GRAY(Color.SILVER),
    GRAY(Color.GRAY),
    BLACK(Color.BLACK),
    LIME(Color.LIME),
    RED(Color.RED),
    DARK_RED(Color.MAROON),
    YELLOW(Color.YELLOW),
    GREEN(Color.OLIVE),
    AQUA(Color.AQUA),
    TEAL(Color.TEAL),
    BLUE(Color.BLUE),
    NAVY(Color.NAVY),
    PURPLE(Color.FUCHSIA),
    PINK(Color.PURPLE),
    ORANGE(Color.ORANGE);

    final Color color;

    BukkitColour(Color color) {
        this.color = color;
    }

    public Color get() {
        return this.color;
    }

    /**
     * Match a bukkit color by name
     *
     * @param name The name of the color
     * @return The color
     */
    public static Optional<BukkitColour> match(String name) {
        return Arrays.stream(BukkitColour.values()).filter(color -> color.name().equalsIgnoreCase(name)).findFirst();
    }
}
