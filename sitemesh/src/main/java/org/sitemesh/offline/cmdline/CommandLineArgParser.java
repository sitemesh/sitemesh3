package org.sitemesh.offline.cmdline;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses command line arguments into a map of key/values.
 *
 * <p>Acceptable formats:</p>
 * <ul>
 * <li><code>--key value</code> <i>(additionally single dashes are allowed)</i></li>
 * <li><code>--key=value</code></li>
 * <li><code>--key "value with spaces"</code></li>
 * <li><code>"--key=value with spaces"</code></li>
 * <li><code>--key="value with spaces"</code></li>
 * <li><code>--key</code> <i>(value will be <code>""</code>)</i></li>
 * </ul>
 *
 * @author Joe Walnes
 */
public class CommandLineArgParser {

    /**
     * @see CommandLineArgParser
     */
    public Map<String,String> parseCommandLine(String... args) {
        Map<String,String> result = new HashMap<String, String>();
        String currentKey = null;
        for (String arg : args) {
            if (arg.startsWith("--")) {
                reset(result, currentKey, "");
                currentKey = arg.substring(2);
                currentKey = split(result, currentKey);
            } else if (arg.startsWith("-")) {
                reset(result, currentKey, "");
                currentKey = arg.substring(1);
                currentKey = split(result, currentKey);
            } else {
                reset(result, currentKey, arg);
                currentKey = null;
            }
        }
        reset(result, currentKey, "");
        return result;
    }

    private void reset(Map<String, String> result, String currentKey, String value) {
        if (currentKey != null) {
            result.put(currentKey, value);
        }
    }

    private String split(Map<String, String> result, String currentKey) {
        String[] split = currentKey.split("=", 2);
        if (split.length == 2) {
            result.put(split[0], split[1]);
            currentKey = null;
        }
        return currentKey;
    }

}
