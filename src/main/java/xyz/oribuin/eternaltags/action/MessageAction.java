package xyz.oribuin.eternaltags.action;

import dev.rosewood.rosegarden.utils.HexUtils;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MessageAction extends Action {

    public MessageAction() {
        super("message");
    }

    @Override
    public void execute(@NotNull Player player, @NotNull StringPlaceholders placeholders) {
        if (this.getMessage().length() == 0)
            return;

        player.sendMessage(HexUtils.colorify(PlaceholderAPI.setPlaceholders(player, placeholders.apply(this.getMessage()))));
    }
}
