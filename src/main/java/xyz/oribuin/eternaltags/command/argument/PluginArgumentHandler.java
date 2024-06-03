package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.Argument;
import dev.rosewood.rosegarden.command.framework.ArgumentHandler;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.InputIterator;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import xyz.oribuin.eternaltags.conversion.ConversionPlugin;
import xyz.oribuin.eternaltags.conversion.ValidPlugin;

import java.util.List;
import java.util.Optional;

public class PluginArgumentHandler extends ArgumentHandler<ConversionPlugin> {

    private final RosePlugin rosePlugin;
    public PluginArgumentHandler(RosePlugin rosePlugin) {
        super(ConversionPlugin.class);
        this.rosePlugin = rosePlugin;
    }

    @Override
    public ConversionPlugin handle(CommandContext context, Argument argument, InputIterator inputIterator) throws HandledArgumentException {
        String input = inputIterator.next();

        Optional<ConversionPlugin> conversion = ValidPlugin.match(input);
        if (conversion.isEmpty())
            throw new HandledArgumentException("argument-handler-plugins", StringPlaceholders.of("input", input));

        return conversion.get();
    }

    @Override
    public List<String> suggest(CommandContext context, Argument argument, String[] args) {
        return ValidPlugin.PLUGINS
                .keySet()
                .stream()
                .toList();
    }
}
