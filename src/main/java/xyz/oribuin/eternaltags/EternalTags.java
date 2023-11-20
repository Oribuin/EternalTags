package xyz.oribuin.eternaltags;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.hook.Expansion;
import xyz.oribuin.eternaltags.listener.BungeeListener;
import xyz.oribuin.eternaltags.listener.PlayerListeners;
import xyz.oribuin.eternaltags.manager.CommandManager;
import xyz.oribuin.eternaltags.manager.ConfigurationManager;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
import xyz.oribuin.eternaltags.manager.ConversionManager;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.PluginConversionManager;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.util.EventWaiter;

import java.util.Arrays;
import java.util.List;

public class EternalTags extends RosePlugin {

    private static EternalTags instance;
    private static EventWaiter eventWaiter;

    public static EternalTags getInstance() {
        return instance;
    }

    public EternalTags() {
        super(91842, 11508, ConfigurationManager.class, DataManager.class, LocaleManager.class, CommandManager.class);
        instance = this;
    }

    @Override
    public void enable() {
        PluginManager pluginManager = this.getServer().getPluginManager();

        // Register Plugin Listeners
        pluginManager.registerEvents(new PlayerListeners(), this);

        // Register Plugin Messaging Channels
        if (Setting.PLUGIN_MESSAGING.getBoolean()) {
            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BungeeListener(this));
        }

        // Register Event Waiter
        eventWaiter = new EventWaiter();

        // Register PlaceholderAPI Expansion
        new Expansion(this).register();
    }

    public void on(ItemStack player) {
    }

    @Override
    public void reload() {
        super.reload(); // Reload the managers

        MenuProvider.reload(); // Reload the menu provider
    }

    @Override
    public void disable() {
        if (Setting.PLUGIN_MESSAGING.getBoolean()) {
            this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
            this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        }
    }

    @Override
    protected List<Class<? extends Manager>> getManagerLoadPriority() {
        return Arrays.asList(
                ConversionManager.class,
                TagsManager.class,
                PluginConversionManager.class
        );
    }

    public static EventWaiter getEventWaiter() {
        return eventWaiter;
    }

}
