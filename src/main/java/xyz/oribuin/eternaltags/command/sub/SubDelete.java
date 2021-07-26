package xyz.oribuin.eternaltags.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.event.TagDeleteEvent;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.command.SubCommand;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

import java.util.List;
import java.util.Optional;

@SubCommand.Info(
        names = {"delete"},
        usage = "/tags delete <name> ",
        permission = "eternaltags.delete",
        command = CmdTags.class
)
public class SubDelete extends SubCommand {

    private final EternalTags plugin = (EternalTags) this.getOriPlugin();

    public SubDelete(EternalTags plugin, CmdTags command) {
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

        final String name = args[1];
        final List<Tag> cachedTags = this.plugin.getManager(TagManager.class).getTags();
        final Optional<Tag> optionalTag = cachedTags.stream().filter(x -> x.getId().equalsIgnoreCase(name)).findAny();

        // Check if the tag exists
        if (!optionalTag.isPresent()) {
            msg.send(sender, "tag-doesnt-exist");
            return;
        }

        final TagDeleteEvent event = new TagDeleteEvent(optionalTag.get());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        // Delete the tag
        this.plugin.getManager(TagManager.class).deleteTag(optionalTag.get());
        msg.send(sender, "deleted-tag", StringPlaceholders.single("tag", name));
    }

}
