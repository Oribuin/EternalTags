package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.gui.FavouritesGUI;
import xyz.oribuin.eternaltags.gui.PluginGUI;
import xyz.oribuin.eternaltags.gui.TagsGUI;

import java.util.LinkedHashMap;
import java.util.Map;

public class MenuManager extends Manager {

    private @NotNull Map<Class<? extends PluginGUI>, PluginGUI> registeredMenus = new LinkedHashMap<>();

    public MenuManager(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public void reload() {
        this.registeredMenus = new LinkedHashMap<>() {{
            this.put(TagsGUI.class, new TagsGUI(rosePlugin));
            this.put(FavouritesGUI.class, new FavouritesGUI(rosePlugin));
        }};

        this.registeredMenus.forEach((name, gui) -> gui.load());
    }

    @SuppressWarnings("unchecked")
    public <T extends PluginGUI> T get(Class<T> menuClass) {
        if (this.registeredMenus.containsKey(menuClass)) {
            return (T) this.registeredMenus.get(menuClass);
        }

        return null;
    }


    @Override
    public void disable() {
        this.registeredMenus.clear();
    }

}
