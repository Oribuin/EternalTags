package xyz.oribuin.eternaltags.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.orilibrary.command.SubCommand;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

@SubCommand.Info(
        names = {"clear"},
        usage = "/tags clear <player>",
        permission = "eternaltags.clear",
        command = CmdTags.class
)
public class SubClear extends SubCommand {

    private final EternalTags plugin = (EternalTags) this.getOriPlugin();

    public SubClear(EternalTags plugin, CmdTags command) {
        super(plugin, command);
    }

    @Override
    public void executeArgument(CommandSender sender, String[] args) {

        final MessageManager msg = this.plugin.getManager(MessageManager.class);

        // Check arguments
        if (args.length != 2) {
            msg.send(sender, "invalid-arguments", StringPlaceholders.single("usage", this.getAnnotation().usage()));
            return;
        }

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
    }

}
