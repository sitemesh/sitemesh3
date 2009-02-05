package com.opensymphony.sitemesh.html;

import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.tagprocessor.util.CharArray;

import java.io.IOException;

/**
 * Implementation of {@link com.opensymphony.sitemesh.Content.Property} that holds its
 * data in an underlying {@link com.opensymphony.sitemesh.tagprocessor.util.CharArray}.
 *
 * @author Joe Walnes
 */
public class CharArrayProperty implements Content.Property {

    private final CharArray charArray;

    public CharArrayProperty(CharArray charArray) {
        this.charArray = charArray;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public int length() {
        return charArray.length();
    }

    @Override
    public String value() {
        return charArray.toString();
    }

    @Override
    public String valueNeverNull() {
        return value();
    }

    @Override
    public void writeTo(Appendable out) throws IOException {
        out.append(value()); // TODO
    }

    @Override
    public String toString() {
        return value();
    }
}
