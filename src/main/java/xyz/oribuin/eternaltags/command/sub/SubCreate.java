package xyz.oribuin.eternaltags.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.event.TagCreateEvent;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.command.SubCommand;
import xyz.oribuin.orilibrary.libs.jetbrains.annotations.NotNull;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

        // Check arguments
        if (args.length < 3) {
            msg.send(sender, "invalid-arguments", StringPlaceholders.single("usage", this.getAnnotation().usage()));
            return;
        }

        final String name = args[1];
        final String newTag = String.join(" ", args).substring(args[0].length() + name.length() + 2);

        final List<Tag> cachedTags = this.plugin.getManager(TagManager.class).getTags();

        // Check if the tag exists
        if (cachedTags.stream().anyMatch(x -> x.getId().equalsIgnoreCase(name))) {
            msg.send(sender, "tag-exists");
            return;
        }

        // Create the new tag
        final Tag tag = new Tag(name.toLowerCase(), name, newTag);
        tag.setDescription(Collections.singletonList("None"));

        final TagCreateEvent event = new TagCreateEvent(tag);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        this.plugin.getManager(TagManager.class).createTag(tag);
        msg.send(sender, "created-tag", StringPlaceholders.single("tag", newTag));
    }

}
