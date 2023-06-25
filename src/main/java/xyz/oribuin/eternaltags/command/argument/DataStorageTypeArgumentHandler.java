package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentParser;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentHandler;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentInfo;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import xyz.oribuin.eternaltags.command.model.DataStorageType;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.util.Arrays;
import java.util.List;

public class DataStorageTypeArgumentHandler extends RoseCommandArgumentHandler<DataStorageType> {

    public DataStorageTypeArgumentHandler(RosePlugin rosePlugin) {
        super(rosePlugin, DataStorageType.class);
    }

    @Override
    protected DataStorageType handleInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) throws HandledArgumentException {
        String input = argumentParser.next();
        DataStorageType value = TagsUtils.getEnum(DataStorageType.class, input);

        if (value == null)
            throw new HandledArgumentException("argument-handler-data-storage-type", StringPlaceholders.of("input", input));

        return value;
    }

    @Override
    protected List<String> suggestInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) {
        argumentParser.next();
        return Arrays.stream(DataStorageType.values()).map(type -> type.name().toLowerCase()).toList();
    }

}
