package dev.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.command.framework.Argument;
import dev.rosewood.rosegarden.command.framework.ArgumentHandler;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.InputIterator;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MaterialArgumentHandler extends ArgumentHandler<Material> {

    public MaterialArgumentHandler() {
        super(Material.class);
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
        return Arrays.stream(Material.values())
                .filter(Material::isItem)
                .map(material -> material.name().toLowerCase())
                .collect(Collectors.toList());
    }

}
