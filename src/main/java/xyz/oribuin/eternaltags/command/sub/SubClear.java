package xyz.oribuin.eternaltags.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.command.SubCommand;
import xyz.oribuin.orilibrary.libs.jetbrains.annotations.NotNull;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

import java.util.List;
import java.util.Optional;

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
    public void executeArgument(@NotNull CommandSender sender, @NotNull String[] args) {

        final MessageManager msg = this.plugin.getManager(MessageManager.class);

        // Check arguments
        if (args.length != 2) {
            msg.send(sender, "invalid-arguments", StringPlaceholders.single("usage", this.getAnnotation().usage()));
            return;
        }

        final Player player = Bukkit.getPlayer(args[1]);

        if (player == null) {
            msg.send(sender, "invalid-player");
            return;
        }

        this.plugin.getManager(DataManager.class).updateUser(player.getUniqueId(), null);
        msg.send(sender, "cleared-tag");
    }

}
