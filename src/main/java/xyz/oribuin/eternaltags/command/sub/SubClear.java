package xyz.oribuin.eternaltags.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.orilibrary.command.SubCommand;

@SubCommand.Info(
        names = {"clear"},
        usage = "/tags clear <player>",
        permission = "eternaltags.clear"
)
public class SubClear extends SubCommand {

    private final EternalTags plugin = (EternalTags) this.getOriPlugin();
    private final MessageManager msg = this.plugin.getManager(MessageManager.class);

    public SubClear(EternalTags plugin, CmdTags command) {
        super(plugin, command);
    }

    @Override
    public void executeArgument(CommandSender sender, String[] args) {

        if (sender.hasPermission("eternaltags.clear.other") && args.length == 2) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

                @SuppressWarnings("deprecation")
                OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);


                if (!player.hasPlayedBefore()) {
                    msg.send(sender, "invalid-player");
                    return;
                }

                this.plugin.getManager(DataManager.class).updateUser(player.getUniqueId(), null);
                msg.send(sender, "cleared-tag");
            });

            return;
        }

        if (!(sender instanceof Player)) {
            msg.send(sender, "player-only");
            return;
        }

        this.plugin.getManager(DataManager.class).updateUser(((Player) sender).getUniqueId(), null);
        msg.send(sender, "cleared-tag");

    }

}
