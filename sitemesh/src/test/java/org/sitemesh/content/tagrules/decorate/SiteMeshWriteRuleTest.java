package org.sitemesh.content.tagrules.decorate;

import org.sitemesh.SiteMeshContext;
import org.sitemesh.SiteMeshContextStub;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.content.memory.InMemoryContent;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.TagRuleBundle;
import org.sitemesh.content.tagrules.html.ExportTagToContentRule;
import org.sitemesh.tagprocessor.State;
import org.sitemesh.tagprocessor.TagProcessor;
import junit.framework.TestCase;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * @author Joe Walnes
 */
public class SiteMeshWriteRuleTest extends TestCase {

    public void testWritesTheProperty() throws IOException {
        Content content = new InMemoryContent();
        content.getExtractedProperties().getChild("foo").setValue("This is the <foo> property.");
        content.getExtractedProperties().getChild("bar").getChild("x").setValue("BAR");
        SiteMeshContextStub context = new SiteMeshContextStub();
        context.setContentToMerge(content);

        String in = "Hello <sitemesh:write property='foo'/> <sitemesh:write property='bar.x'/>!";
        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("sitemesh:write", new SiteMeshWriteRule(context));
        tagProcessor.process();
        CharSequence out = tagProcessor.getDefaultBufferContents();

        assertEquals("Hello This is the <foo> property. BAR!", out.toString());
    }

    public void testRemovesTagBodyIfContentSupplied() throws IOException {
        Content content = new InMemoryContent();
        SiteMeshContextStub context = new SiteMeshContextStub();
        context.setContentToMerge(content);

        String in = "Hello <sitemesh:write property='notfound'>X</sitemesh:write>" +
                " <sitemesh:write property='found.not'>X</sitemesh:write>!";
        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("sitemesh:write", new SiteMeshWriteRule(context));
        tagProcessor.process();
        CharSequence out = tagProcessor.getDefaultBufferContents();

        assertEquals("Hello  !", out.toString());
    }

    public void testLeavesTagBodyIfContentMissing() throws IOException {
        SiteMeshContextStub context = new SiteMeshContextStub();
        context.setContentToMerge(null); // No content

        String in = "Hello <sitemesh:write property='notfound'>X</sitemesh:write>" +
                " <sitemesh:write property='found.not'>X</sitemesh:write>!";
        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("sitemesh:write", new SiteMeshWriteRule(context));
        tagProcessor.process();
        CharSequence out = tagProcessor.getDefaultBufferContents();

        assertEquals("Hello X X!", out.toString());
    }

    public void testSkipsMissingProperties() throws IOException {
        Content content = new InMemoryContent();
        SiteMeshContextStub context = new SiteMeshContextStub();
        content.getExtractedProperties().getChild("found").setValue("FOUND");
        context.setContentToMerge(content);

        String in = "Hello <sitemesh:write property='found'>BAD</sitemesh:write>" +
                " <sitemesh:write property='notfound'>BAD</sitemesh:write>!";
        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("sitemesh:write", new SiteMeshWriteRule(context));
        tagProcessor.process();
        CharSequence out = tagProcessor.getDefaultBufferContents();

        assertEquals("Hello FOUND !", out.toString());
    }

    public void testWritesBodyAttributesSimple() throws IOException {
        Content content = new InMemoryContent();
        SiteMeshContextStub context = new SiteMeshContextStub();
        content.getExtractedProperties().getChild("body").getChild("id").setValue("ID");
        context.setContentToMerge(content);

        String in = "<body id=\"<sitemesh:write property='body.id'/>\">Hello!</body>";
        TagProcessor tagProcessor = new TagProcessor(CharBuffer.wrap(in));
        tagProcessor.addRule("sitemesh:write", new SiteMeshWriteRule(context));
        tagProcessor.process();
        CharSequence out = tagProcessor.getDefaultBufferContents();

        assertEquals("<body id=\"ID\">Hello!</body>", out.toString());
    }
    
    public void testWritesBodyAttributesAdvanced() throws IOException {
        Content content = new InMemoryContent();
        SiteMeshContextStub context = new SiteMeshContextStub();
        content.getExtractedProperties().getChild("body").setValue("Hello!");
        content.getExtractedProperties().getChild("body").getChild("id").setValue("ID");
        context.setContentToMerge(content);

        String in = "<body id=\"<sitemesh:write property='body.id'/>\">Hello!</body>";
        TagBasedContentProcessor processor = new TagBasedContentProcessor(new TagRuleBundle() {
            
            @Override public void install(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
                defaultState.addRule("sitemesh:write", new SiteMeshWriteRule(siteMeshContext));
                defaultState.addRule("body", new ExportTagToContentRule(siteMeshContext, contentProperty.getChild("body"), true));
            }
            
            @Override public void cleanUp(State defaultState, ContentProperty contentProperty, SiteMeshContext siteMeshContext) {
                // NO-OP
            }
        });
        
        context.setContentProcessor(processor);
        
        StringBuilder out = new StringBuilder();
        Content decorated = processor.build(CharBuffer.wrap(in), context);
        decorated.getData().writeValueTo(out);

        assertEquals("<body id=\"ID\">Hello!</body>", out.toString());
    }

}
