package xyz.oribuin.eternaltags.gui.enums;

import org.jetbrains.annotations.Nullable;
import xyz.oribuin.eternaltags.obj.Category;
import xyz.oribuin.eternaltags.obj.Tag;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public enum SortType {
    ALPHABETICAL,
    CUSTOM,
    NONE,
    RANDOM;

    public void sort(List<Tag> tags) {
        tags.removeIf(Objects::isNull); // Remove null tags.

        switch (this) {
            case ALPHABETICAL -> tags.sort(Comparator.comparing(Tag::getName));
            case CUSTOM -> tags.sort(Comparator.comparingInt(Tag::getOrder));
            case RANDOM -> Collections.shuffle(tags);
        }
    }

    public void sortCategories(List<Category> categories) {
        categories.removeIf(Objects::isNull); // Remove null categories.

        switch (this) {
            case ALPHABETICAL -> categories.sort(Comparator.comparing(Category::getDisplayName));
            case CUSTOM -> categories.sort(Comparator.comparingInt(Category::getOrder));
            case RANDOM -> Collections.shuffle(categories);
        }

    }

}