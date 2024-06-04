package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.command.framework.Argument;
import dev.rosewood.rosegarden.command.framework.ArgumentHandler;
import dev.rosewood.rosegarden.command.framework.CommandContext;
import dev.rosewood.rosegarden.command.framework.InputIterator;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import xyz.oribuin.eternaltags.EternalTags;
import xyz.oribuin.eternaltags.manager.CategoryManager;
import xyz.oribuin.eternaltags.obj.Category;
import xyz.oribuin.eternaltags.obj.CategoryType;

import java.util.List;

public class CategoryArgumentHandler extends ArgumentHandler<Category> {

    public CategoryArgumentHandler() {
        super(Category.class);
    }

    @Override
    public Category handle(CommandContext context, Argument argument, InputIterator inputIterator) throws HandledArgumentException {
        String input = inputIterator.next();
        Category value = EternalTags.getInstance().getManager(CategoryManager.class).getCategory(input.toLowerCase());
        if (value == null || value.getType() == CategoryType.GLOBAL)
            throw new HandledArgumentException("argument-handler-category", StringPlaceholders.of("input", input));

        return value;
    }

    @Override
    public List<String> suggest(CommandContext context, Argument argument, String[] args) {
        return EternalTags.getInstance().getManager(CategoryManager.class).getCategories()
                .stream()
                .filter(category -> category.getType() != CategoryType.GLOBAL)
                .map(Category::getId)
                .toList();
    }

}
