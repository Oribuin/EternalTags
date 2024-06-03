package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.Argument;
import dev.rosewood.rosegarden.command.framework.ArgumentHandler;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.InputIterator;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagsArgumentHandler extends ArgumentHandler<Tag> {

    private final RosePlugin rosePlugin;
    public TagsArgumentHandler(RosePlugin rosePlugin) {
        super(Tag.class);
        this.rosePlugin = rosePlugin;
    }

    @Override
    public Tag handle(CommandContext context, Argument argument, InputIterator inputIterator) throws HandledArgumentException {
        String input = inputIterator.next();
        Tag value = this.rosePlugin.getManager(TagsManager.class).getTagFromId(input.toLowerCase());
        if (value == null)
            throw new HandledArgumentException("argument-handler-tags", StringPlaceholders.of("input", input));

        return value;
    }

    @Override
    public List<String> suggest(CommandContext context, Argument argument, String[] args) {
        List<String> tags = new ArrayList<>(this.rosePlugin.getManager(TagsManager.class).getCachedTags().keySet());
        if (tags.isEmpty())
            return Collections.singletonList("<no loaded tags>");

        return tags;
    }
}
