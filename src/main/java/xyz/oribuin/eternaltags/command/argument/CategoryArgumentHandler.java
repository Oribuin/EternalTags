package xyz.oribuin.eternaltags.command.argument;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.command.framework.ArgumentParser;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentHandler;
import dev.rosewood.rosegarden.command.framework.RoseCommandArgumentInfo;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import xyz.oribuin.eternaltags.manager.TagsManager;
import xyz.oribuin.eternaltags.obj.Category;

import java.util.List;
import java.util.Map;

public class CategoryArgumentHandler extends RoseCommandArgumentHandler<Category> {

    public CategoryArgumentHandler(RosePlugin rosePlugin) {
        super(rosePlugin, Category.class);
    }

    @Override
    protected Category handleInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) throws HandledArgumentException {
        String input = argumentParser.next();
        Category value = this.rosePlugin.getManager(TagsManager.class).getCategory(input.toLowerCase());
        if (value == null || value.isGlobal())
            throw new HandledArgumentException("argument-handler-category", StringPlaceholders.single("input", input));

        return value;
    }

    @Override
    protected List<String> suggestInternal(RoseCommandArgumentInfo argumentInfo, ArgumentParser argumentParser) {
        argumentParser.next();

        return this.rosePlugin.getManager(TagsManager.class).getCachedCategories()
                .entrySet()
                .stream().filter(entry -> !entry.getValue().isGlobal())
                .map(Map.Entry::getKey)
                .toList();
    }

}
