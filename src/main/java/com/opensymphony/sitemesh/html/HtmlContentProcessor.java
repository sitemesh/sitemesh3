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
import com.opensymphony.sitemesh.tagprocessor.util.CharArray;
import com.opensymphony.sitemesh.tagprocessor.TagProcessor;
import com.opensymphony.sitemesh.tagprocessor.State;
import com.opensymphony.sitemesh.tagprocessor.StateTransitionRule;

import java.nio.CharBuffer;
import java.io.IOException;

/**
 * {@link com.opensymphony.sitemesh.Content} implementation that will build itself from
 * an HTML page, built on {@link TagProcessor}.
 * <p/>
 * Users can override {@link #addUserDefinedRules(State, State, com.opensymphony.sitemesh.html.rules.PageBuilder)} to customize
 * the tag processing rules.
 *
 * @author Joe Walnes
 */
public class HtmlContentProcessor<C extends Context> implements ContentProcessor<C> {

    public static final String BODY = "body";
    public static final String HEAD = "head";
    public static final String TITLE = "title";

    @Override
    public Content build(CharBuffer data, C context) throws IOException {
        CharArray head = new CharArray(64);
        CharArray body = new CharArray(4096);

        final InMemoryContent content = new InMemoryContent(data);

        TagProcessor processor = new TagProcessor(data, body);
        State html = processor.defaultState();

        PageBuilder builder = new PageBuilder() {
            @Override
            public void addProperty(String key, String value) {
                content.addProperty(key, value);
            }
        };
        // Core rules for SiteMesh to be functional.
        html.addRule(new HeadExtractingRule(head)); // contents of <head>
        html.addRule(new BodyTagRule(builder, body)); // contents of <body>
        content.addProperty(HEAD, new CharArrayProperty(head));
        content.addProperty(BODY, new CharArrayProperty(body));

        html.addRule(new TitleExtractingRule(builder)); // the <title>
        html.addRule(new FramesetRule(builder)); // if the page is a frameset

        // Ensure that while in <xml> tag, none of the other rules kick in.
        // For example <xml><book><title>hello</title></book></xml> should not affect the title of the page.
        State xml = new State();
        html.addRule(new StateTransitionRule("xml", xml));

        // Additional rules - designed to be tweaked.
        addUserDefinedRules(html, xml, builder);

        processor.process();

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
