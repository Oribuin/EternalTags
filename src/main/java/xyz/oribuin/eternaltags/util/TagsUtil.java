package xyz.oribuin.eternaltags.util;

import java.util.Arrays;
import java.util.List;

public final class TagsUtil {

    /**
     * Check if the server is using paper
     *
     * @return If paper is found.
     */
    public static boolean isUsingPaper() {
        try {
            Class.forName("com.destroystokyo.paper.util.VersionFetcher");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * Format a List<String> into a single String
     *
     * @param list The list
     * @return the new formatted string.
     */
    public static String formatList(List<String> list) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            builder.append(list.get(i));
            if (i != list.size() - 1)
                builder.append(", ");
        }

        return builder.toString();
    }

    /**
     * Format a List<String> into a single String
     *
     * @param list The list
     * @return the new formatted string.
     */
    public static String formatList(String[] list) {
        return formatList(Arrays.asList(list));
    }

}
