package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentParser;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentHandler;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentInfo;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.apache.commons.lang.StringUtils;
import xyz.oribuin.eternaltags.obj.EditOption;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TagOptionArgumentHandler extends RoseCommandArgumentHandler<EditOption> {

    public TagOptionArgumentHandler(RosePlugin rosePlugin) {
        super(rosePlugin, EditOption.class);
    }

    @Override
    protected EditOption handleInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) throws HandledArgumentException {
        String input = argumentParser.next();
        Optional<EditOption> value = EditOption.match(input);
        if (value.isEmpty())
            throw new HandledArgumentException("argument-handler-edit-option", StringPlaceholders.single("input", input));

        return value.get();
    }

    @Override
    protected List<String> suggestInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) {
        argumentParser.next();

        return Arrays.stream(EditOption.values())
                .map(EditOption::name)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }
}
