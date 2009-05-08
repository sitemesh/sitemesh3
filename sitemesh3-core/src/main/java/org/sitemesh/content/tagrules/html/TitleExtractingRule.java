package org.sitemesh.content.tagrules.html;

import org.sitemesh.content.ContentProperty;
import org.sitemesh.tagprocessor.BasicBlockRule;
import org.sitemesh.tagprocessor.Tag;

import java.io.IOException;

/**
 * Extracts the contents of the <code>&lt;title&gt;</code> element from the
 * page and exports it as the <code>title</code> property.
 *
 * @author Joe Walnes
 */
public class TitleExtractingRule extends BasicBlockRule {

    private final ContentProperty propertyToExport;

    private boolean seenAtLeastOneTitle;

    public TitleExtractingRule(ContentProperty propertyToExport) {
        this.propertyToExport = propertyToExport;
    }

    @Override
    protected Object processStart(Tag tag) throws IOException {
        // Push 2 buffers...

        // Outer buffer will contain complete tag and contents (e.g. <title>Hello</title>).
        // Note: that we use a data only buffer, so the <title> does not get written to the
        // current extracted property (i.e. <head>). See Content.createDataOnlyBuffer().
        tagProcessorContext.pushBuffer(propertyToExport.getOwningContent().createDataOnlyBuffer());
        tag.writeTo(tagProcessorContext.currentBuffer()); // Opening tag.

        // Inner buffer will contain just the title value (e.g. Hello).
        tagProcessorContext.pushBuffer();

        return null;
    }

    @Override
    protected void processEnd(Tag tag, Object data) throws IOException {
        // Inner buffer. Contains just the title value (e.g. Hello).
        CharSequence justTitle = tagProcessorContext.currentBufferContents();
        tagProcessorContext.popBuffer();

        // Outer buffer. Already contiains opening tag.
        tagProcessorContext.currentBuffer().append(justTitle); // write title contents.
        tag.writeTo(tagProcessorContext.currentBuffer()); // write closing tag.
        CharSequence completeTitleElement = tagProcessorContext.currentBufferContents();
        tagProcessorContext.popBuffer();

        // Write complete title element to main buffer (see comment about data only
        // buffer above).
        tagProcessorContext.currentBuffer().append(completeTitleElement);

        // Export title.
        if (!seenAtLeastOneTitle) {
            propertyToExport.setValue(justTitle);
            seenAtLeastOneTitle = true;
        }
    }

}
