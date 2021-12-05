package xyz.oribuin.eternaltags.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.event.TagEquipEvent;
import xyz.oribuin.eternaltags.manager.DataManager;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.eternaltags.manager.TagManager;
import xyz.oribuin.eternaltags.obj.Tag;
import xyz.oribuin.orilibrary.command.SubCommand;
import xyz.oribuin.orilibrary.util.HexUtils;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@SubCommand.Info(
        names = {"random"},
        usage = "/tags random",
        permission = "eternaltags.random"
)
public class SubRandom extends SubCommand {

    private final EternalTags plugin;
    private final MessageManager msg;

    public SubRandom(EternalTags plugin) {
        this.plugin = plugin;
        this.msg = this.plugin.getManager(MessageManager.class);
    }

    @Override
    public void executeArgument(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            this.msg.send(sender, "player-only");
            return;
        }

        final Player player = (Player) sender;

        final List<Tag> tags = this.plugin.getManager(TagManager.class).getPlayersTag(player);
        if (tags.size() == 0) {
            this.msg.send(sender, "no-tags");
            return;
        }

        final int tag = new Random().nextInt(tags.size());

        Optional<Tag> tagOptional = Optional.ofNullable(tags.get(tag));
        if (!tagOptional.isPresent()) {
            this.msg.send(sender, "no-tags");
            return;
        }

        final TagEquipEvent event = new TagEquipEvent(player, tagOptional.get());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        this.plugin.getManager(DataManager.class).updateUser(player.getUniqueId(), tagOptional.get());
        msg.send(sender, "changed-tag", StringPlaceholders.single("tag", HexUtils.colorify(tagOptional.get().getTag())));
    }

}
