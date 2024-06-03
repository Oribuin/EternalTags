package xyz.oribuin.eternaltags.command.command;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.*;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.command.CommandSender;
import xyz.oribuin.eternaltags.command.argument.TagsArgumentHandlers;
import xyz.oribuin.eternaltags.conversion.ConversionPlugin;
import xyz.oribuin.eternaltags.conversion.ValidPlugin;
import xyz.oribuin.eternaltags.manager.LocaleManager;
import xyz.oribuin.eternaltags.manager.PluginConversionManager;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.util.stream.Collectors;

public class ConvertCommand extends BaseRoseCommand {

    public ConvertCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, ConversionPlugin plugin) {
        final LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        final PluginConversionManager manager = this.rosePlugin.getManager(PluginConversionManager.class);
        CommandSender sender = context.getSender();

        // Check if the player arg was provided.
        if (plugin == null) {
            locale.sendMessage(sender, "command-convert-invalid-plugin", StringPlaceholders.of("options", TagsUtils.formatList(ValidPlugin.PLUGINS.values()
                    .stream()
                    .map(ConversionPlugin::getPluginName)
                    .collect(Collectors.toList()), ", ")));
            return;
        }

        int total = manager.convertPlugin(plugin).size();
        locale.sendMessage(sender, "command-convert-converted", StringPlaceholders.of("total", total));
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("convert")
                .descriptionKey("command-convert-description")
                .permission("eternaltags.convert")
                .playerOnly(false)
                .build();
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder()
                .required("plugin", TagsArgumentHandlers.CONVERSION_PLUGIN)
                .build();
    }
}
