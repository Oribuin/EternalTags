package xyz.oribuin.eternaltags.command;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.gui.TagGUI;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.orilibrary.command.Command;
import xyz.oribuin.orilibrary.libs.jetbrains.annotations.NotNull;

@Command.Info(
        name = "tags",
        description = "Main command for EternalTags",
        permission = "eternaltags.use",
        playerOnly = false,
        usage = "/tags",
        subcommands = {},
        aliases = {}
)
public class CmdTags extends Command {

    private final EternalTags plugin = (EternalTags) this.getOriPlugin();

    public CmdTags(EternalTags plugin) {
        super(plugin);
    }

    @Override
    public void runFunction(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player && args.length == 0) {
            new TagGUI(this.plugin).createGUI((Player) sender);
            return;
        }

        final FileConfiguration config = this.plugin.getManager(MessageManager.class).getConfig();
        final String unknownCommand = config.getString("unknown-command");
        final String noPerm = config.getString("invalid-permission");
        this.runSubCommands(sender, args, unknownCommand, noPerm);
    }

}
