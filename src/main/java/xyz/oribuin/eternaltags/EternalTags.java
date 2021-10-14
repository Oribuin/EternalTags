package xyz.oribuin.eternaltags;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.hook.Expansion;
import xyz.oribuin.eternaltags.listener.PlayerJoinListener;
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
    private FileConfiguration favouriteConfig;

    @Override
    public void enablePlugin() {

        // Check if server has PlaceholderAPI, No sure why it wouldn't though.
        if (!hasPlugin("PlaceholderAPI"))
            return;

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
        this.loadMenus();

        final MessageManager msg = this.getManager(MessageManager.class);
        new CmdTags(this).register(sender -> msg.send(sender, "player-only"), sender -> msg.send(sender, "invalid-permission"));

        // Register Listeners.
        new PlayerJoinListener(this);

        // Initialize the API
        new EternalAPI(this);
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
            if (UpdateChecker.isUpdateAvailable(UpdateChecker.getLatestVersion(), this.getDescription().getVersion())) {
                this.getLogger().info(HexUtils.colorify("&aA new update is available for EternalTags (&c" + UpdateChecker.getLatestVersion() + "&a), You are on v" + this.getDescription().getVersion()));
                return;
            }

            this.getLogger().info(HexUtils.colorify("&aYou are on the latest version of EternalTags!"));
        } else {
            this.getLogger().info(HexUtils.colorify("&cChecking for update failed, Could not get latest version..."));
        }

    }

    @Override
    public void reload() {
        super.reload();
        this.loadMenus();
    }

    /**
     * Load all the plugin menus in the plugin.
     */
    private void loadMenus() {
        this.menuConfig = YamlConfiguration.loadConfiguration(FileUtils.createFile(this, "menus", "tag-menu.yml"));
        this.favouriteConfig = YamlConfiguration.loadConfiguration(FileUtils.createFile(this, "menus", "favourites-menu.yml"));
    }

    public FileConfiguration getMenuConfig() {
        return menuConfig;
    }

    public FileConfiguration getFavouriteConfig() {
        return favouriteConfig;
    }


}
