package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentParser;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentHandler;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentInfo;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import xyz.oribuin.eternaltags.conversion.ConversionPlugin;
import xyz.oribuin.eternaltags.conversion.ValidPlugin;

import java.util.List;
import java.util.Optional;

public class PluginArgumentHandler extends RoseCommandArgumentHandler<ConversionPlugin> {

    public PluginArgumentHandler(RosePlugin rosePlugin) {
        super(rosePlugin, ConversionPlugin.class);
    }

    @Override
    protected ConversionPlugin handleInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) throws HandledArgumentException {
        String input = argumentParser.next();

        Optional<ConversionPlugin> conversion = ValidPlugin.match(input);
        if (conversion.isEmpty())
            throw new HandledArgumentException("argument-handler-plugins", StringPlaceholders.of("input", input));

        return conversion.get();
    }

    @Override
    protected List<String> suggestInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) {
        argumentParser.next();

        return ValidPlugin.PLUGINS
                .keySet()
                .stream()
                .toList();
    }
}
