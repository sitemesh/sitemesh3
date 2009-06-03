package org.sitemesh.content.debug;

import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.content.Content;
import org.sitemesh.SiteMeshContext;

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

    public Content build(CharBuffer data, SiteMeshContext context) throws IOException {
        Content result = contentProcessor.build(data, context);
        dump(result, debugOut);
        return result;
    }

    public static void dump(Content content, Appendable out) throws IOException {
        out.append("~~~~~~ MAIN ~~~~~~");
        content.getData().writeValueTo(out);
        for (ContentProperty descendant : content.getExtractedProperties().getDescendants()) {
            out.append("~~~~~~ " + getFullPath(descendant) + " ~~~~~~");
            descendant.writeValueTo(out);
            out.append("\n\n");
        }
    }

    public static String dump(Content content) {
        StringBuilder result = new StringBuilder();
        try {
            dump(content, result);
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
