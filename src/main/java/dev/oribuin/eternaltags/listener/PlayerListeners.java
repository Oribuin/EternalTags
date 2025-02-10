package dev.oribuin.eternaltags.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import dev.oribuin.eternaltags.EternalTags;
import dev.oribuin.eternaltags.manager.DataManager;
import dev.oribuin.eternaltags.manager.TagsManager;

public class PlayerListeners implements Listener {

    private final TagsManager manager = EternalTags.get().getManager(TagsManager.class);
    private final DataManager dataManager = EternalTags.get().getManager(DataManager.class);

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Load the user from the database
        this.dataManager.loadUser(event.getPlayer().getUniqueId()).thenRun(() -> {
            // Update the player's tag
            this.manager.getUserTag(event.getPlayer());
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.dataManager.getCachedUsers().remove(event.getPlayer().getUniqueId()); // Remove the user from the cache
    }

}
