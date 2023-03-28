package xyz.oribuin.eternaltags.gui.enums;

import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.obj.Category;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public enum SortType {
    ALPHABETICAL,
    CUSTOM,
    NONE,
    RANDOM;

    public void sort(List<Tag> tags) {
        switch (this) {
            case ALPHABETICAL -> tags.sort(Comparator.comparing(Tag::getName));
            case CUSTOM -> tags.sort(Comparator.comparingInt(Tag::getOrder));
            case RANDOM -> Collections.shuffle(tags);
        }
    }

    public void sortCategories(List<Category> categories) {
        switch (this) {
            case ALPHABETICAL -> categories.sort(Comparator.comparing(Category::getDisplayName));
            case CUSTOM -> categories.sort(Comparator.comparingInt(Category::getOrder));
            case RANDOM -> Collections.shuffle(categories);
        }

    }

    /**
     * Match a sort type by their name.
     *
     * @param name The name of the sort type
     * @return A matching type if present.
     */
    public static @Nullable SortType match(@Nullable String name) {
        if (name == null)
            return null;

        for (SortType type : values()) {
            if (type.name().equalsIgnoreCase(name))
                return type;
        }

        return null;
    }
}