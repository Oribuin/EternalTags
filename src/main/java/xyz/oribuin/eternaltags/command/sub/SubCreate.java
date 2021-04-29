package xyz.oribuin.eternaltags.command.sub;

import org.bukkit.command.CommandSender;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.orilibrary.command.SubCommand;
import xyz.oribuin.orilibrary.libs.jetbrains.annotations.NotNull;

@SubCommand.Info(
        names = {"create"},
        usage = "/tags create <name> <tag>",
        permission = "eternaltags.create",
        command = CmdTags.class
)
public class SubCreate extends SubCommand {

    private final EternalTags plugin = (EternalTags) this.getOriPlugin();

    public SubCreate(EternalTags plugin, CmdTags command) {
        super(plugin, command);
    }

    @Override
    public void executeArgument(@NotNull CommandSender sender, @NotNull String[] args) {

        final MessageManager msg = this.plugin.getManager(MessageManager.class);

        // /tags create <name> <tag>
        // -1   0        1       2

        if (args.length != 3) {

        }
    }

}
