package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.RoseCommand;
import dev.rosewood.rosegarden.command.framework.RoseCommandWrapper;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.command.CommandSender;
import xyz.oribuin.eternaltags.conversion.ConversionPlugin;
import xyz.oribuin.eternaltags.conversion.ValidPlugin;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.PluginConversionManager;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.util.stream.Collectors;

public class ConvertCommand extends RoseCommand {

    public ConvertCommand(RosePlugin rosePlugin, RoseCommandWrapper parent) {
        super(rosePlugin, parent);
    }

    @RoseExecutable
    public void execute(CommandContext context, ConversionPlugin plugin) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final PluginConversionManager manager = this.rosePlugin.getManager(PluginConversionManager.class);
        CommandSender sender = context.getSender();

        // Check if the player arg was provided.
        if (plugin == null) {
            locale.sendMessage(sender, "command-convert-invalid-plugin", StringPlaceholders.single("options", TagsUtils.formatList(ValidPlugin.PLUGINS.values()
                    .stream()
                    .map(ConversionPlugin::getPluginName)
                    .collect(Collectors.toList()), ", ")));
            return;
        }

        int total = manager.convertPlugin(plugin).size();
        locale.sendMessage(sender, "command-convert-converted", StringPlaceholders.single("total", total));
    }


    @Override
    protected String getDefaultName() {
        return "convert";
    }

    @Override
    public String getDescriptionKey() {
        return "command-convert-description";
    }

    @Override
    public String getRequiredPermission() {
        return "eternaltags.convert";
    }

    @Override
    public boolean isPlayerOnly() {
        return false;
    }

}
