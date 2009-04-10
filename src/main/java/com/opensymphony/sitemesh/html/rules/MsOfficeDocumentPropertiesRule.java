package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BasicRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;
import com.opensymphony.sitemesh.Content;

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
public class MsOfficeDocumentPropertiesRule extends BasicRule {

    private final Content content;

    public MsOfficeDocumentPropertiesRule(Content content) {
        this.content = content;
    }

    @Override
    public void process(Tag tag) throws IOException {
        if (tag.getType() == Tag.Type.OPEN) {
            tag.writeTo(context.currentBuffer());
            context.pushBuffer();
        } else if (tag.getType() == Tag.Type.CLOSE) {
            String name = tag.getName().substring(2);
            content.addProperty("office.DocumentProperties." + name, context.currentBufferContents());
            CharSequence contents = context.currentBufferContents();
            context.popBuffer();
            context.currentBuffer().append(contents);
            tag.writeTo(context.currentBuffer());
        }
    }

}
