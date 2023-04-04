package xyz.oribuin.eternaltags.hook;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.Glyph;
import org.bukkit.Bukkit;

import java.util.Optional;

public final class OraxenHook {

    private static final String pluginIdentifier = "oraxen:";

    /**
     * Parse oraxen glyphs through a tag
     *
     * @param tag The tag to parse
     * @return The parsed glyph
     */
    public static String parseGlyph(String tag) {
        Optional<Glyph> matchedGlyph = Optional.empty();
        for (Glyph glyph : OraxenPlugin.get().getFontManager().getGlyphs()) {
            if (tag.contains(pluginIdentifier + glyph.getName())) {
                matchedGlyph = Optional.of(glyph);
            }
        }

        return matchedGlyph.map(glyph -> tag.replace(pluginIdentifier + glyph.getName(), String.valueOf(glyph.getCharacter()))).orElse(tag);

    }

    /**
     * @return If oraxen is enabled or not
     */
    public static boolean enabled() {
        return Bukkit.getPluginManager().isPluginEnabled("Oraxen");
    }

}
