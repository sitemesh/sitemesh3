package com.opensymphony.sitemesh.html;

import com.opensymphony.sitemesh.ContentProcessor;
import com.opensymphony.sitemesh.Context;
import com.opensymphony.sitemesh.Content;
import com.opensymphony.sitemesh.InMemoryContent;
import com.opensymphony.sitemesh.html.rules.HeadExtractingRule;
import com.opensymphony.sitemesh.html.rules.BodyTagRule;
import com.opensymphony.sitemesh.html.rules.TitleExtractingRule;
import com.opensymphony.sitemesh.html.rules.FramesetRule;
import com.opensymphony.sitemesh.html.rules.PageBuilder;
import com.opensymphony.sitemesh.html.rules.HtmlAttributesRule;
import com.opensymphony.sitemesh.html.rules.MetaTagRule;
import com.opensymphony.sitemesh.html.rules.ParameterExtractingRule;
import com.opensymphony.sitemesh.html.rules.ContentBlockExtractingRule;
import com.opensymphony.sitemesh.html.rules.MSOfficeDocumentPropertiesRule;
import com.opensymphony.sitemesh.tagprocessor.TagProcessor;
import com.opensymphony.sitemesh.tagprocessor.State;
import com.opensymphony.sitemesh.tagprocessor.StateTransitionRule;

import java.nio.CharBuffer;
import java.io.IOException;

/**
 * {@link Content} implementation that will build itself from
 * an HTML page, built on {@link TagProcessor}.
 * <p/>
 * Users can override {@link #addUserDefinedRules(State, State, PageBuilder)} to customize
 * the tag processing rules.
 *
 * @author Joe Walnes
 */
public class HtmlContentProcessor<C extends Context> implements ContentProcessor<C> {

    @Override
    public Content build(CharBuffer data, C context) throws IOException {
        InMemoryContent content = new InMemoryContent(data);
        PageBuilder builder = new InMemoryContentBuilder(content);

        TagProcessor processor = new TagProcessor(data);
        State html = processor.defaultState();

        // Core rules for SiteMesh to be functional.
        html.addRule(new HeadExtractingRule(builder)); // contents of <head>
        html.addRule(new BodyTagRule(builder)); // contents of <body>

        html.addRule(new TitleExtractingRule(builder)); // the <title>
        html.addRule(new FramesetRule(builder)); // if the page is a frameset

        // Ensure that while in <xml> tag, none of the other rules kick in.
        // For example <xml><book><title>hello</title></book></xml> should not affect the title of the page.
        State xml = new State();
        html.addRule(new StateTransitionRule("xml", xml));

        // Additional rules - designed to be tweaked.
        addUserDefinedRules(html, xml, builder);

        // Run the processor.
        processor.process();

        // In the event that no <body> tag was captured, use the default buffer contents instead
        // (i.e. the whole document, except anything that was written to other buffers).
        if (!content.getProperty("body").exists()) {
          content.addProperty("body", processor.getDefaultBufferContents());
        }

        return content;
    }

    protected void addUserDefinedRules(State html, State xml, PageBuilder page) {
        // Useful properties
        html.addRule(new HtmlAttributesRule(page));         // attributes in <html> element
        html.addRule(new MetaTagRule(page));                // all <meta> tags
        html.addRule(new ParameterExtractingRule(page));    // <parameter> blocks
        html.addRule(new ContentBlockExtractingRule(page)); // <content> blocks

        // Capture properties written to documents by MS Office (author, version, company, etc).
        // Note: These properties are from the xml state, not the html state.
        xml.addRule(new MSOfficeDocumentPropertiesRule(page));
    }

}
