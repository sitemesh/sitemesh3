package org.sitemesh.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The PathMapper is used to map file patterns to keys, and find an approriate
 * key for a given file path. The pattern rules are consistent with those defined
 * in the Servlet 2.3 API on the whole. Wildcard patterns are also supported, using
 * any combination of * and ?.
 *
 * <h3>Example</h3>
 *
 * <blockquote><code>
 * PathMapper pm = new PathMapper();<br>
 * <br>
 * pm.put("/", one);<br>
 * pm.put("/mydir/*", two);<br>
 * pm.put("*.xml", three);<br>
 * pm.put(""/myexactfile.html", four);<br>
 * pm.put("/*\/admin/*.??ml", five);<br>
 * <br>
 * result1 = pm.get("/mydir/myfile.xml"); // returns two;<br>
 * result2 = pm.get("/mydir/otherdir/admin/myfile.html"); // returns five;<br>
 * </code></blockquote>
 *
 * @author Joe Walnes
 * @author Mike Cannon-Brookes
 * @author Hani Suleiman
 * @author Vladimir Orany
 */
public class PathMapper<T> {
    
    public static final Set<String> DEFAULT_KEYS;
    
    static {
        Set<String> set = new HashSet<String>();
        set.add("/");
        set.add("*");
        set.add("**");
        set.add("/*");
        set.add("/**");
        DEFAULT_KEYS = Collections.unmodifiableSet(set);
    }

    private final Map<String, T> mappings = new HashMap<String, T>();

    /** Add a key and appropriate matching pattern. */
    public void put(String pattern, T value) {
        if (value != null) {
            mappings.put(pattern, value);
        }
    }

    /** Retrieve appropriate key by matching patterns with supplied path. */
    public T get(String path) {
        if (path == null) path = "/";
        String result = findExactKey(path);
        if (result == null) result = findComplexKey(path);
        if (result == null) result = findDefaultKey();
        String mapped = result;
        if (mapped == null) return null;
        return mappings.get(mapped);
    }
    
    /** Retrieve appropriate pattern by matching patterns with supplied path. */
    public String getPatternInUse(String path) {
        if (path == null) path = "/";
        String result = findExactKey(path);
        if (result == null) result = findComplexKey(path);
        if (result == null) result = findDefaultKey();
        return result;
    }

    /** Check if path matches exact pattern ( /blah/blah.jsp ). */
    private String findExactKey(String path) {
        if (mappings.containsKey(path)) return path;
        return null;
    }

    private String findComplexKey(String path) {
        String result = null;

        for (String key : mappings.keySet()) {
            if (isComplexKey(key) && match(key, path, false)) {
                if (result == null || key.length() > result.length()) {
                    // longest key wins
                    result = key;
                }
            }
        }
        return result;
    }

    /**
     * Return <code>true</code> if the key contains wild cards.
     * @param key key under test
     * @return <code>true</code> if the key contains wild cards
     */
    public static boolean isComplexKey(String key) {
        return key.length() > 1 && (key.indexOf('?') != -1 || key.indexOf('*') != -1);
    }
    
    /**
     * Return <code>true</code> if the key default key matching all paths.
     * @param key key under test
     * @return <code>true</code> if the key default key matching all paths
     */
    public static boolean isDefaultKey(String key) {
        return DEFAULT_KEYS.contains(key);
    }

    /** Look for root pattern ( / ). */
    private String findDefaultKey() {
        for (String defaultKey : DEFAULT_KEYS) {
            if (mappings.containsKey(defaultKey)) return defaultKey;
        }
        return null;
    }

    private static boolean match(String pattern, String str, boolean isCaseSensitive) {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        char ch;

        boolean containsStar = false;
        for (int i = 0; i < patArr.length; i++) {
            if (patArr[i] == '*') {
                containsStar = true;
                break;
            }
        }

        if (!containsStar) {
            // No '*'s, so we make a shortcut
            if (patIdxEnd != strIdxEnd) {
                return false; // Pattern and string do not have the same size
            }
            for (int i = 0; i <= patIdxEnd; i++) {
                ch = patArr[i];
                if (ch != '?') {
                    if (isCaseSensitive && ch != strArr[i]) {
                        return false; // Character mismatch
                    }
                    if (!isCaseSensitive && Character.toUpperCase(ch) !=
                            Character.toUpperCase(strArr[i])) {
                        return false; // Character mismatch
                    }
                }
            }
            return true; // String matches against pattern
        }

        if (patIdxEnd == 0) {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?') {
                if (isCaseSensitive && ch != strArr[strIdxStart]) {
                    return false; // Character mismatch
                }
                if (!isCaseSensitive && Character.toUpperCase(ch) !=
                        Character.toUpperCase(strArr[strIdxStart])) {
                    return false; // Character mismatch
                }
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // Process characters after last star
        while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?') {
                if (isCaseSensitive && ch != strArr[strIdxEnd]) {
                    return false; // Character mismatch
                }
                if (!isCaseSensitive && Character.toUpperCase(ch) !=
                        Character.toUpperCase(strArr[strIdxEnd])) {
                    return false; // Character mismatch
                }
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patArr[i] == '*') {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    ch = patArr[patIdxStart + j + 1];
                    if (ch != '?') {
                        if (isCaseSensitive && ch != strArr[strIdxStart + i + j]) {
                            continue strLoop;
                        }
                        if (!isCaseSensitive && Character.toUpperCase(ch) !=
                                Character.toUpperCase(strArr[strIdxStart + i + j])) {
                            continue strLoop;
                        }
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (patArr[i] != '*') {
                return false;
            }
        }
        return true;
    }

    /**
     * Wheter the first pattern is more specific than the second one.
     * Exact patterns are more specific than complex patterns and 
     * complex patterns are more specific than default patterns.
     * @param exclusionPattern pattern which must be more or equally specific to return <code>true</code>
     * @param decoratorPattern pattern which must be less specific to return <code>true</code>
     * @return <code>true</code> if the first pattern is more specific than the second one
     */
    public static boolean isMoreSpecific(String exclusionPattern, String decoratorPattern) {
        if(isDefaultKey(decoratorPattern)) {
            return true;
        }
        if(isComplexKey(decoratorPattern)){
            if(isComplexKey(exclusionPattern)) {
                return exclusionPattern.length() >= decoratorPattern.length();
            }
            return !isDefaultKey(decoratorPattern);
        }
        return !isComplexKey(exclusionPattern) && !isDefaultKey(exclusionPattern);
    }
}