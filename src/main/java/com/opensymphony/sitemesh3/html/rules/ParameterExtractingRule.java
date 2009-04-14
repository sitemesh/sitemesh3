package com.opensymphony.sitemesh3.html.rules;

import com.opensymphony.sitemesh3.tagprocessor.BasicRule;
import com.opensymphony.sitemesh3.tagprocessor.Tag;
import com.opensymphony.sitemesh3.ContentProperty;

/**
 * Extracts the contents of any elements that look like
 * <code>&lt;parameter name='x' value='y'&gt;</code> and write the contents
 * to a page property (<code>page.x=y</code>).
 *
 * <p>This is a cheap and cheerful mechanism for exporting values from content to decorators.</p>
 *
 * @author Joe Walnes
 */
public class ParameterExtractingRule extends BasicRule{

    private final ContentProperty propertyToExport;

    public ParameterExtractingRule(ContentProperty propertyToExport) {
        this.propertyToExport = propertyToExport;
    }

    @Override
    public void process(Tag tag) {
        propertyToExport.getChild(tag.getAttributeValue("name", false))
                .setValue(tag.getAttributeValue("value", false));
    }
}
