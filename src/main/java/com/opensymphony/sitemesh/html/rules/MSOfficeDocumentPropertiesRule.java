package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BasicRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;

import java.io.IOException;

/**
 * Extracts the extra properties saved in HTML from MS Office applications (Word and Excel),
 * such as Author, Company, Version, etc.
 *
 * <p>These are exported under the <code>office.DocumentProperties.</code> property prefix.
 * (e.g. <code>office.DocumentProperties.Author</code>).
 *
 * @author Joe Walnes
 */
public class MSOfficeDocumentPropertiesRule extends BasicRule {

    private final PageBuilder page;
    private boolean inDocumentProperties;

    public MSOfficeDocumentPropertiesRule(PageBuilder page) {
        this.page = page;
    }

    @Override
    public boolean shouldProcess(String name) {
        return (inDocumentProperties && name.startsWith("o:")) || name.equals("o:documentproperties");
    }

    @Override
    public void process(Tag tag) throws IOException {
        if (tag.getName().equals("o:DocumentProperties")) {
            inDocumentProperties = (tag.getType() == Tag.Type.OPEN);
            tag.writeTo(context.currentBuffer());
        } else {
            if (tag.getType() == Tag.Type.OPEN) {
                tag.writeTo(context.currentBuffer());
                context.pushBuffer();
            } else if (tag.getType() == Tag.Type.CLOSE) {
                String name = tag.getName().substring(2);
                page.addProperty("office.DocumentProperties." + name, context.currentBufferContents());
                CharSequence contents = context.currentBufferContents();
                context.popBuffer();
                context.currentBuffer().append(contents);
                tag.writeTo(context.currentBuffer());
            }
        }
    }

}
