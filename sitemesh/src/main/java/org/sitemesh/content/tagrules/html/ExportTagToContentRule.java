package org.sitemesh.content.tagrules.html;

import java.io.IOException;
import java.nio.CharBuffer;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.tagprocessor.BasicBlockRule;
import org.sitemesh.tagprocessor.CustomTag;
import org.sitemesh.tagprocessor.Tag;

/**
 * Exports the contents of a match tag to property of the passed in {@link ContentProperty}.
 *
 * Additionally, if this tag has attributes, they will be written as child properties.
 *
 * <h3>Example</h3>
 *
 * <pre>
 * // Java
 * myState.addRule("foo", new ExportTagToContentRule(content, "bar");
 *
 * // Input
 * &lt;foo x=1 b=2&gt;hello&lt/foo&gt;
 *
 * // Exported properties of Content
 * bar=hello
 * bar.x=1
 * bar.b=2
 * </pre>
 *
 * @author Joe Walnes
 */
public class ExportTagToContentRule extends BasicBlockRule {

    private final ContentProperty targetProperty;
    private final boolean includeInContent;
    private final SiteMeshContext context;
;
    /**
     * @param targetProperty   ContentProperty to export tag contents to.
     * @param includeInContent Whether the tag should be included in the content (if false, it will be stripped
     *                         from the current ContentProperty that is being written to.
     * @see ExportTagToContentRule
     */
    public ExportTagToContentRule(SiteMeshContext context, ContentProperty targetProperty, boolean includeInContent) {
        this.targetProperty = targetProperty;
        this.includeInContent = includeInContent;
        this.context = context;
    }

    @Override
    protected Object processStart(Tag tag) throws IOException {
        // Some terminology:
        // Given a tag: '<foo>hello</foo>'
        // INNER contents refers to 'hello'
        // OUTER contents refers to '<foo>hello</foo>'
        
        Tag t = tag;

        // Export all attributes of the opening tag as child nodes on the target ContentProperty.
        for (int i = 0; i < t.getAttributeCount(); i++) {
            // attributes of tags using this rule doesn't expand sitemesh:write
            // https://github.com/sitemesh/sitemesh3/issues/23
            String value = t.getAttributeValue(i);
            
            // only if there might be another tag inside the attribute
            if(value != null && (value.indexOf('<') < value.indexOf('>'))){
                StringBuilder sb = new StringBuilder();
                context.getContentProcessor().build(CharBuffer.wrap(value), context).getData().writeValueTo(sb);
                value = sb.toString();
                
                if(!(t instanceof CustomTag)){
                    t = new CustomTag(t);
                }
                  
                CustomTag custom = (CustomTag) t;
                custom.setAttributeValue(i, value);
                
            }
                
            targetProperty.getChild(t.getAttributeName(i)).setValue(value);
        }

        // Push a buffer for the OUTER contents.
        if (!includeInContent) {
            // If the tag should NOT be included in the contents, we use a data-only buffer,
            // which means that although the contents won't be written
            // back to the ContentProperty, they will be available in the main Content data.
            // See Content.createDataOnlyBuffer()
            tagProcessorContext.pushBuffer(targetProperty.getOwningContent().createDataOnlyBuffer());
        } else {
            tagProcessorContext.pushBuffer();
        }

        // Write opening tag to OUTER buffer.
        t.writeTo(tagProcessorContext.currentBuffer());

        // Push a new buffer for storing the INNER contents.
        tagProcessorContext.pushBuffer();
        return null;
    }

    @Override
    protected void processEnd(Tag tag, Object data) throws IOException {
        // Get INNER content, and pop the buffer for INNER contents.
        CharSequence innerContent = tagProcessorContext.currentBufferContents();
        tagProcessorContext.popBuffer();

        // Write the INNER content and closing tag, to OUTER buffer and pop it.
        tagProcessorContext.currentBuffer().append(innerContent);
        if (tag.getType() != Tag.Type.EMPTY) { // if the tag is empty we have already written it in processStart().
            tag.writeTo(tagProcessorContext.currentBuffer());
        }
        CharSequence outerContent = tagProcessorContext.currentBufferContents();
        tagProcessorContext.popBuffer();

        // Write the OUTER contents to the current buffer, which is now the buffer before the
        // tag was processed. Note that if !includeInContent, this buffer will not be written
        // to the ContentProperty (though it will be available in Content.getData()).
        // See comment in processStart().
        tagProcessorContext.currentBuffer().append(outerContent);

        // Export the tag's inner contents to
        if (!targetProperty.hasValue()) {
            targetProperty.setValue(innerContent);
        }
    }
}
