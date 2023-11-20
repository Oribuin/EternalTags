package xyz.oribuin.eternaltags.action;

import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CloseAction extends Action {

    public CloseAction() {
        super("close");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void execute(@NotNull Player player, @NotNull StringPlaceholders placeholders) {
        player.closeInventory();
    }


}
