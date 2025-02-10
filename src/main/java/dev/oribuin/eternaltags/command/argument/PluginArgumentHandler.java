package dev.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.command.framework.Argument;
import dev.rosewood.rosegarden.command.framework.ArgumentHandler;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.InputIterator;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import dev.oribuin.eternaltags.conversion.ConversionPlugin;
import dev.oribuin.eternaltags.conversion.ConversionPluginRegistry;

import java.util.List;
import java.util.Optional;

public class PluginArgumentHandler extends ArgumentHandler<ConversionPlugin> {

    public PluginArgumentHandler() {
        super(ConversionPlugin.class);
    }

    @Override
    public ConversionPlugin handle(CommandContext context, Argument argument, InputIterator inputIterator) throws HandledArgumentException {
        String input = inputIterator.next();

        ConversionPlugin conversionPlugin = ConversionPluginRegistry.match(input);
        if (conversionPlugin != null) return conversionPlugin;
        
        throw new HandledArgumentException("argument-handler-plugins", StringPlaceholders.of("input", input));
    }

    @Override
    public List<String> suggest(CommandContext context, Argument argument, String[] args) {
        return ConversionPluginRegistry.PLUGINS
                .keySet()
                .stream()
                .toList();
    }

}
