package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentParser;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentHandler;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentInfo;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import xyz.oribuin.eternaltags.conversion.ValidPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PluginArgumentHandler extends RoseCommandArgumentHandler<ValidPlugin> {

    public PluginArgumentHandler(RosePlugin rosePlugin) {
        super(rosePlugin, ValidPlugin.class);
    }

    @Override
    protected ValidPlugin handleInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) throws HandledArgumentException {
        String input = argumentParser.next();
        Optional<ValidPlugin> value = ValidPlugin.match(input);
        if (!value.isPresent())
            throw new HandledArgumentException("argument-handler-plugins", StringPlaceholders.single("input", input));

        return value.get();
    }

    @Override
    protected List<String> suggestInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) {
        argumentParser.next();

        return Arrays.stream(ValidPlugin.values())
                .map(ValidPlugin::getDisplay)
                .collect(Collectors.toList());

    }
}
