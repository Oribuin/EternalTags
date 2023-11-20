package xyz.oribuin.eternaltags.util.nms;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.rosewood.rosegarden.utils.NMSUtil;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

public final class SkullUtils {

    private static Method method_SkullMeta_setProfile;

    private SkullUtils() {

    }

    /**
     * Applies a base64 encoded texture to an item's SkullMeta
     *
     * @param skullMeta The ItemMeta for the Skull
     * @param texture   The texture to apply to the skull
     */
    @SuppressWarnings("deprecation")
    public static void setSkullTexture(SkullMeta skullMeta, String texture) {

        if (texture != null && texture.startsWith("hdb:") && Bukkit.getPluginManager().isPluginEnabled("HeadDatabase")) {
            texture = new HeadDatabaseAPI().getBase64(texture.substring(4));
        }

        if (texture == null || texture.isEmpty())
            return;

        if (NMSUtil.getVersionNumber() >= 18) { // No need to use NMS on 1.18.1+
            if (NMSUtil.isPaper()) {
                setTexturesPaper(skullMeta, texture);
                return;
            }

            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.nameUUIDFromBytes(texture.getBytes()), "");
            PlayerTextures textures = profile.getTextures();

            String decodedTextureJson = new String(Base64.getDecoder().decode(texture));
            String decodedTextureUrl = decodedTextureJson.substring(28, decodedTextureJson.length() - 4);

            try {
                textures.setSkin(new URL(decodedTextureUrl));
            } catch (MalformedURLException e) {
                Bukkit.getLogger().severe("Failed to set skull texture: " + e.getMessage());
            }

            profile.setTextures(textures);
            skullMeta.setOwnerProfile(profile);
            return;
        }

        // 1.17 and below
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(texture.getBytes()), "");
        profile.getProperties().put("textures", new Property("textures", texture));

        try {
            if (method_SkullMeta_setProfile == null) {
                method_SkullMeta_setProfile = skullMeta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
                method_SkullMeta_setProfile.setAccessible(true);
            }

            method_SkullMeta_setProfile.invoke(skullMeta, profile);
        } catch (ReflectiveOperationException e) {
            Bukkit.getLogger().severe("Failed to set skull texture: " + e.getMessage());
        }
    }

    /**
     * Set the texture using the paper api for 1.18+
     *
     * @param meta    The skull meta
     * @param texture The texture
     */
    private static void setTexturesPaper(SkullMeta meta, String texture) {
        if (texture == null || texture.isEmpty())
            return;

        com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(texture.getBytes()), "");
        PlayerTextures textures = profile.getTextures();

        String decodedTextureJson = new String(Base64.getDecoder().decode(texture));
        String decodedTextureUrl = decodedTextureJson.substring(28, decodedTextureJson.length() - 4);

        try {
            textures.setSkin(new URL(decodedTextureUrl));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        profile.setTextures(textures);
        meta.setPlayerProfile(profile);
    }

}