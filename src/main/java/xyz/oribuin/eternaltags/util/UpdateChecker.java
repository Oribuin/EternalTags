package xyz.oribuin.eternaltags.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.regex.Pattern;

public class UpdateChecker {

    public static String getLatestVersion() {
        try {
            final URL url = new URL("https://api.spiget.org/v2/resources/91842/versions/latest");
            final URLConnection connection = url.openConnection();
            final InputStream stream = connection.getInputStream();

            if (stream == null)
                return null;

            final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            final JsonObject object = new JsonParser().parse(reader).getAsJsonObject();


            return object.get("name").getAsString().substring(1);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }


    /**
     * @param latest  The latest version of the plugin from Spigot
     * @param current The currently installed version of the plugin
     * @return true if available, otherwise false
     * @author Esophose from {@link "https://github.com/Esophose/PlayerParticles/blob/master/src/main/java/dev/esophose/playerparticles/manager/PluginUpdateManager.java#L58" }
     * <p>
     * Checks if there is an update available
     */
    public static boolean isUpdateAvailable(String latest, String current) {
        // Break versions into individual numerical pieces separated by periods
        int[] latestSplit = Arrays.stream(latest.replaceAll("[^0-9.]", "").split(Pattern.quote("."))).mapToInt(Integer::parseInt).toArray();
        int[] currentSplit = Arrays.stream(current.replaceAll("[^0-9.]", "").split(Pattern.quote("."))).mapToInt(Integer::parseInt).toArray();

        // Make sure both arrays are the same length
        if (latestSplit.length > currentSplit.length) {
            currentSplit = Arrays.copyOf(currentSplit, latestSplit.length);
        } else if (currentSplit.length > latestSplit.length) {
            latestSplit = Arrays.copyOf(latestSplit, currentSplit.length);
        }

        // Compare pieces from most significant to least significant
        for (int i = 0; i < latestSplit.length; i++) {
            if (latestSplit[i] > currentSplit[i]) {
                return true;
            } else if (currentSplit[i] > latestSplit[i]) {
                break;
            }
        }

        return false;
    }
}
