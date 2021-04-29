package xyz.oribuin.eternaltags;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.hook.Expansion;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.orilibrary.OriPlugin;
import xyz.oribuin.orilibrary.util.FileUtils;

public class EternalTags extends OriPlugin {

    private FileConfiguration menuConfig;

    @Override
    public void enablePlugin() {

        // Check if server has PlaceholderAPI, No sure why it wouldn't though.
        if (!hasPlugin("PlaceholderAPI")) return;

        this.getServer().getScheduler().runTaskAsynchronously(this, () -> {
            this.getManager(TagManager.class);
            this.getManager(DataManager.class);
        });

        // Register PlaceholderAPI Expansion
        new Expansion(this).register();

        // Register Menu Config
        this.menuConfig = YamlConfiguration.loadConfiguration(FileUtils.createMenuFile(this, "tag-menu"));

        // Register Commands
        new CmdTags(this).register(null, null);


        // Register Listeners.

    }

    @Override
    public void disablePlugin() {

    }

    public FileConfiguration getMenuConfig() {
        return menuConfig;
    }
}
