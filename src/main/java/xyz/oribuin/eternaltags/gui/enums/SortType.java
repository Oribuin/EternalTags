package xyz.oribuin.eternaltags.gui.enums;

import org.jetbrains.annotations.Nullable;
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
            case ALPHABETICAL -> tags.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            case CUSTOM -> tags.sort(Comparator.comparingInt(Tag::getOrder));
            case RANDOM -> Collections.shuffle(tags);
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