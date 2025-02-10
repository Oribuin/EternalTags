package dev.oribuin.eternaltags.gui.enums;

import dev.oribuin.eternaltags.obj.Tag;

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

}