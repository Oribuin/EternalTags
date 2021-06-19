package xyz.oribuin.eternaltags.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import xyz.oribuin.eternaltags.obj.Tag;

public class TagEquipEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList list = new HandlerList();
    private final Tag tag;
    private boolean cancelled = false;

    public TagEquipEvent(Player player, Tag tag) {
        super(player);
        this.tag = tag;
    }

    public Tag getTag() {
        return tag;
    }

    public static HandlerList getHandlerList() {
        return list;
    }

    @Override
    public HandlerList getHandlers() {
        return list;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
