package xyz.oribuin.eternaltags.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.command.CmdTags;
import xyz.oribuin.eternaltags.manager.MessageManager;
import xyz.oribuin.orilibrary.command.SubCommand;
import xyz.oribuin.orilibrary.util.HexUtils;
import xyz.oribuin.orilibrary.util.StringPlaceholders;

import java.util.concurrent.atomic.AtomicInteger;

@SubCommand.Info(
        names = {"view"},
        usage = "/tags view <amount> <tag>",
        permission = "eternaltags.view"
)
public class SubView extends SubCommand {

    private final EternalTags plugin;
    private final MessageManager msg;

    public SubView(EternalTags plugin) {
        this.plugin = plugin;
        this.msg = this.plugin.getManager(MessageManager.class);
    }

    @Override
    public void executeArgument(CommandSender sender, String[] args) {

        if (args.length < 3) {
            this.msg.send(sender, "invalid-arguments", StringPlaceholders.single("usage", this.getInfo().usage()));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            this.msg.send(sender, "invalid-num");
            return;
        }

        if (amount <= 0 || amount >= 16) {
            this.msg.send(sender, "invalid-num");
            return;
        }

        final String argument = String.join(" ", args).substring(args[0].length() + args[1].length() + 2);
        AtomicInteger i = new AtomicInteger();
        Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, (task) -> {
            if (i.get() >= amount) {
                task.cancel();
                return;
            }

            i.getAndIncrement();
            sender.sendMessage(HexUtils.colorify(argument));
        }, 0, 1);
    }

}
