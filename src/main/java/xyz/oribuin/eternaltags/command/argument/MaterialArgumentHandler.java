package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentParser;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentHandler;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentInfo;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public class MaterialArgumentHandler extends RoseCommandArgumentHandler<Material> {

    public MaterialArgumentHandler(RosePlugin rosePlugin) {
        super(rosePlugin, Material.class);
    }

    @Override
    protected Material handleInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) throws HandledArgumentException {
        String input = argumentParser.next();
        Material value = Material.matchMaterial(input);
        if (value == null)
            throw new HandledArgumentException("argument-handler-material", StringPlaceholders.single("input", input));

        return value;
    }

    @Override
    protected List<String> suggestInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) {
        argumentParser.next();

        return Arrays.stream(Material.values()).map(material -> material.name().toLowerCase()).toList();
    }
}
