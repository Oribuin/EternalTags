package xyz.oribuin.eternaltags.manager;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.hook.PAPI;
import xyz.oribuin.orilibrary.manager.Manager;
import xyz.oribuin.orilibrary.util.FileUtils;
import xyz.oribuin.orilibrary.util.HexUtils;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

import java.io.File;
import java.io.IOException;

public class MessageManager extends Manager {

    private final EternalTags plugin = (EternalTags) this.getPlugin();

    private FileConfiguration config;

    public MessageManager(EternalTags plugin) {
        super(plugin);
    }

    @Override
    public void enable() {
        this.config = YamlConfiguration.loadConfiguration(FileUtils.createFile(this.plugin, "messages.yml"));

        // Set any values that dont exist
        for (Messages value : Messages.values()) {
            if (config.get(value.key) == null) {
                config.set(value.key, value.defaultValue);
            }
        }

        try {
            config.save(new File(plugin.getDataFolder(), "messages.yml"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public FileConfiguration getConfig() {
        return config;
    }

    /**
     * Send a configuration message without any placeholders
     *
     * @param receiver  The CommandSender who receives the message.
     * @param messageId The message path
     */
    public void send(CommandSender receiver, String messageId) {
        this.send(receiver, messageId, StringPlaceholders.empty());
    }

    /**
     * Send a configuration messageId with placeholders.
     *
     * @param receiver     The CommandSender who receives the messageId.
     * @param messageId    The messageId path
     * @param placeholders The Placeholders
     */
    public void send(CommandSender receiver, String messageId, StringPlaceholders placeholders) {
        final String msg = this.getConfig().getString(messageId);

        if (msg == null) {
            receiver.sendMessage(HexUtils.colorify("&c&lError &7| &fThis is an invalid message in the messages file, Please contact the server owner about this issue. (Id: " + messageId + ")"));
            return;
        }

        receiver.sendMessage(HexUtils.colorify(PAPI.apply(receiver instanceof Player ? (Player) receiver : null, placeholders.apply(msg))));
    }

    @Override
    public void disable() {

    }

    private enum Messages {
        PREFIX("prefix", "&b&lTags &8| &f"),

        RELOAD("reload", "You have reloaded EternalTags!"),
        DISABLED_WORLD("disabled-world", "&cYou cannot do this in this world."),
        INVALID_PERMISSION("invalid-permission", "&cYou do not have permission to execute this command."),
        INVALID_PLAYER("invalid-player", "&cPlease enter a valid player."),
        INVALID_ARGUMENTS("invalid-arguments", "&cPlease provide valid arguments. Correct usage: %usage%"),
        INVALID_FUNDS("invalid-funds", "&cYou do not have enough funds to do this, You need $%price%."),
        UNKNOWN_COMMAND("unknown-command", "&cPlease include a valid command."),
        PLAYER_ONLY("player-only", "&cOnly a player can execute this command."),
        CONSOLE_ONLY("console-only", "&cOnly console can execute this command.");

        private final String key;
        private final Object defaultValue;

        Messages(final String key, final Object defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }
    }

}
