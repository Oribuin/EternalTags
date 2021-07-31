package xyz.oribuin.eternaltags.hook;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public final class PAPI {

    public static String apply(@Nullable Player player, String text) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI"))
            return text;

        return PlaceholderAPI.setPlaceholders(player, text);
    }

}
