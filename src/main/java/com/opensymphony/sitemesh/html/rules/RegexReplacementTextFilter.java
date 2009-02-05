package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.TextFilter;

import java.util.regex.Pattern;

/**
 * TextFilter that substitutes content using a regular expression.
 *
 * @author Joe Walnes
 */
public class RegexReplacementTextFilter implements TextFilter {

    private final Pattern regex;
    private final String replacement;

    public RegexReplacementTextFilter(String regex, String replacement) {
        this(Pattern.compile(regex), replacement);
    }

    public RegexReplacementTextFilter(Pattern regex, String replacement) {
        this.regex = regex;
        this.replacement = replacement;
    }

    @Override
    public String filter(String text) {
        return regex.matcher(text).replaceAll(replacement);
    }

}
