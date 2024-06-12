package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedConfigurationSection;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.manager.Manager;
import xyz.oribuin.eternaltags.obj.Category;
import xyz.oribuin.eternaltags.obj.CategoryType;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryManager extends Manager {

    private final Map<String, Category> cachedCategories = new HashMap<>();
    private File categoryFile;
    private CommentedFileConfiguration categoryConfig;

    public CategoryManager(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public void reload() {
        this.categoryFile = TagsUtils.createFile(this.rosePlugin, "categories.yml");
        this.categoryConfig = CommentedFileConfiguration.loadConfiguration(this.categoryFile);
        this.cachedCategories.clear();

        CommentedConfigurationSection section = this.categoryConfig.getConfigurationSection("categories");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            this.loadCategory(this.categoryConfig, key);
        }

        List<Category> global = this.getByType(CategoryType.GLOBAL);
        if (global.size() > 1) {
            this.rosePlugin.getLogger().severe("You have more than one global category, this is not allowed. Please remove the extra global categories.");
        }

        List<Category> defaults = this.getByType(CategoryType.DEFAULT);
        if (defaults.size() > 1) {
            this.rosePlugin.getLogger().severe("You have more than one default category, this is not allowed. Please remove the extra default categories.");
        }
    }

    /**
     * Load a specific category from a configuration section
     *
     * @param section The configuration section
     * @param key     The key to load.
     */
    private void loadCategory(CommentedConfigurationSection section, String key) {
        String newKey = "categories." + key;

        boolean global = section.getBoolean(newKey + ".global", false);
        boolean defaultCategory = section.getBoolean(newKey + ".default", false);
        CategoryType categoryType = TagsUtils.getEnum(CategoryType.class, section.getString(newKey + ".type", "CUSTOM"));
        if (global) categoryType = CategoryType.GLOBAL;
        if (defaultCategory) categoryType = CategoryType.DEFAULT;

        Category category = new Category(key.toLowerCase());
        category.setDisplayName(section.getString(newKey + ".display-name", newKey));
        category.setType(categoryType);
        category.setOrder(section.getInt(newKey + ".order", -1));
        category.setPermission(section.getString(newKey + ".permission", null));
        category.setBypassPermission(section.getBoolean(newKey + ".unlocks-all-tags", false));

        this.cachedCategories.put(key.toLowerCase(), category);
    }

    /**
     * Get the first category of a specific type
     *
     * @param categoryType The type of category
     * @return The first category
     */
    public Category getFirst(CategoryType categoryType) {
        if (this.cachedCategories.isEmpty()) return null;

        return this.cachedCategories.values()
                .stream()
                .filter(category -> category.getType() == categoryType)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get a category from the cache.
     *
     * @param id The id of the category
     * @return The category
     */
    public Category getCategory(String id) {
        if (this.cachedCategories.isEmpty()) return null;

        return this.cachedCategories.get(id.toLowerCase());
    }

    /**
     * Save a category with the configuration file
     *
     * @param category The category to save
     */
    public void save(Category category) {
        if (category == null || this.categoryConfig == null) return;

        this.categoryConfig.set("categories.display-name", category.getDisplayName());
        this.categoryConfig.set("categories.type", category.getType().name());
        this.categoryConfig.set("categories.order", category.getOrder());
        this.categoryConfig.set("categories.permission", category.getPermission());
        this.categoryConfig.set("categories.unlocks-all-tags", category.isBypassPermission());

        this.categoryConfig.save(this.categoryFile);
        this.cachedCategories.put(category.getId().toLowerCase(), category);
    }

    /**
     * Get a list of categories associated with a specific type
     *
     * @param type The type of category
     * @return A list of categories
     */
    public List<Category> getByType(CategoryType type) {
        if (this.cachedCategories.isEmpty()) return null;

        return this.cachedCategories.values()
                .stream()
                .filter(category -> category.getType() == type)
                .toList();
    }

    /**
     * Get all saved categories in the cache
     *
     * @return A list of categories
     */
    public List<Category> getCategories() {
        return this.cachedCategories.values().stream().toList();
    }

    /**
     * Check if the category manager is enabled
     *
     * @return If categories are enabled
     */
    public boolean isEnabled() {
        return !this.cachedCategories.isEmpty();
    }

    @Override
    public void disable() {

    }

}
