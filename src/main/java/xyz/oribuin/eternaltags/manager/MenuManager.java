package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import xyz.oribuin.eternaltags.gui.FavouritesGUI;
import xyz.oribuin.eternaltags.gui.OriGUI;
import xyz.oribuin.eternaltags.gui.TagsGUI;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class MenuManager extends Manager {

    private Map<String, OriGUI> registeredGUIs;

    public MenuManager(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public void reload() {
        this.registeredGUIs = new LinkedHashMap<String, OriGUI>() {{
            this.put("tags-gui", new TagsGUI(rosePlugin));
            this.put("favorites-gui", new FavouritesGUI(rosePlugin));
        }};

        // Load menu configurations
        this.registeredGUIs.forEach((s, gui) -> gui.loadConfiguration());
    }

    /**
     * Find a menu from the id
     *
     * @param id The id of the menu
     * @return The GUI if present
     */
    public Optional<OriGUI> matchMenu(String id) {
        return Optional.ofNullable(this.registeredGUIs.getOrDefault(id, null));
    }

    @Override
    public void disable() {
        this.registeredGUIs.clear();
    }

}
