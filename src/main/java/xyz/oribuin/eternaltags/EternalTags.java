package xyz.oribuin.eternaltags;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import xyz.oribuin.eternaltags.hook.Expansion;
import xyz.oribuin.eternaltags.listener.PlayerListeners;
import xyz.oribuin.eternaltags.manager.CommandManager;
import xyz.oribuin.eternaltags.manager.ConfigurationManager;
import xyz.oribuin.eternaltags.manager.ConversionManager;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.MenuManager;
import xyz.oribuin.eternaltags.manager.PluginConversionManager;
import xyz.oribuin.eternaltags.manager.TagsManager;

import java.util.Arrays;
import java.util.List;

public class EternalTags extends RosePlugin {

    private static EternalTags instance;

    public static EternalTags getInstance() {
        return instance;
    }

    public EternalTags() {
        super(91842, 11508, ConfigurationManager.class, DataManager.class, LocaleManager.class, CommandManager.class);
        instance = this;
    }

    @Override
    public void enable() {

        // Make sure the server has PlaceholderAPI
        if (!this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.getLogger().severe("Please install PlaceholderAPI onto your server to use this plugin.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register PlaceholderAPI Expansion
        new Expansion(this).register();

        // Register Plugin Listeners
        this.getServer().getPluginManager().registerEvents(new PlayerListeners(this), this);

        // Initialize the API
        new EternalAPI(this);
    }

    @Override
    public void disable() {
        // Unused
    }

    @Override
    protected List<Class<? extends Manager>> getManagerLoadPriority() {
        return Arrays.asList(
                ConversionManager.class,
                TagsManager.class,
                MenuManager.class,
                PluginConversionManager.class
        );
    }

}
