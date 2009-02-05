package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.BasicRule;
import com.opensymphony.sitemesh.tagprocessor.Tag;
import com.opensymphony.sitemesh.tagprocessor.util.CharArray;

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
public class BodyTagRule extends BasicRule {

    private final PageBuilder page;
    private final CharArray body;

    public BodyTagRule(PageBuilder page, CharArray body) {
        super("body");
        this.page = page;
        this.body = body;
    }

    @Override
    public void process(Tag tag) {
        if (tag.getType() == Tag.Type.OPEN || tag.getType() == Tag.Type.EMPTY) {
            for (int i = 0; i < tag.getAttributeCount(); i++) {
                page.addProperty("body." + tag.getAttributeName(i), tag.getAttributeValue(i));
            }
            body.clear();
        } else {
             // unused buffer: everything after </body> is discarded.
            CharArray unused = new CharArray(64); // TODO: Don't actually buffer the remaining content.
            context.pushBuffer(unused);
        }
    }

}
