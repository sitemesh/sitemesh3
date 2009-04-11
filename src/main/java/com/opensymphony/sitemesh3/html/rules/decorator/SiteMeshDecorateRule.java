package com.opensymphony.sitemesh3.html.rules.decorator;

import com.opensymphony.sitemesh3.SiteMeshContext;
import com.opensymphony.sitemesh3.InMemoryContent;
import com.opensymphony.sitemesh3.Content;
import com.opensymphony.sitemesh3.tagprocessor.BasicBlockRule;
import com.opensymphony.sitemesh3.tagprocessor.Tag;

import java.io.IOException;

/**
 * Rule that applies decorators to inline blocks of content.
 *
 * <ul>
 * <li>A {@link Content} object will be created for the inline block.</li>
 * <li>The contents of the tag body will be exposed as the <code>body</code> property.</li>
 * <li>All attributes of the tag will be copied as named properties (see example below).</li>
 * <li>The <code>decorator</code> attribute will specify which decorator is used.</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>Some content {@code <sitemesh:decorate decorator='/mydecorator' title='foo' cheese='bar'>blah</sitemesh:decorate>}
 *
 * <p>This will apply the decorator named <code>/mydecorator</code>, passing in {@link com.opensymphony.sitemesh3.Content}
 * with the following properties:</p>
 * <pre>
 * body=blah
 * title=foo
 * cheese=bar
 * </pre>
 *
 * @author Joe Walnes
 */
public class SiteMeshDecorateRule extends BasicBlockRule<Content> {

    private final SiteMeshContext siteMeshContext;

    public SiteMeshDecorateRule(SiteMeshContext siteMeshContext) {
        this.siteMeshContext = siteMeshContext;
    }

    @Override
    protected Content processStart(Tag tag) throws IOException {
        tagProcessorContext.pushBuffer();

        Content content = new InMemoryContent();

        for (int i = 0, count = tag.getAttributeCount(); i < count; i++) {
            content.getProperty(tag.getAttributeName(i)).update(tag.getAttributeValue(i));
        }

        return content;
    }

    @Override
    protected void processEnd(Tag tag, Content content) throws IOException {
        CharSequence body = tagProcessorContext.currentBufferContents();
        tagProcessorContext.popBuffer();

        content.getOriginal().update(body);
        // TODO: Use a 'default' property
        content.getProperty("body").update(body);

        String decoratorName = content.getProperty("decorator").value();
        if (decoratorName == null) {
            tagProcessorContext.currentBuffer().append(body);
            return;
        }

        Content decorated = siteMeshContext.decorate(decoratorName, content);
        if (decorated != null) {
            // TODO: Use a 'default' property
            decorated.getProperty("body").writeTo(tagProcessorContext.currentBuffer());
        } else {
            tagProcessorContext.currentBuffer().append(body);
        }
    }

}
