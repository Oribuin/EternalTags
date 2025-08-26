package dev.oribuin.eternaltags.listener;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Matcher;

/**
 * Adds PlaceholderAPI Support to the chat within the messages sent.
 */
@SuppressWarnings("deprecation") // thank you paper
public class ChatListener implements Listener {

    /**
     * Thank you PacksGamingHD for creating the original code.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String format = event.getFormat();
        Matcher matcher = PlaceholderAPI.getBracketPlaceholderPattern().matcher(format);
        if (!matcher.find()) return; // No placeholders found

        format = PlaceholderAPI.setBracketPlaceholders(player, format);

        event.setFormat(format);
    }

}
