package xyz.oribuin.eternaltags.gui;

import dev.rosewood.rosegarden.RosePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TagsGUI extends OriGUI {

    public TagsGUI(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    @Override
    public void createGUI(Player player) {
        // TODO
    }

    @Override
    public @NotNull Map<String, Object> getRequiredValues() {
        return new LinkedHashMap<String, Object>() {{
            this.put("#1", "Configure the name at the top of the gui.");
            this.put("menu-name", "EternalTags | %page%/%total%");
            this.put("#2", "Available Options: ALPHABETICAL, CUSTOM, NONE, RANDOM");
            this.put("sort-type", SortType.ALPHABETICAL.name());
            this.put("#3", "Should the player's favourite tag be put first in the gui?");
            this.put("favorites-first", true);

            // Tag Item
            this.put("#4", "The display item for tags");
            this.put("tag.material", Material.NAME_TAG.name());
            this.put("tag.amount", 1);
            this.put("tag.name", "%tag%");
            this.put("tag.lore", Arrays.asList(" &f| &7Click to change your", " &f| &7active tag to %name%", " &f| &7Shift-Click to set as favorite", " &f|", " &f| &7%description%"));
            this.put("tag.glow", true);

            // Next Page Item
            this.put("#5", "The display item for the next page button");
            this.put("next-page.material", Material.PAPER.name());
            this.put("next-page.name", "#00B4DB&lNext Page");

            // Previous Page Item
            this.put("#6", "The display item for the next page button");
            this.put("previous-page.material", Material.PAPER.name());
            this.put("previous-page.name", "#00B4DB&lPrevious Page");
        }};
    }

    @Override
    public int getRows() {
        return 6;
    }

    @Override
    public @NotNull String getMenuName() {
        return "tags-gui";
    }

    @Override
    public @NotNull List<Integer> getPageSlots() {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 45; i++)
            slots.add(i);

        return slots;
    }

    private enum SortType {
        ALPHABETICAL, CUSTOM, NONE, RANDOM;

        /**
         * Match a sort type by their name.
         *
         * @param name The name of the sort type
         * @return A matching type if present.
         */
        public static Optional<SortType> match(String name) {
            return Arrays.stream(SortType.values()).filter(sortType -> sortType.name().equalsIgnoreCase(name)).findFirst();
        }
    }
}
