package dev.oribuin.eternaltags;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.RoseSetting;
import dev.rosewood.rosegarden.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.codehaus.plexus.util.Expand;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.oribuin.eternaltags.config.Setting;
import dev.oribuin.eternaltags.gui.MenuProvider;
import dev.oribuin.eternaltags.hook.Expansion;
import dev.oribuin.eternaltags.listener.ChatListener;
import dev.oribuin.eternaltags.listener.PlayerListeners;
import dev.oribuin.eternaltags.manager.CommandManager;
import dev.oribuin.eternaltags.manager.DataManager;
import dev.oribuin.eternaltags.manager.LocaleManager;
import dev.oribuin.eternaltags.manager.TagsManager;

import java.util.Arrays;
import java.util.List;

public class EternalTags extends RosePlugin {

    private static final Logger log = LoggerFactory.getLogger(EternalTags.class);
    private static EternalTags instance;
    private ChatListener chatListener;

    public EternalTags() {
        super(91842, 11508, DataManager.class, LocaleManager.class, CommandManager.class);
        instance = this;
    }

    public static EternalTags get() {
        return instance;
    }

    @Override
    public void enable() {
        // Register Plugin Listeners
        this.chatListener = new ChatListener();

        PluginManager manager = this.getServer().getPluginManager();
        manager.registerEvents(new PlayerListeners(), this);
        
        Expansion expansion = new Expansion(this);
        expansion.register();
    }

    @Override
    public void reload() {
        super.reload(); // Reload the managers
        MenuProvider.reload(); // Reload the menu provider
        
        // Reload the chat listener
        HandlerList.unregisterAll(this.chatListener);

        // Enable Placeholder Formatting :-)
        if (Setting.CHAT_PLACEHOLDERS.get()) {
            Bukkit.getPluginManager().registerEvents(this.chatListener, this);
        }
        
    }

    @Override
    public void disable() {

    }

    @Override
    protected @NotNull List<Class<? extends Manager>> getManagerLoadPriority() {
        return List.of(TagsManager.class);
    }

    @Override
    protected @NotNull List<RoseSetting<?>> getRoseConfigSettings() {
        return Setting.getKeys();
    }

    @Override
    protected @NotNull String[] getRoseConfigHeader() {
        return new String[]{
                "___________ __                             ._____________",
                "\\_   _____//  |_  ___________  ____ _____  |  \\__    ___/____     ____  ______",
                " |    __)_\\   __\\/ __ \\_  __ \\/    \\\\__  \\ |  | |    |  \\__  \\   / ___\\/  ___/",
                " |        \\|  | \\  ___/|  | \\/   |  \\/ __ \\|  |_|    |   / __ \\_/ /_/  >___ \\ ",
                "/_______  /|__|  \\___  >__|  |___|  (____  /____/____|  (____  /\\___  /____  >",
                "        \\/           \\/           \\/     \\/                  \\//_____/     \\/ "
        };
    }
    
}
