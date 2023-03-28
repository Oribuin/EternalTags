package xyz.oribuin.eternaltags;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import dev.rosewood.rosegarden.utils.NMSUtil;
import org.bukkit.plugin.PluginManager;
import xyz.oribuin.eternaltags.listener.BungeeListener;
import xyz.oribuin.eternaltags.hook.Expansion;
import xyz.oribuin.eternaltags.listener.PlayerListeners;
import xyz.oribuin.eternaltags.manager.*;
import xyz.oribuin.eternaltags.manager.ConfigurationManager.Setting;
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

        // Make sure the server has PlaceholderAPI
        if (!pluginManager.isPluginEnabled("PlaceholderAPI")) {
            this.getLogger().severe("Please install PlaceholderAPI onto your server to use this plugin.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Make sure the server is on MC 1.16
        if (NMSUtil.getVersionNumber() < 16) {
            this.getLogger().severe("This plugin only supports 1.16+ Minecraft.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

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
