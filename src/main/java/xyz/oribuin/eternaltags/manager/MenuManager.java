package xyz.oribuin.eternaltags.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import xyz.oribuin.eternaltags.gui.OriGUI;
import xyz.oribuin.eternaltags.gui.TagsGUI;

import java.util.LinkedHashMap;
import java.util.Map;

public class MenuManager extends Manager {

    private Map<String, OriGUI> registeredGUIs;

    public MenuManager(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public void reload() {
        this.registeredGUIs = new LinkedHashMap<String, OriGUI>() {{
            this.put("tags-gui", new TagsGUI(rosePlugin));
        }};

        // Load menu configurations
        this.registeredGUIs.forEach((s, gui) -> gui.loadConfiguration());
    }

    @Override
    public void disable() {
        this.registeredGUIs.clear();
    }

}
