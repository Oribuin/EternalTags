package xyz.oribuin.eternaltags.action;

import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.util.Arrays;

public class SoundAction extends Action {

    public SoundAction() {
        super("sound");
    }

    @Override
    public void execute(@NotNull Player player, @NotNull StringPlaceholders placeholders) {
        if (this.getMessage().length() == 0)
            return;

        String[] args = this.getMessage().split(" ");
        Sound sound = null;
        if (args.length >= 1) {
            sound = TagsUtils.getEnum(Sound.class, args[0]);
        }

        if (sound == null)
            return;

        float volume = 100f;
        if (args.length >= 2) {
            volume = Float.parseFloat(args[1]);
        }

        if (volume > 100f)
            volume = 100f;

        float pitch = 1f;
        if (args.length >= 3) {
            pitch = Float.parseFloat(args[2]);
        }

        player.playSound(player.getLocation(), sound, volume, pitch);
    }

}
