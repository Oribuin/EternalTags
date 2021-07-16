package xyz.oribuin.eternaltags.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.event.TagEquipEvent;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.command.SubCommand;

import xyz.oribuin.orilibrary.util.StringPlaceholders;

import java.util.List;
import java.util.Optional;

@SubCommand.Info(
        names = {"set"},
        usage = "/tags set <player> <tag>",
        permission = "eternaltags.set",
        command = CmdTags.class
)
public class SubSet extends SubCommand {

    private final EternalTags plugin = (EternalTags) this.getOriPlugin();

    public SubSet(EternalTags plugin, CmdTags command) {
        super(plugin, command);
    }

    @Override
    public void executeArgument( CommandSender sender,  String[] args) {

        final MessageManager msg = this.plugin.getManager(MessageManager.class);

        // Check arguments
        if (args.length != 3) {
            msg.send(sender, "invalid-arguments", StringPlaceholders.single("usage", this.getAnnotation().usage()));
            return;
        }

        final Player player = Bukkit.getPlayer(args[1]);

        if (player == null) {
            msg.send(sender, "invalid-player");
            return;
        }

        final List<Tag> cachedTags = this.plugin.getManager(TagManager.class).getTags();

        final Optional<Tag> tagOptional = cachedTags.stream().filter(tag -> tag.getId().equalsIgnoreCase(args[2])).findAny();

        // Check if the tag exists
        if (!tagOptional.isPresent()) {
            msg.send(sender, "tag-doesnt-exist");
            return;
        }

        final TagEquipEvent event = new TagEquipEvent(player, tagOptional.get());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        this.plugin.getManager(DataManager.class).updateUser(player.getUniqueId(), tagOptional.get());
        msg.send(sender, "changed-tag", StringPlaceholders.single("tag", tagOptional.get().getTag()));
    }

}
