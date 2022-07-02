package xyz.oribuin.eternaltags.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.DataManager;

public class PlayerListeners implements Listener {

    private final DataManager data;

    public PlayerListeners(final EternalTags plugin) {
        this.data = plugin.getManager(DataManager.class);
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        this.data.loadUser(event.getPlayer().getUniqueId());
        this.data.loadFavourites(event.getPlayer().getUniqueId());
    }

}
