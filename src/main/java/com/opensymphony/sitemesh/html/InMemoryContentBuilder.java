package com.opensymphony.sitemesh.html;

import com.opensymphony.sitemesh.html.rules.PageBuilder;
import com.opensymphony.sitemesh.InMemoryContent;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.tagprocessor.util.CharSequenceList;

import java.io.IOException;

/**
 * {@link PageBuilder} implementation (used by the tag rules) that writes to a SiteMesh
 * {@link InMemoryContent} instance.
 *
 * @author Joe Walnes
 */
public class InMemoryContentBuilder implements PageBuilder {

    private final InMemoryContent content;

    public InMemoryContentBuilder(InMemoryContent content) {
        this.content = content;
    }

    @Override
    public void addProperty(String key, CharSequence value) {
        if (value instanceof CharSequenceList) {
            // Optimization.
            content.addProperty(key, new CharSequenceListProperty((CharSequenceList) value));
        } else {
            content.addProperty(key, value);
        }
    }

    /**
     * Implementation of {@link Content.Property} that is optimized for
     * {@link CharSequenceList}.
     */
    private static class CharSequenceListProperty implements Content.Property {
        private final CharSequenceList value;

        public CharSequenceListProperty(CharSequenceList value) {
            this.value = value;
        }

        @Override
        public boolean exists() {
            return true;
        }

        @Override
        public String value() {
            return value.toString();
        }

        @Override
        public String valueNeverNull() {
            return value.toString();
        }

        @Override
        public void writeTo(Appendable out) throws IOException {
            value.writeTo(out);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

}
