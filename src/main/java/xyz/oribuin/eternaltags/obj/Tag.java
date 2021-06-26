package xyz.oribuin.eternaltags.obj;

import java.util.ArrayList;
import java.util.List;

public class Tag {

    private final String id;
    private final String name;
    private final String tag;
    private String permission;
    private List<String> description;

    public Tag(final String id, final String name, final String tag) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.description = new ArrayList<>();
        this.permission = "eternaltags.tag." + id.toLowerCase();
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

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }
}
