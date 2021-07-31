package xyz.oribuin.eternaltags.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.gui.TagGUI;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.orilibrary.command.SubCommand;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

@SubCommand.Info(
        names = {"search", "find"},
        usage = "/tags search <name>",
        permission = "eternaltags.search"
)
public class SubSearch extends SubCommand {

    private final EternalTags plugin = (EternalTags) this.getOriPlugin();

    public SubSearch(EternalTags plugin, CmdTags command) {
        super(plugin, command);
    }

    @Override
    public void executeArgument(CommandSender sender, String[] args) {

        final MessageManager msg = this.plugin.getManager(MessageManager.class);

        // Check arguments
        if (args.length < 2) {
            msg.send(sender, "invalid-arguments", StringPlaceholders.single("usage", this.getInfo().usage()));
            return;
        }

        if (!(sender instanceof Player)) {
            msg.sendRaw(sender, "player-only");
            return;
        }


        final String keyword = String.join(" ", args).substring(args[0].length() + 1);
        new TagGUI(plugin, (Player) sender, keyword).createGUI();
    }

}
