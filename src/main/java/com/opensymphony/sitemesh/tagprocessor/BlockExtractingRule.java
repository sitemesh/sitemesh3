package com.opensymphony.sitemesh.tagprocessor;

import com.opensymphony.sitemesh.tagprocessor.util.CharArray;

/**
 * This is the base class for a {@link TagRule} that will extract the contents
 * of a block (pair of opening and closing tags, otherwise known as an element)
 * from the page. The extracted block can then be written back to the page,
 * discarded, or modified.
 *
 * @author Joe Walnes
 */
public abstract class BlockExtractingRule extends BasicRule {

    private boolean includeEnclosingTags;

    // we should only handle tags that have been opened previously.
    // else the parser throws a NoSuchElementException (SIM-216)
    private boolean seenOpeningTag;

    protected BlockExtractingRule(boolean includeEnclosingTags, String acceptableTagName) {
        super(acceptableTagName);
        this.includeEnclosingTags = includeEnclosingTags;
    }

    protected BlockExtractingRule(boolean includeEnclosingTags) {
        this.includeEnclosingTags = includeEnclosingTags;
    }

    @Override
    public void process(Tag tag) {
        if (tag.getType() == Tag.Type.OPEN) {
            if (includeEnclosingTags) {
                tag.writeTo(context.currentBuffer());
            }
            context.pushBuffer(createBuffer());
            start(tag);
            seenOpeningTag = true;
        } else if (tag.getType() == Tag.Type.CLOSE && seenOpeningTag) {
            end(tag);
            context.popBuffer();
            if (includeEnclosingTags) {
                tag.writeTo(context.currentBuffer());
            }
        }
    }

    protected void start(Tag tag) {
    }

    protected void end(Tag tag) {
    }

    protected CharArray createBuffer() {
        return new CharArray(512);
    }

}
