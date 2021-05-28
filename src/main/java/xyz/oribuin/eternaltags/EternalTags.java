package xyz.oribuin.eternaltags;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.hook.Expansion;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.util.Metrics;
import xyz.oribuin.eternaltags.util.UpdateChecker;
import xyz.oribuin.orilibrary.OriPlugin;
import xyz.oribuin.orilibrary.util.FileUtils;
import xyz.oribuin.orilibrary.util.HexUtils;

public class EternalTags extends OriPlugin {

    private FileConfiguration menuConfig;

    @Override
    public void enablePlugin() {

        // Check if server has PlaceholderAPI, No sure why it wouldn't though.
        if (!hasPlugin("PlaceholderAPI")) return;

        // Add bstats metrics
        if (this.getConfig().getBoolean("metrics")) {
            new Metrics(this, 11508);
        }

        // Load all plugin manages asynchronously
        this.getServer().getScheduler().runTaskAsynchronously(this, () -> {
            this.getManager(TagManager.class);
            this.getManager(DataManager.class);
            this.getManager(MessageManager.class);
        });

        // Check for plugin updates
        if (this.getConfig().getBoolean("check-updates")) {
            this.checkUpdates();
        }

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

    /**
     * Check for any plugin updates.
     */
    public void checkUpdates() {
        this.getLogger().info(HexUtils.colorify("&aChecking for plugin updates..."));

        if (UpdateChecker.getLatestVersion() != null) {
            // The amount of else here hurts my soul
            if (UpdateChecker.isUpdateAvailable(UpdateChecker.getLatestVersion(), this.getDescription().getVersion())) {
                this.getLogger().info(HexUtils.colorify("&aA new update is available for EternalTags (&c" + UpdateChecker.getLatestVersion() + "&a), You are on v" + this.getDescription().getVersion()));
            } else {
                this.getLogger().info(HexUtils.colorify("&aYou are on the latest version of EternalTags!"));
            }

        } else {
            this.getLogger().info(HexUtils.colorify("&cChecking for update failed, Could not get latest version..."));
        }
    }

    public FileConfiguration getMenuConfig() {
        return menuConfig;
    }

    public void setMenuConfig(FileConfiguration menuConfig) {
        this.menuConfig = menuConfig;
    }

}
