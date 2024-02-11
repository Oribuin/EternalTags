package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentParser;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentHandler;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentInfo;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import xyz.oribuin.eternaltags.manager.CategoryManager;
import xyz.oribuin.eternaltags.obj.Category;
import xyz.oribuin.eternaltags.obj.CategoryType;

import java.util.List;
import java.util.stream.Collectors;

public class CategoryArgumentHandler extends RoseCommandArgumentHandler<Category> {

    public CategoryArgumentHandler(RosePlugin rosePlugin) {
        super(rosePlugin, Category.class);
    }

    @Override
    protected Category handleInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) throws HandledArgumentException {
        String input = argumentParser.next();
        Category value = this.rosePlugin.getManager(CategoryManager.class).getCategory(input.toLowerCase());
        if (value == null || value.getType() == CategoryType.GLOBAL)
            throw new HandledArgumentException("argument-handler-category", StringPlaceholders.of("input", input));

        return value;
    }

    @Override
    protected List<String> suggestInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) {
        argumentParser.next();

        return this.rosePlugin.getManager(CategoryManager.class)
                .getCategories()
                .stream()
                .filter(category -> category.getType() != CategoryType.GLOBAL)
                .map(Category::getId)
                .collect(Collectors.toList());
    }

}
