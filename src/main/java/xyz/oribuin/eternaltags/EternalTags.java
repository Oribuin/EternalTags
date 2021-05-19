package xyz.oribuin.eternaltags;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.hook.Expansion;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.MessageManager;
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
            this.getManager(MessageManager.class);
        });

        // Register PlaceholderAPI Expansion
        new Expansion(this).register();

        // Register Menu Config
        this.menuConfig = YamlConfiguration.loadConfiguration(FileUtils.createMenuFile(this, "tag-menu"));

        // Get command messages
        final FileConfiguration config = this.getManager(MessageManager.class).getConfig();
        final String prefix = config.getString("prefix");
        final String noPerm = prefix + config.getString("invalid-permission");

        // Register Commands
        new CmdTags(this).register(null, noPerm);
    }

    @Override
    public void disablePlugin() {
        // Unused
    }

    public FileConfiguration getMenuConfig() {
        return menuConfig;
    }

    public void setMenuConfig(FileConfiguration menuConfig) {
        this.menuConfig = menuConfig;
    }

}
