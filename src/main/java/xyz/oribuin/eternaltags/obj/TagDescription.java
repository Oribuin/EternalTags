package xyz.oribuin.eternaltags.obj;

import java.util.List;

public class TagDescription {

    private final List<String> description;

    public TagDescription(final List<String> description) {
        this.description = description;
    }

    public List<String> getDescription() {
        return description;
    }

}
