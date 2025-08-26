package dev.oribuin.eternaltags.command.impl;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentsDefinition;
import dev.rosewood.rosegarden.command.framework.BaseRoseCommand;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.CommandInfo;
import dev.rosewood.rosegarden.command.framework.annotation.RoseExecutable;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.command.CommandSender;
import dev.oribuin.eternaltags.command.argument.PluginArgumentHandler;
import dev.oribuin.eternaltags.conversion.ConversionPlugin;
import dev.oribuin.eternaltags.conversion.ConversionPluginRegistry;
import dev.oribuin.eternaltags.manager.LocaleManager;
import dev.oribuin.eternaltags.util.TagsUtils;

import java.util.stream.Collectors;

public class ConvertCommand extends BaseRoseCommand {

    public ConvertCommand(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @RoseExecutable
    public void execute(CommandContext context, ConversionPlugin plugin) {
        LocaleManager locale = this.rosePlugin.getManager(LocaleManager.class);
        CommandSender sender = context.getSender();

        // Check if the player arg was provided.
        if (plugin == null) {
            String options = ConversionPluginRegistry.PLUGINS.values().stream()
                    .map(x -> x.get().getPluginName())
                    .collect(Collectors.joining(", "));
            
            locale.sendMessage(sender, "command-convert-invalid-plugin", StringPlaceholders.of("options", options));
            return;
        }

        int total = plugin.convert().size();
        locale.sendMessage(sender, "command-convert-converted", StringPlaceholders.of("total", total));
    }

    @Override
    protected CommandInfo createCommandInfo() {
        return CommandInfo.builder("convert")
                .descriptionKey("command-convert-description")
                .permission("eternaltags.convert")
                .arguments(this.createArguments())
                .build();
    }

    private ArgumentsDefinition createArguments() {
        return ArgumentsDefinition.builder()
                .required("plugin", new PluginArgumentHandler())
                .build();
    }

}
