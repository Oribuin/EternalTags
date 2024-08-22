package xyz.oribuin.eternaltags.action;

import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;

public class CloseAction extends Action {

    public CloseAction() {
        super("close");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void execute(Player player, StringPlaceholders placeholders) {
        player.closeInventory();
    }


}
