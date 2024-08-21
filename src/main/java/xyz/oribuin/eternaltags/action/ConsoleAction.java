package xyz.oribuin.eternaltags.action;

import dev.rosewood.rosegarden.utils.StringPlaceholders;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ConsoleAction extends Action {

    public ConsoleAction() {
        super("console");
    }

    @Override
    public void execute(Player player, StringPlaceholders placeholders) {
        if (this.getMessage().length() == 0)
            return;

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceholderAPI.setPlaceholders(player, placeholders.apply(this.getMessage())));
    }

}
