package xyz.oribuin.eternaltags.action;

import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.oribuin.eternaltags.util.TagsUtils;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SoundAction extends Action {

    private final Pattern volumeRegex = Pattern.compile("volume:([0-9]+)");

    public SoundAction() {
        super("sound");
    }

    @Override
    public void execute(@NotNull Player player, @NotNull StringPlaceholders placeholders) {
        if (this.getMessage().length() == 0)
            return;

        String[] args = this.getMessage().split(" ");
        float volume = 100f;
        if (args.length <= 2) {
            Matcher volumeMatch = volumeRegex.matcher(args[1]);
            if (volumeMatch.find()) {
                volume = Float.parseFloat(volumeMatch.group(1));
            }
        }

        Sound sound = TagsUtils.getEnum(Sound.class, args[0]);
        if (sound == null) {
            return;
        }

        player.playSound(player.getLocation(),sound, volume, 1f);
    }

    private Sound getSound(String sound) {
        return Arrays.stream(Sound.values()).filter(x -> x.name().equalsIgnoreCase(sound)).findFirst().orElse(null);
    }

}
