package xyz.oribuin.eternaltags.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.orilibrary.command.SubCommand;
import xyz.oribuin.orilibrary.libs.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.CompletableFuture;

@SubCommand.Info(
        names = {"reload"},
        usage = "/tags reload",
        permission = "eternaltags.reload",
        command = CmdTags.class
)
public class SubReload extends SubCommand {

    private final EternalTags plugin = (EternalTags) this.getOriPlugin();

    public SubReload(EternalTags plugin, CmdTags command) {
        super(plugin, command);
    }

    @Override
    public void executeArgument(@NotNull CommandSender sender, @NotNull String[] args) {

        final MessageManager msg = this.plugin.getManager(MessageManager.class);
        final DataManager data = this.plugin.getManager(DataManager.class);

        // Reload gui config
        final File menuFolder = new File(plugin.getDataFolder(), "menus");
        YamlConfiguration.loadConfiguration(new File(menuFolder, "tag-menu.yml"));

        // Reload main config files
        this.plugin.getManager(TagManager.class).enable();
        msg.enable();

        // Disable then re enable the data manager
        CompletableFuture.runAsync(data::disable).thenRunAsync(data::enable);

        msg.send(sender, "reload");
    }

}
