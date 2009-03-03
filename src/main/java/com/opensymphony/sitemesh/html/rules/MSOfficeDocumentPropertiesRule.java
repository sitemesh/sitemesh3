package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BlockExtractingRule;
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
public class MSOfficeDocumentPropertiesRule extends BlockExtractingRule {

    private final PageBuilder page;
    private boolean inDocumentProperties;

    public MSOfficeDocumentPropertiesRule(PageBuilder page) {
        super(true);
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
            tag.writeTo(currentBuffer());
        } else {
            super.process(tag);
        }
    }

    @Override
    protected void start(Tag tag) {
    }

    @Override
    protected void end(Tag tag) {
        String name = tag.getName().substring(2);
        page.addProperty("office.DocumentProperties." + name, currentBuffer().toString());
        context.mergeBuffer();
    }

}
