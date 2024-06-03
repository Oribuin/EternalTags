package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.Argument;
import dev.rosewood.rosegarden.command.framework.ArgumentHandler;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.InputIterator;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Category;

import java.util.List;
import java.util.Map;

public class CategoryArgumentHandler extends ArgumentHandler<Category> {

    private final RosePlugin rosePlugin;

    public CategoryArgumentHandler(RosePlugin rosePlugin) {
        super(Category.class);
        this.rosePlugin = rosePlugin;
    }


    @Override
    public Category handle(CommandContext context, Argument argument, InputIterator inputIterator) throws HandledArgumentException {
        String input = inputIterator.next();
        Category value = this.rosePlugin.getManager(TagsManager.class).getCategory(input.toLowerCase());
        if (value == null || value.isGlobal())
            throw new ArgumentHandler.HandledArgumentException("argument-handler-category", StringPlaceholders.of("input", input));

        return value;
    }

    @Override
    public List<String> suggest(CommandContext context, Argument argument, String[] args) {
        return this.rosePlugin.getManager(TagsManager.class).getCachedCategories()
                .entrySet()
                .stream().filter(entry -> !entry.getValue().isGlobal())
                .map(Map.Entry::getKey)
                .toList();
    }
}
