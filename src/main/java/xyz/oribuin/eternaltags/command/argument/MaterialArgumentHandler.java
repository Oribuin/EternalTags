package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.Argument;
import dev.rosewood.rosegarden.command.framework.ArgumentHandler;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.InputIterator;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public class MaterialArgumentHandler extends ArgumentHandler<Material> {

    private final RosePlugin rosePlugin;

    public MaterialArgumentHandler(RosePlugin rosePlugin) {
        super(Material.class);
        this.rosePlugin = rosePlugin;
    }

    @Override
    public Material handle(CommandContext context, Argument argument, InputIterator inputIterator) throws HandledArgumentException {
        String input = inputIterator.next();
        Material value = Material.matchMaterial(input);
        if (value == null)
            throw new HandledArgumentException("argument-handler-material", StringPlaceholders.of("input", input));

        return value;
    }

    @Override
    public List<String> suggest(CommandContext context, Argument argument, String[] args) {
        return Arrays.stream(Material.values()).map(material -> material.name().toLowerCase()).toList();
    }
}
