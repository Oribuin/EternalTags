package xyz.oribuin.eternaltags.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.orilibrary.command.SubCommand;
import xyz.oribuin.orilibrary.util.FileUtils;

@SubCommand.Info(
        names = {"reload"},
        usage = "/tags reload",
        permission = "eternaltags.reload"
)
public class SubReload extends SubCommand {

    private final EternalTags plugin = (EternalTags) this.getOriPlugin();
    private final MessageManager msg = this.plugin.getManager(MessageManager.class);

    public SubReload(EternalTags plugin, CmdTags command) {
        super(plugin, command);
    }

    @Override
    public void executeArgument(CommandSender sender, String[] args) {
        this.plugin.saveDefaultConfig();

        // Reload the plugin.
        this.plugin.reload();

        msg.send(sender, "reload");
    }

}
