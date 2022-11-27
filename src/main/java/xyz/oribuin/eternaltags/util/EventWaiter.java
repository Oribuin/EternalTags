package xyz.oribuin.eternaltags.util;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import xyz.oribuin.eternaltags.EternalTags;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A class that allows you to wait for an event to occur.
 * <p>
 * Taken from <a href="https://github.com/JDA-Applications/JDA-Utilities/blob/master/commons/src/main/java/com/jagrosh/jdautilities/commons/waiter/EventWaiter.java">JDA Utilities</a>
 */
@SuppressWarnings("unchecked")
public class EventWaiter implements Listener {

    private final Map<Class<?>, Set<WaitingEvent>> waitingEvents = new HashMap<>();

    /**
     * Waits for an event to occur.
     *
     * @param eventClass The class of the event to wait for.
     * @param condition  The condition that must be met for the event to be accepted.
     * @param action     The action to perform when the event is accepted.
     * @param <T>        The type of the event.
     */
    public <T extends Event> void waitForEvent(Class<T> eventClass, Predicate<T> condition, Consumer<T> action) {
        this.waitForEvent(eventClass, condition, action, -1, null, null);
    }

    /**
     * Waits for an event to occur.
     *
     * @param eventClass    The class of the event to wait for.
     * @param predicate     The check to perform on the event.
     * @param action        The action to perform on the event.
     * @param timeout       The timeout for the event to occur.
     * @param unit          The unit of the timeout.
     * @param timeoutAction The action to perform if the event does not occur within the timeout.
     * @param <T>           The type of the event.
     */
    public <T extends Event> void waitForEvent(Class<T> eventClass, Predicate<T> predicate, Consumer<T> action, long timeout, TimeUnit unit, Runnable timeoutAction) {


        var we = new WaitingEvent<>(predicate, action);
        var set = this.waitingEvents.computeIfAbsent(eventClass, k -> new HashSet<>());
        set.add(we);

        // Register the event if it hasn't been registered yet.
        Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.LOWEST, (listener, event) -> {
            if (predicate.test((T) event)) {
                action.accept((T) event);
                HandlerList.unregisterAll(this);

            }

        }, EternalTags.getInstance(), false);


        if (timeout > 0 && unit != null) {
            EternalTags.getInstance().getServer().getScheduler().runTaskLater(EternalTags.getInstance(), () -> {
                if (set.remove(we) && timeoutAction != null) {
                    timeoutAction.run();
                    HandlerList.unregisterAll(this);
                }

            }, unit.toMillis(timeout));
        }
    }

    private record WaitingEvent<T extends Event>(Predicate<T> predicate, Consumer<T> action) {
        boolean attempt(T event) {
            if (predicate.test(event)) {
                action.accept(event);
                return true;
            }

            return false;
        }
    }

}
