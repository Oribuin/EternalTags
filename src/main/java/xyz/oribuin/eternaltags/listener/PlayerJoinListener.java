package xyz.oribuin.eternaltags.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;

import javax.swing.*;
import java.util.Optional;

public class PlayerJoinListener implements Listener {

    private final EternalTags plugin;
    private final DataManager dataManager;

    public PlayerJoinListener(final EternalTags plugin) {
        this.plugin = plugin;
        this.dataManager = this.plugin.getManager(DataManager.class);

        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        if (event.getPlayer().hasPlayedBefore()) {
            return;
        }

        final String tagId = this.plugin.getConfig().getString("default-tag");

        if (tagId == null || tagId.equalsIgnoreCase("None")) {
            return;
        }

        final Optional<Tag> optionalTag = this.plugin.getManager(TagManager.class).getTags()
                .stream()
                .filter(tag -> tag.getId().equalsIgnoreCase(tagId))
                .findAny();

        optionalTag.ifPresent(tag -> this.dataManager.updateUser(event.getPlayer().getUniqueId(), tag));
    }

}
