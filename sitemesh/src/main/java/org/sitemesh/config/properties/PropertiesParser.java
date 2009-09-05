package org.sitemesh.config.properties;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Parses and cleans up string based properties.
 *
 * @author Joe Walnes
 */
class PropertiesParser {

    private final Map<String, String> properties;

    PropertiesParser(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * Return string value, trimming whitespace.
     *
     * Will return null if property is not found, empty or whitespace only.
     */
    String getString(String... keysToSearch) {
        for (String key : keysToSearch) {
            String string = properties.get(key);
            if (string != null) {
                string = string.trim();
                if (string.length() > 0) {
                    return string;
                }
            }
        }
        return null;
    }

    /**
     * Return string array, splitting on whitespace or commas, and trimming whitespace.
     * e.g. "a,b,c" equivalent to "a b c" or "a\nb\nc" or "a    b,\nc  "
     *
     * Will return empty array if property is not found or empty.
     */
    String[] getStringArray(String key) {
        String string = getString(key);
        if (string == null) {
            return new String[0];
        }
        String[] result = string.split("[,\\s]+");
        for (int i = 0; i < result.length; i++) {
            result[i] = result[i].trim();
        }
        return result;
    }

    /**
     * Return Map, splitting entries on whitespace or commas, and trimming whitespace.
     * Entries must consist of key=value (with no whitespace around the = char).
     * Multiple values can be specified using a pipe | delimiter (no whitespace around
     * it).
     * e.g. "a=Apples, b=Bananas|Beef, c=Cherries" or "a=Apples\nb=Bananas\nc=Cherries".
     * The map will retain the order that the entries were defined in.
     *
     * Will return empty map if property is not found or empty.
     */
    Map<String, String[]> getStringMultiMap(String key) {
        Map<String, String[]> result = new LinkedHashMap<String, String[]>();
        String[] entries = getStringArray(key);
        if (entries != null) {
            for (String entry : entries) {
                String[] split = entry.split("=", 2);
                if (split.length == 2) {
                    String itemKey = split[0];
                    String[] itemValue = split[1].split("\\|");
                    result.put(itemKey, itemValue);
                }
            }
        }
        return result;
    }
}
