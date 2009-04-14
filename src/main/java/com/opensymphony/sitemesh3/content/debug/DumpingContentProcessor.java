package com.opensymphony.sitemesh3.content.debug;

import com.opensymphony.sitemesh3.content.ContentProcessor;
import com.opensymphony.sitemesh3.content.ContentProperty;
import com.opensymphony.sitemesh3.SiteMeshContext;

import java.nio.CharBuffer;
import java.io.IOException;

/**
 * Decorates a ContentProcessor and will dump the contents of each Content that is created to
 * an output stream. Useful for debugging.
 *
 * @author Joe Walnes
 */
public class DumpingContentProcessor implements ContentProcessor {

    private final ContentProcessor contentProcessor;
    private final Appendable debugOut;

    public DumpingContentProcessor(ContentProcessor contentProcessor, Appendable debugOut) {
        this.contentProcessor = contentProcessor;
        this.debugOut = debugOut;
    }

    @Override
    public ContentProperty build(CharBuffer data, SiteMeshContext context) throws IOException {
        ContentProperty result = contentProcessor.build(data, context);
        dump(result, debugOut);
        return result;
    }

    public static void dump(ContentProperty contentProperty, Appendable out) throws IOException {
        for (ContentProperty descendant : contentProperty.getDescendants()) {
            out.append("~~~~~~ " + getFullPath(descendant) + " ~~~~~~");
            out.append("\n[[ORIGINAL]]\n");
            descendant.getOriginal().writeValueTo(out);
            out.append("\n[[PROCESSED]]\n");
            descendant.writeValueTo(out);
            out.append("\n\n");
        }
    }

    public static String dump(ContentProperty contentProperty) {
        StringBuilder result = new StringBuilder();
        try {
            dump(contentProperty, result);
        } catch (IOException e) {
            return "Exception: " + e.toString();
        }
        return result.toString();
    }

    public static String getFullPath(ContentProperty contentProperty) {
        StringBuilder result = new StringBuilder();
        for (ContentProperty item : contentProperty.getFullPath()) {
            if (result.length() > 0) {
                result.append('.');
            }
            result.append(item.getName());
        }
        return result.toString();
    }

}
