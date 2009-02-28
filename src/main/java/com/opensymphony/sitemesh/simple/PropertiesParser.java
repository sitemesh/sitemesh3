package com.opensymphony.sitemesh.simple;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Parses and cleans up string based properties.
 *
 * @author Joe Walnes
 */
abstract class PropertiesParser {

    /**
     * Load the actual string property. May return null or whitespace.
     */
    protected abstract String getProperty(String key);

    /**
     * Return string value, trimming whitespace and returning the default if not found or empty.
     */
    public String getString(String key, String defaultValue) {
        String string = getProperty(key);
        if (string == null) {
            return defaultValue;
        }
        string = string.trim();
        if (string.isEmpty()) {
            return defaultValue;
        }
        return string;
    }

    /**
     * Return string array, splitting on whitespace or commas, trimming whitespace and
     * returning the default if not found or empty.
     * e.g. "a,b,c" equivalent to "a b c" or "a\nb\nc" or "a    b,\nc  "
     */
    public String[] getStringArray(String key, String defaultValue) {
        String string = getString(key, defaultValue);
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
     * Return Map, splitting entries on whitespace or commas, trimming whitespace and
     * returning the default if not found or empty. Entries must consist of key=value (with
     * no whitespace around the = char.
     * e.g. "a=Apples, b=Bananas, c=Cherries" or "a=Apples\nb=Bananas\nc=Cherries".
     * The map will retain the order that the entries were defined in.
     */
    public Map<String, String> getStringMap(String key, String defaultValue) {
        Map<String, String> result = new LinkedHashMap<String, String>();
        String[] entries = getStringArray(key, defaultValue);
        for (String entry : entries) {
            String[] split = entry.split("=", 2);
            if (split.length == 2) {
                result.put(split[0], split[1]);
            }
        }
        return result;
    }
}
