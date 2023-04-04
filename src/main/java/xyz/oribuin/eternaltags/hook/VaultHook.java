package xyz.oribuin.eternaltags.hook;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import javax.annotation.Nullable;

public class VaultHook {

    private static final boolean enabled;

    static {
        enabled = Bukkit.getPluginManager().isPluginEnabled("Vault");
    }

    /**
     * Get the vault permission instance
     *
     * @return The permission instance
     */
    @Nullable
    public static Permission getPermission() {
        if (!enabled)
            return null;

        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (rsp == null)
            return null;

        return rsp.getProvider();
    }

    /**
     * Get the highest group of a player
     *
     * @param player The player to get the group of
     * @return The highest group of the player
     */
    public static String getPrimaryGroup(Player player) {
        Permission permission = getPermission(); // Get the permission instance
        if (permission == null)
            return null;

        return permission.getPrimaryGroup(player); // Get the highest group (This is the group with the highest priority
    }

    /**
     * @return If vault is enabled or not
     */
    public static boolean isEnabled() {
        return enabled;
    }

}
