package xyz.oribuin.eternaltags.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.TagsManager;

import java.util.List;

public class PlayerListeners implements Listener {

    private final TagsManager manager = EternalTags.getInstance().getManager(TagsManager.class);
    private final DataManager dataManager = EternalTags.getInstance().getManager(DataManager.class);

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.manager.getUserTag(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.dataManager.getCachedUsers().remove(event.getPlayer().getUniqueId());
    }

}
