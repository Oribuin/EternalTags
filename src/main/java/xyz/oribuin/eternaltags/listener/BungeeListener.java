package xyz.oribuin.eternaltags.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.io.*;
import java.util.List;
import java.util.UUID;

public class BungeeListener implements PluginMessageListener {

    private final TagsManager manager;

    public BungeeListener(EternalTags plugin) {
        this.manager = plugin.getManager(TagsManager.class);
    }



    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!channel.equalsIgnoreCase("BungeeCord"))
            return;

        ByteArrayInputStream bytes = new ByteArrayInputStream(message);
        DataInputStream in = new DataInputStream(bytes);

        try {
            String commandInfo = in.readUTF();
            String[] commandInfoSplit = commandInfo.split(":");
            String namespace = commandInfoSplit[0];
            String command = commandInfoSplit.length > 1 ? commandInfoSplit[1] : null;

            if (!namespace.equalsIgnoreCase("eternaltags"))
                return;

            byte[] msgBytes = new byte[in.readShort()];
            in.readFully(msgBytes);
            DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(msgBytes));
            String received = msgIn.readUTF();
            String[] receivedSplit = received.split(":");

            if (command == null)
                return;

            // Delete the tag
            if (command.equalsIgnoreCase("delete")) {
                String tagId = receivedSplit[0];
                this.manager.clearTagFromUsers(tagId);
                this.manager.getCachedTags().remove(tagId);
                return;
            }

            if (command.equalsIgnoreCase("modify")) {
                // id, name, tag, permission, order, icon, lore
                Tag newTag = new Tag(receivedSplit[0].toLowerCase(), receivedSplit[1], receivedSplit[2]);
                newTag.setPermission(receivedSplit[3]);
                newTag.setOrder(Integer.parseInt(receivedSplit[4]));
                String icon = receivedSplit[5];
                if (icon.equalsIgnoreCase("none"))
                    newTag.setIcon(Material.getMaterial(icon));

                newTag.setDescription(List.of(receivedSplit[6].split("\n")));

                this.manager.saveTag(newTag);
                this.manager.updateActiveTag(newTag);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Send a plugin message to the bungee server to modify a tag
     *
     * @param tag The tag to modify
     */
    public static void modifyTag(@NotNull Tag tag) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(outputStream);

        try {
            out.writeUTF("Forward");
            out.writeUTF("ALL");
            out.writeUTF("eternaltags:modify");

            // id, name, tag, permission, order, icon, lore
            String lore = String.join("\n", tag.getDescription());
            String message = tag.getId() + ":" + tag.getName() + ":" + tag.getTag() + ":" + tag.getPermission() + ":" + tag.getOrder() + ":" + (tag.getIcon() == null ? "null" : tag.getIcon().name()) + ":" + lore;
            sendPluginMessage(outputStream, out, message);

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Send a plugin message to the bungee server to delete a tag
     *
     * @param name The name of the tag to delete
     */
    public static void deleteTag(@NotNull String name) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(outputStream);

        try {
            out.writeUTF("Forward");
            out.writeUTF("ALL");
            out.writeUTF("eternaltags:delete");

            sendPluginMessage(outputStream, out, name);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Send a message to the BungeeCord server
     *
     * @param outputStream The output stream
     * @param stream       The stream to send
     * @param message      The message to send
     */
    private static void sendPluginMessage(ByteArrayOutputStream outputStream, DataOutputStream stream, String message) {
        try {
            ByteArrayOutputStream msgBytes = new ByteArrayOutputStream();
            DataOutputStream msgOut = new DataOutputStream(msgBytes);
            msgOut.writeUTF(message);

            stream.writeShort(msgBytes.toByteArray().length);
            stream.write(msgBytes.toByteArray());
            Bukkit.getServer().sendPluginMessage(EternalTags.getInstance(), "BungeeCord", outputStream.toByteArray());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
