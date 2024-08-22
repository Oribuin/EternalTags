package xyz.oribuin.eternaltags.action;

import dev.rosewood.rosegarden.utils.HexUtils;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class MessageAction extends Action {

    public MessageAction() {
        super("message");
    }

    @Override
    public void execute(Player player, StringPlaceholders placeholders) {
        if (this.getMessage().length() == 0)
            return;

        player.sendMessage(HexUtils.colorify(PlaceholderAPI.setPlaceholders(player, placeholders.apply(this.getMessage()))));
    }
}
