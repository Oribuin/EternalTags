package xyz.oribuin.eternaltags.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.TagsManager;

import java.util.concurrent.CompletableFuture;

public class PlayerListeners implements Listener {

    private final TagsManager manager = EternalTags.getInstance().getManager(TagsManager.class);
    private final DataManager dataManager = EternalTags.getInstance().getManager(DataManager.class);

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
