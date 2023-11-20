package xyz.oribuin.eternaltags.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.TagsManager;

public class PlayerListeners implements Listener {

    private final TagsManager manager = EternalTags.getInstance().getManager(TagsManager.class);
    private final DataManager dataManager = EternalTags.getInstance().getManager(DataManager.class);

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.dataManager.loadUser(event.getPlayer().getUniqueId()); // Load the user from the database
        this.manager.getUserTag(event.getPlayer()); // Get the user's tag (This will detect default tags or the user's tag)

    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.dataManager.getCachedUsers().remove(event.getPlayer().getUniqueId()); // Remove the user from the cache
    }

}
