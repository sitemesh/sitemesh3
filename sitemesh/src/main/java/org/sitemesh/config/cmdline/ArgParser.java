/*
 *    Copyright 2009-2023 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.sitemesh.config.cmdline;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Simple command line argument parser that extracts a list of key/value properties and remaining arguments.
 *
 * <h2>Examples:</h2>
 * <ul>
 *  <li><code>"-firstname", "Joe", "-lastname", "Walnes", "a", "b", "c"</code></li>
 *  <li><code>"--firstname", "Joe", "--lastname", "Walnes", "a", "b", "c"</code></li>
 *  <li><code>"--firstname=Joe", "--lastname=Walnes", "a", "b", "c"</code></li>
 *  <li><code>"--firstname=Joe", "--lastname"</code> (illegal: No value for lastname)</li>
 *  <li><code>"--firstname=Joe", "a", "b", "--lastname", "Walnes"</code> (illegal: Named parameters have to appear first in list)</li>
 * </ul>
 *
 * @author Joe Walnes
 */
public class ArgParser {

    private final Map<String, String> properties = new HashMap<String, String>();
    private final List<String> remaining = new ArrayList<String>();

    public ArgParser(String... args) throws IllegalArgumentException {
        String currentKey = null;
        for (String arg : args) {
            if (currentKey != null) {
                properties.put(currentKey, arg);
                currentKey = null;
            } else if (arg.startsWith("-")) {
                if (!remaining.isEmpty()) {
                    throw new IllegalArgumentException("Parameters have to appear first.");
                }
                String token = arg.substring(arg.startsWith("--") ? 2 : 1);
                int equals = token.indexOf('=');
                if (equals > -1) {
                    properties.put(token.substring(0, equals), token.substring(equals + 1));
                } else {
                    currentKey = token;
                }
            } else {
                remaining.add(arg);
            }
        }
        if (currentKey != null) {
            throw new IllegalArgumentException("Parameter '" + currentKey + "' does not have value associated.");
        }
    }

    public Map<String, String> getProperties() {
        return new HashMap<String, String>(properties);
    }

    public List<String> getRemaining() {
        return new ArrayList<String>(remaining);
    }
}
