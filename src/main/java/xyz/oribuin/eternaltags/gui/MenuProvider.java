package xyz.oribuin.eternaltags.gui;

import xyz.oribuin.eternaltags.gui.menu.CategoryGUI;
import xyz.oribuin.eternaltags.gui.menu.FavouritesGUI;
import xyz.oribuin.eternaltags.gui.menu.TagsGUI;

import java.util.HashMap;
import java.util.Map;

public enum MenuProvider {
    ;

    private final static Map<Class<? extends PluginMenu>, PluginMenu> menuCache = new HashMap<>();

    static {
        menuCache.put(TagsGUI.class, new TagsGUI());
        menuCache.put(FavouritesGUI.class, new FavouritesGUI());
        menuCache.put(CategoryGUI.class, new CategoryGUI());

        menuCache.forEach((aClass, pluginMenu) -> pluginMenu.load());
    }

    public static void reload() {
        menuCache.forEach((aClass, pluginMenu) -> pluginMenu.load());
    }

    /**
     * Get the instance of the menu.
     *
     * @param <T> the type of the menu.
     * @return the instance of the menu.
     */
    @SuppressWarnings("unchecked")
    public static<T extends PluginMenu> T get(Class<T> menuClass) {
        if (menuCache.containsKey(menuClass)) {
            return (T) menuCache.get(menuClass);
        }

        try {
            T menu = menuClass.getDeclaredConstructor().newInstance();
            menu.load();
            menuCache.put(menuClass, menu);
            return menu;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create instance of " + menuClass.getName(), e);
        }
    }

}