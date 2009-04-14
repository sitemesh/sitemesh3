package com.opensymphony.sitemesh3.html;

import com.opensymphony.sitemesh3.SiteMeshContext;
import com.opensymphony.sitemesh3.content.ContentProperty;
import com.opensymphony.sitemesh3.html.rules.ContentBlockExtractingRule;
import com.opensymphony.sitemesh3.html.rules.FramesetRule;
import com.opensymphony.sitemesh3.html.rules.HtmlAttributesRule;
import com.opensymphony.sitemesh3.html.rules.ParameterExtractingRule;
import com.opensymphony.sitemesh3.tagprocessor.State;

/**
 * Extension to {@link HtmlContentProcessor} that adds additional properties - these are the same
 * properties extraced by SiteMesh 2 and earlier.
 *
 * <p>In addition to the properties extracted by {@link HtmlContentProcessor}, this adds:</p>
 * <ul>
 * <li><b><code>frameset</code></b>: Will have the value <code>true</code> if any <code>&lt;frame&gt;</code> or
 * <code>&lt;frameset&gt;</code> (but not <code>&lt;iframe&gt;</code>) tags are encountered on the page.</li>
 * <li><b><code>XXX</code></b>: Each attribute of the <code>&lt;html&gt;</code> tag, where
 * <code>XXX</code> is the attribute name.</li>
 * <li><b><code>page.XXX</code></b>: For each element of the form <code>&lt;parameter name='XXX' value='YYY'&gt;</code>
 * or <code>&lt;content tag='XXX'&gt;YYY&lt;/content&gt;</code> on the page.</li>
 * </ul>
 *
 * @see HtmlContentProcessor
 * @author Joe Walnes
 */
public class Sm2HtmlContentProcessor extends HtmlContentProcessor {

    @Override
    protected void setupRules(State htmlState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
        super.setupRules(htmlState, contentProperty, siteMeshContext);
        htmlState.addRule("frameset", new FramesetRule(contentProperty.getChild("frameset")));          // Detect framesets.
        htmlState.addRule("html", new HtmlAttributesRule(contentProperty));                             // attributes in <html> element
        htmlState.addRule("parameter", new ParameterExtractingRule(contentProperty.getChild("page")));  // <parameter> blocks
        htmlState.addRule("content", new ContentBlockExtractingRule(contentProperty.getChild("page"))); // <content> blocks
    }

}
