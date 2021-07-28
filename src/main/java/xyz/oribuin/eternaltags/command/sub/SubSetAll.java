package xyz.oribuin.eternaltags.command.sub;

import org.bukkit.command.CommandSender;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.command.SubCommand;
import xyz.oribuin.orilibrary.util.HexUtils;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

import java.util.List;
import java.util.Optional;

@SubCommand.Info(
        names = {"setall"},
        usage = "/tags setall <tag>",
        permission = "eternaltags.setall",
        command = CmdTags.class
)
public class SubSetAll extends SubCommand {

    private final EternalTags plugin = (EternalTags) this.getOriPlugin();

    public SubSetAll(EternalTags plugin, CmdTags command) {
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

        final List<Tag> cachedTags = this.plugin.getManager(TagManager.class).getTags();
        final Optional<Tag> tagOptional = cachedTags.stream().filter(tag -> tag.getId().equalsIgnoreCase(args[1])).findAny();

        // Check if the tag exists
        if (!tagOptional.isPresent()) {
            msg.send(sender, "tag-doesnt-exist");
            return;
        }

        this.plugin.getManager(DataManager.class).updateEveryone(tagOptional.get());
        msg.send(sender, "changed-all-tags", StringPlaceholders.single("tag", HexUtils.colorify(tagOptional.get().getTag())));
    }

}
