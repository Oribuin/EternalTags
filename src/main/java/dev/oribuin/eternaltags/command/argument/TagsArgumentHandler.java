package dev.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.command.framework.Argument;
import dev.rosewood.rosegarden.command.framework.ArgumentHandler;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.InputIterator;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;
import dev.oribuin.eternaltags.EternalTags;
import dev.oribuin.eternaltags.manager.TagsManager;
import dev.oribuin.eternaltags.obj.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagsArgumentHandler extends ArgumentHandler<Tag> {

    public TagsArgumentHandler() {
        super(Tag.class);
    }

    @Override
    public Tag handle(CommandContext context, Argument argument, InputIterator inputIterator) throws HandledArgumentException {
        String input = inputIterator.next();
        Tag value = EternalTags.get().getManager(TagsManager.class).getTagFromId(input.toLowerCase());
        if (value == null)
            throw new HandledArgumentException("argument-handler-tags", StringPlaceholders.of("input", input));

        return value;
    }

    @Override
    public List<String> suggest(CommandContext context, Argument argument, String[] args) {
        TagsManager manager = EternalTags.get().getManager(TagsManager.class);

        if (!(context.getSender() instanceof Player player)) {
            List<String> tags = new ArrayList<>(manager.getCachedTags().keySet());

            if (tags.isEmpty())
                return Collections.singletonList("<no loaded tags>");

            return tags;
        }

        List<String> tags = new ArrayList<>(manager.getPlayerTags(player)).stream().map(Tag::getId).toList();
        if (tags.isEmpty())
            return Collections.singletonList("<no tags>");

        return tags;
    }

}
