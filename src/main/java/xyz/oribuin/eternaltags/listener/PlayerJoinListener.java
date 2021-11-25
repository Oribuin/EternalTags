package xyz.oribuin.eternaltags.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.Optional;

public class PlayerJoinListener implements Listener {

    private final EternalTags plugin;
    private final DataManager data;

    public PlayerJoinListener(final EternalTags plugin) {
        this.plugin = plugin;
        this.data = this.plugin.getManager(DataManager.class);

        this.plugin.getServer().getPluginManager().registerEvents(this, this.plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        this.data.getTag(event.getPlayer().getUniqueId());
        this.data.getFavourites(event.getPlayer());

        // Add a default tag to any user who hasnt joined.
        if (!event.getPlayer().hasPlayedBefore()) {
            final String tagId = this.plugin.getConfig().getString("default-tag");
            if (tagId == null || tagId.equalsIgnoreCase("None")) {
                return;
            }

            final Optional<Tag> optionalTag = this.plugin.getManager(TagManager.class).getTags()
                    .stream()
                    .filter(tag -> tag.getId().equalsIgnoreCase(tagId))
                    .findAny();

            optionalTag.ifPresent(tag -> this.data.updateUser(event.getPlayer().getUniqueId(), tag));
        }

        // Remove if the user doesn't have permission to use the option.
        if (this.plugin.getConfig().getBoolean("remove-inaccessible-tags")) {
            final Tag tag = this.data.getTag(event.getPlayer().getUniqueId());
            if (tag == null)
                return;

            if (!event.getPlayer().hasPermission(tag.getPermission()))
                data.removeUser(event.getPlayer().getUniqueId());
        }
    }
}
