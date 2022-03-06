package xyz.oribuin.eternaltags.locale;

import dev.rosewood.rosegarden.locale.Locale;

import java.util.LinkedHashMap;
import java.util.Map;

public class EnglishLocale implements Locale {

    @Override
    public String getLocaleName() {
        return "en_US";
    }

    @Override
    public String getTranslatorName() {
        return "Oribuin";
    }

    @Override
    public Map<String, Object> getDefaultLocaleValues() {
        return new LinkedHashMap<String, Object>() {{
            this.put("#0", "Plugin Message Prefix");
            this.put("prefix", "&7[<g:#00B4DB:#0083B0>&lEternalTags &8| &f");

            this.put("#1", "Generic Command Messages");
            this.put("no-permission", "You don't have permission for that.");
            this.put("only-player", "This command can only be executed by a player.");
            this.put("only-console", "This command can only be executed by console.");
            this.put("unknown-command", "Unknown command, use &b/#99ff99%cmd%&f help for more info");

            this.put("#2", "Clear Command");
            this.put("command-clear-description", "Clears your active tag.");
            this.put("command-clear-cleared", "You have cleared your current active tag.");
            this.put("command-clear-cleared-other", "You have cleared %player%'s active tag.");

            this.put("#3", "Convert Command");
            this.put("command-convert-description", "Converts tags into EternalTags.");
            this.put("command-convert-converted", "You have converted %total% tags into EternalTags");
            this.put("command-convert-invalid-plugin", "Please provide a valid plugin to convert from. [%options%]");

            this.put("#4", "Create Command");
            this.put("command-create-description", "Creates a new tag ingame.");
            this.put("command-create-created", "You have successfully created a new tag, [%tag%&f]!");
            this.put("command-create-tag-exists", "This tag already exists.");

            this.put("#5", "Delete Command");
            this.put("command-delete-description", "Deletes a tag from the config.");
            this.put("command-delete-deleted", "You have successfully deleted the tag, [%tag%]&f!");

            this.put("#6", "Favorite Command");
            this.put("command-favorite-description", "Toggles a tag as a favourite.");
            this.put("command-favorite-toggled", "You have toggled the tag %tag% &f%toggled%!");
            this.put("command-favorite-on", "on");
            this.put("command-favorite-off", "off");

            this.put("#7", "Random Command");
            this.put("command-random-description", "Automatically equips a random tag you own.");

            this.put("#8", "Reload Command");
            this.put("command-reload-description", "Reloads the plugin.");
            this.put("command-reload-reloaded", "Configuration and locale files were reloaded");

            this.put("#9", "Search Command");
            this.put("command-search-description", "Search for a tag that you own.");

            this.put("#10", "Set Command");
            this.put("command-set-description", "Change your current active tag.");
            this.put("command-set-changed", "Your active tag has been changed to %tag%&f!");
            this.put("command-set-changed-other", "You have changed %player%'s active tag to %tag%&f!");

            this.put("#11", "Set All Command");
            this.put("command-setall-description", "Change all player's active tag.");
            this.put("command-setall-changed", "You have changed everyone's tag to %tag%");

            this.put("#12", "Help Command");
            this.put("command-help-description", "Displays the help menu.");

            this.put("#13", "Plugin Error Messages");
            this.put("no-tags", "You currently do not own any tags.");
            this.put("tag-doesnt-exist", "The tag you have provided doesn't exist.");

            this.put("#14", "Argument Handler Error Messages");
            this.put("argument-handler-player", "Please provide an online username.");
            this.put("argument-handler-plugins", "Please provide a valid input for valid plugins.");
            this.put("argument-handler-tags", "Please provide a valid input for valid plugins.");
        }};
    }
}
