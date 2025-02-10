package dev.oribuin.eternaltags.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import dev.oribuin.eternaltags.EternalTags;
import dev.oribuin.eternaltags.manager.TagsManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class BungeeListener implements PluginMessageListener {

    private static final Logger LOGGER = Logger.getLogger("EternalTags/PluginMessenger");
    private final TagsManager manager;

    public BungeeListener(EternalTags plugin) {
        this.manager = plugin.getManager(TagsManager.class);
    }

    /**
     * Force all eternaltags servers to reload the plugin
     */
    public static void sendReload() {
//        if (!Setting.PLUGIN_MESSAGING_RELOAD.getBoolean()) return;

        try (
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(stream)
        ) {
            out.writeUTF("Forward");
            out.writeUTF("ALL");
            out.writeUTF("eternaltags:reload");

            Bukkit.getServer().sendPluginMessage(EternalTags.get(), "BungeeCord", stream.toByteArray());
        } catch (IOException ex) {
            LOGGER.severe("Failed to send reload message to BungeeCord: " + ex.getMessage());
        }
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!channel.equalsIgnoreCase("BungeeCord")) return;

        try (
                ByteArrayInputStream bytes = new ByteArrayInputStream(message);
                DataInputStream in = new DataInputStream(bytes)) {

            String commandInfo = in.readUTF();
            String[] commandInfoSplit = commandInfo.split(":");
            String namespace = commandInfoSplit[0];
            String command = commandInfoSplit.length > 1 ? commandInfoSplit[1] : null;

            if (!namespace.equalsIgnoreCase("eternaltags")) return;
            if (command == null) return;

            if (command.equalsIgnoreCase("reload")) {
                this.manager.getCachedTags().clear(); // Clear the cached tags
                this.manager.reload(); // Reload the plugin
            }

        } catch (IOException ex) {
            LOGGER.severe("Failed to receive message from BungeeCord: " + ex.getMessage());
        }
    }


}
