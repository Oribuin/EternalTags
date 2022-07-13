package xyz.oribuin.eternaltags.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.DataManager;

public class PlayerListeners implements Listener {

    private final DataManager data = EternalTags.getInstance().getManager(DataManager.class);

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.data.loadUser(event.getPlayer().getUniqueId());
    }

}
