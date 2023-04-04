package xyz.oribuin.eternaltags.obj;

import java.util.List;

// This will have to stay as it is for compatibility reasons.
public class TagDescription {

    private final List<String> description;

    public TagDescription(final List<String> description) {
        this.description = description;
    }

    public List<String> getDescription() {
        return description;
    }

}
