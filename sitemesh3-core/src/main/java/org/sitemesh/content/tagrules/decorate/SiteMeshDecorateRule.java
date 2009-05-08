package org.sitemesh.content.tagrules.decorate;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.memory.InMemoryContent;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.content.Content;
import org.sitemesh.tagprocessor.Tag;
import org.sitemesh.tagprocessor.BasicBlockRule;

import java.io.IOException;

/**
 * Rule that applies decorators to inline blocks of content.
 *
 * <ul>
 * <li>A {@link ContentProperty} object will be created for the inline block.</li>
 * <li>The contents of the tag body will be exposed as the <code>body</code> property.</li>
 * <li>All attributes of the tag will be copied as named properties (see example below).</li>
 * <li>The <code>decorator</code> attribute will specify which decorator is used.</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * <pre>Some content {@code <sitemesh:decorate decorator='/mydecorator' title='foo' cheese='bar'>blah</sitemesh:decorate>}
 *
 * <p>This will apply the decorator named <code>/mydecorator</code>, passing in {@link ContentProperty}
 * with the following properties:</p>
 * <pre>
 * body=blah
 * title=foo
 * cheese=bar
 * </pre>
 *
 * @author Joe Walnes
 */
public class SiteMeshDecorateRule extends BasicBlockRule<SiteMeshDecorateRule.Holder> {

    static class Holder {
        public final Content content = new InMemoryContent();
        public String decoratorName;
    }

    private final SiteMeshContext siteMeshContext;

    public SiteMeshDecorateRule(SiteMeshContext siteMeshContext) {
        this.siteMeshContext = siteMeshContext;
    }

    @Override
    protected Holder processStart(Tag tag) throws IOException {
        tagProcessorContext.pushBuffer();

        Holder holder = new Holder();
        for (int i = 0, count = tag.getAttributeCount(); i < count; i++) {
            String name = tag.getAttributeName(i);
            String value = tag.getAttributeValue(i);
            if (name.equals("decorator")) {
                holder.decoratorName = value;
            } else {
                holder.content.getExtractedProperties().getChild(name).setValue(value);
            }
        }

        return holder;
    }

    @Override
    protected void processEnd(Tag tag, Holder holder) throws IOException {
        CharSequence body = tagProcessorContext.currentBufferContents();
        tagProcessorContext.popBuffer();

        holder.content.getData().setValue(body);
        // TODO: Use a 'default' property
        holder.content.getExtractedProperties().getChild("body").setValue(body);

        if (holder.decoratorName == null) {
            tagProcessorContext.currentBuffer().append(body);
            return;
        }

        Content decorated = siteMeshContext.decorate(holder.decoratorName, holder.content);
        if (decorated != null) {
            // TODO: Use a 'default' property
            decorated.getExtractedProperties().getChild("body").writeValueTo(tagProcessorContext.currentBuffer());
        } else {
            tagProcessorContext.currentBuffer().append(body);
        }
    }

}
