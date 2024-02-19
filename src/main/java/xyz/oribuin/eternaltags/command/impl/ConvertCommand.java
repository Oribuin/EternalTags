package xyz.oribuin.eternaltags.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.command.CommandSender;
import xyz.oribuin.eternaltags.command.argument.PluginArgumentHandler;
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
    public void execute(CommandContext context) {
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        PluginConversionManager manager = this.rosePlugin.getManager(PluginConversionManager.class);
        ConversionPlugin plugin = context.get("plugin");
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
                .build();
    }

    @Override
    protected ArgumentsDefinition createArgumentsDefinition() {
        return ArgumentsDefinition.builder()
                .required("plugin", new PluginArgumentHandler())
                .build();
    }

}
