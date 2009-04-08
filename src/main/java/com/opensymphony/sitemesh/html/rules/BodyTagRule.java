package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BasicBlockRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;
import com.opensymphony.sitemesh.Content;

import java.io.IOException;

/**
 * Extracts the contents of the &lt;body&gt; tag, writing into the passed in buffer.
 *
 * <p>Additionally, any attributes on the &lt;body&gt; tag (e.g. onclick) will be exported
 * to the page as properties under the 'body.' prefix (e.g. body.onclick).</p>
 *
 * <p>This rule also deals with documents that do not contain any &lt;body&gt; tags,
 * treating the entire document as the body instead.</p>
 *
 * @author Joe Walnes
 */
public class BodyTagRule extends BasicBlockRule {

    private final Content content;

    public BodyTagRule(Content content) {
        super("body");
        this.content = content;
    }

    @Override
    protected Object processStart(Tag tag) throws IOException {
        tag.writeTo(context.currentBuffer());
        for (int i = 0; i < tag.getAttributeCount(); i++) {
            content.addProperty("body." + tag.getAttributeName(i), tag.getAttributeValue(i));
        }
        context.pushBuffer();
        return null;
    }

    @Override
    protected void processEnd(Tag tag, Object data) throws IOException {
        CharSequence body = context.currentBufferContents();
        content.addProperty("body", body);
        context.popBuffer();
        context.currentBuffer().append(body);
        tag.writeTo(context.currentBuffer());
    }

}
