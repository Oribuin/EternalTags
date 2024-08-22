package xyz.oribuin.eternaltags.action;

import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.entity.Player;

public abstract class Action {

    private final String name;
    private String message = "";

    protected Action(String name) {
        this.name = name;
    }

    /**
     * Execute the action function
     *
     * @param player       The player
     * @param placeholders Message placeholders
     */
    public abstract void execute(Player player, StringPlaceholders placeholders);

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}