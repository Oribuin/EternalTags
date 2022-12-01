package xyz.oribuin.eternaltags.action;

import dev.rosewood.rosegarden.utils.HexUtils;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerAction extends Action {

    public PlayerAction() {
        super("player");
    }

    @Override
    public void execute(@NotNull Player player, @NotNull StringPlaceholders placeholders) {
        if (this.getMessage().length() == 0)
            return;

        Bukkit.dispatchCommand(player, HexUtils.colorify(PlaceholderAPI.setPlaceholders(player, placeholders.apply(this.getMessage()))));
    }

}
