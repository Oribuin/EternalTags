package xyz.oribuin.eternaltags.obj;

// Practice

public class Tag {

    private final String id;
    private final String name;
    private final String tag;
    private String description = null;

    public Tag(final String id, final String name, final String tag) {
        this.id = id;
        this.name = name;
        this.tag = tag;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
