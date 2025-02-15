package xyz.oribuin.eternaltags;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.RoseSetting;
import dev.rosewood.rosegarden.manager.Manager;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.config.Setting;
import xyz.oribuin.eternaltags.gui.MenuProvider;
import xyz.oribuin.eternaltags.hook.Expansion;
import xyz.oribuin.eternaltags.listener.BungeeListener;
import xyz.oribuin.eternaltags.listener.ChatListener;
import xyz.oribuin.eternaltags.listener.PlayerListeners;
import xyz.oribuin.eternaltags.manager.CommandManager;
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

    public EternalTags() {
        super(91842, 11508,DataManager.class, LocaleManager.class, CommandManager.class);
        instance = this;
    }

    public static EternalTags getInstance() {
        return instance;
    }

    public static EventWaiter getEventWaiter() {
        return eventWaiter;
    }

    @Override
    public void enable() {
        PluginManager pluginManager = this.getServer().getPluginManager();

        // Register Plugin Listeners
        pluginManager.registerEvents(new PlayerListeners(), this);

        // Enable Placeholder Formatting :-)
        if (Setting.CHAT_PLACEHOLDERS.get()) {
            pluginManager.registerEvents(new ChatListener(), this);
        }

        // Register Event Waiter
        eventWaiter = new EventWaiter();

        // Register PlaceholderAPI Expansion
        new Expansion(this).register();
    }

    @Override
    public void reload() {
        super.reload(); // Reload the managers

        MenuProvider.reload(); // Reload the menu provider

        // Register Plugin Messaging Channels
        if (Setting.PLUGIN_MESSAGING_RELOAD.get()) {
            this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
            this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);

            this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BungeeListener(this));
        }
    }

    @Override
    public void disable() {

    }

    @Override
    protected List<Class<? extends Manager>> getManagerLoadPriority() {
        return Arrays.asList(
                TagsManager.class,
                PluginConversionManager.class
        );
    }

    @Override
    protected @NotNull List<RoseSetting<?>> getRoseConfigSettings() {
        return Setting.getKeys();
    }

    @Override
    protected @NotNull String[] getRoseConfigHeader() {
        return Setting.getHeader();
    }
}
