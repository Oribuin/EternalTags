package xyz.oribuin.eternaltags.hook;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.font.FontManager;
import io.th0rgal.oraxen.font.Glyph;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;

public class OraxenHook {

    private static final String pluginIdentifier = "oraxen:";

    public static String parseTag(String tag) {

        final String text = StringUtils.substringBetween(tag, "%", "%");

        if (text == null || !text.startsWith(pluginIdentifier))
            return tag;

        final FontManager fontManager = OraxenPlugin.get().getFontManager();
        final Glyph glyph = fontManager.getGlyphFromName(text.replace(pluginIdentifier, ""));

        if (glyph == null)
            return tag;

        String glyphText = tag.replace("%", "");
        glyphText = glyphText.replace(text, String.valueOf(glyph.getCharacter()));

        return glyphText;
    }

    /**
     * @return If oraxen is enabled or not
     */
    public static boolean enabled() {
        return Bukkit.getPluginManager().isPluginEnabled("Oraxen");
    }

}
