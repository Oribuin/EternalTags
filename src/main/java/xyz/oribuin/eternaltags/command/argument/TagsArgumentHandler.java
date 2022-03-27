package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentParser;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentHandler;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentInfo;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TagsArgumentHandler extends RoseCommandArgumentHandler<Tag> {

    public TagsArgumentHandler(RosePlugin rosePlugin) {
        super(rosePlugin, Tag.class);
    }

    @Override
    protected Tag handleInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) throws HandledArgumentException {
        String input = argumentParser.next();
        Optional<Tag> value = this.rosePlugin.getManager(TagsManager.class).matchTagFromID(input.toLowerCase());
        if (value.isEmpty())
            throw new HandledArgumentException("argument-handler-tags", StringPlaceholders.single("input", input));

        return value.get();
    }

    @Override
    protected List<String> suggestInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) {
        argumentParser.next();
        List<Tag> tags = new ArrayList<>(this.rosePlugin.getManager(TagsManager.class).getCachedTags().values());
        if (tags.isEmpty())
            return Collections.singletonList("<no loaded tags>");

        return tags.stream()
                .map(Tag::getId)
                .collect(Collectors.toList());
    }
}
