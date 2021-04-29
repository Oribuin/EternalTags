package xyz.oribuin.eternaltags.command;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.sub.SubCreate;
import xyz.oribuin.eternaltags.command.sub.SubDelete;
import xyz.oribuin.eternaltags.command.sub.SubSet;
import xyz.oribuin.eternaltags.gui.TagGUI;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.command.Command;
import xyz.oribuin.orilibrary.libs.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Command.Info(
        name = "tags",
        description = "Main command for EternalTags",
        permission = "eternaltags.use",
        playerOnly = false,
        usage = "/tags",
        subcommands = {SubCreate.class, SubDelete.class, SubSet.class},
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
            new TagGUI(this.plugin).createGUI((Player) sender, null);
            return;
        }

        final FileConfiguration config = this.plugin.getManager(MessageManager.class).getConfig();
        final String prefix = config.getString("prefix");
        final String unknownCommand = prefix + config.getString("unknown-command");
        final String noPerm = prefix + config.getString("invalid-permission");
        this.runSubCommands(sender, args, unknownCommand, noPerm);
    }

    @Override
    public @NotNull List<String> completeString(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {

        final TagManager tag = this.plugin.getManager(TagManager.class);
        final List<String> tabComplete = new ArrayList<>();

        if (this.getAnnotation().permission().length() > 0 && !sender.hasPermission(this.getAnnotation().permission()))
            return playerList(sender);

        switch (args.length) {
            case 1: {
                tabComplete.addAll(Arrays.asList("create", "delete", "set", "clear"));
                break;
            }

            case 2: {
                if (args[0].equalsIgnoreCase("create")) tabComplete.add("<name>");
                if (args[0].equalsIgnoreCase("delete")) tabComplete.addAll(tag.getTags().stream().map(Tag::getId).collect(Collectors.toList()));
                if (Arrays.asList("set", "clear").contains(args[0])) return playerList(sender);
                break;
            }

            case 3: {
                if (args[0].equalsIgnoreCase("set")) tabComplete.addAll(tag.getTags().stream().map(Tag::getId).collect(Collectors.toList()));
                if (args[0].equalsIgnoreCase("create")) tabComplete.add("<tag>");
                break;
            }

            default:
                tabComplete.addAll(playerList(sender));
                break;
        }

        return tabComplete;
    }
}
