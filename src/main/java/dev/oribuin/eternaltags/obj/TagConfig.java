package dev.oribuin.eternaltags.obj;

import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public record TagConfig(File file, CommentedFileConfiguration config, Map<String, Tag> tags) {

    /**
     * Create a new tag config from a file
     *
     * @param file The file to create the config from
     * @return The config file if available, null otherwise
     */
    public static @Nullable TagConfig from(@NotNull File file) {
        if (!file.getName().endsWith(".yml")) return null;

        CommentedFileConfiguration config = CommentedFileConfiguration.loadConfiguration(file);
        CommentedConfigurationSection section = config.getConfigurationSection("tags");
        if (section == null) return null;

        Map<String, Tag> tags = new HashMap<>();
        section.getKeys(false).forEach(tagId -> {
            Tag tag = Tag.fromConfig(section, tagId);
            if (tag == null) return;

            tags.put(tag.getId().toLowerCase(), tag);
        });

        return new TagConfig(file, config, tags);
    }

    /**
     * Write all the tags in the config
     */
    public void writeAll() {
        CommentedConfigurationSection section = this.getTagSection();
        this.tags.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .forEach(tag -> Tag.SERIALIZER.write(section, tag.getId(), tag));
        
        this.config.save(this.file);
    }

    /**
     * Write and save a tag into the config
     *
     * @param tag The tag to save
     */
    public void write(@NotNull Tag tag) {
        CommentedConfigurationSection section = this.getTagSection();

        Tag.SERIALIZER.write(section, tag.getId(), tag);
        this.config.save(this.file);
        this.tags.put(tag.getId(), tag);

    }

    /**
     * Write and save a tag into the config
     *
     * @param tagId The tag to save
     */
    public void write(@NotNull String tagId) {
        Tag tag = this.tags.get(tagId);
        if (tag != null) this.write(tag);
    }

    /**
     * Delete a tag from the tag config
     *
     * @param tag The tag to delete
     */
    public void delete(@NotNull Tag tag) {
        CommentedConfigurationSection section = this.getTagSection();
        section.set(tag.getId(), null);
        this.config.save(this.file);
        this.tags.remove(tag.getId());
    }

    /**
     * Delete a tag from the tag config
     *
     * @param tagId The tag to delete
     */
    public void delete(@NotNull String tagId) {
        Tag tag = this.tags.get(tagId);
        if (tag != null) this.delete(tag);
    }

    /**
     * Save all the tag configs to the plugin
     */
    public void save() {
        CommentedConfigurationSection section = this.getTagSection();
        this.tags.forEach((string, tag) -> Tag.SERIALIZER.write(section, tag.getId(), tag));
        this.config.save(this.file);
    }

    /**
     * Get a tag from a config file via the id of the tag
     *
     * @param id The id to check
     * @return The tag if available to retrieve
     */
    public @Nullable Tag from(@NotNull String id) {
        return this.tags.get(id.toLowerCase());
    }

    /**
     * Check if the tag config has the existing tag
     *
     * @param id The id of the tag to check
     * @return true if contains, false otherwise
     */
    public boolean has(@NotNull String id) {
        return this.tags.containsKey(id.toLowerCase());
    }

    /**
     * Search for a taginside the category
     *
     * @param text The text to search through
     * @return The condition for finding a tag
     */
    public static Predicate<Tag> search(@NotNull String text) {
        String textLower = text.toLowerCase();

        return tag -> tag.getId().toLowerCase().contains(textLower)
                || tag.getContent().toLowerCase().contains(textLower);

    }

    @Override
    public String toString() {
        return "TagConfig[" +
                "file=" + file + ", " +
                "config=" + config + ", " +
                "tags=" + tags + ']';
    }

    @NotNull
    private CommentedConfigurationSection getTagSection() {
        CommentedConfigurationSection section = this.config.getConfigurationSection("tags");
        if (section != null) return section;

        return this.config.createSection("tags");
    }

}
