package xyz.oribuin.eternaltags.obj;

import xyz.oribuin.eternaltags.conversion.ValidPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;

public enum EditOption {
    TAG(Tag::setTag),
    NAME(Tag::setName),
    PERMISSION(Tag::setPermission),
    DESCRIPTION((tag, description) -> tag.setDescription(Collections.singletonList(description)));

    private final BiConsumer<Tag, String> action;

    EditOption(BiConsumer<Tag, String> action) {
        this.action = action;
    }

    public BiConsumer<Tag, String> getAction() {
        return action;
    }

    /**
     * Match a valid plugin by the name
     *
     * @param name The name of the plugin
     * @return The enum if present.
     */
    public static Optional<EditOption> match(String name) {
        return Arrays.stream(EditOption.values()).filter(value -> value.name().equalsIgnoreCase(name)).findFirst();
    }
}
