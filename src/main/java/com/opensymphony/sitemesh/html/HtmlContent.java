package com.opensymphony.sitemesh.html;

import com.opensymphony.sitemesh.BaseContent;
import com.opensymphony.sitemesh.tagprocessor.util.CharArray;
import com.opensymphony.sitemesh.html.rules.*;
import com.opensymphony.sitemesh.tagprocessor.State;
import com.opensymphony.sitemesh.tagprocessor.StateTransitionRule;
import com.opensymphony.sitemesh.tagprocessor.TagProcessor;

import java.io.IOException;

/**
 * {@link com.opensymphony.sitemesh.Content} implementation that will build itself from
 * an HTML page, built on {@link TagProcessor}.
 * <p/>
 * Users can override {@link #addUserDefinedRules(State, State, PageBuilder)} to customize
 * the tag processing rules.
 *
 * @author Joe Walnes
 */
public class HtmlContent extends BaseContent implements PageBuilder {

    public static final String BODY = "body";
    public static final String HEAD = "head";
    public static final String TITLE = "title";

    public HtmlContent(String data) throws IOException {
        super(data);
    }

    @Override
    protected void processContent(String original) throws IOException {
        CharArray head = new CharArray(64);
        CharArray body = new CharArray(4096);

        TagProcessor processor = new TagProcessor(original.toCharArray() /*TODO*/, body);
        State html = processor.defaultState();

        // Core rules for SiteMesh to be functional.
        html.addRule(new HeadExtractingRule(head)); // contents of <head>
        html.addRule(new BodyTagRule(this, body)); // contents of <body>
        addProperty(HEAD, new CharArrayProperty(head));
        addProperty(BODY, new CharArrayProperty(body));

        html.addRule(new TitleExtractingRule(this)); // the <title>
        html.addRule(new FramesetRule(this)); // if the page is a frameset

        // Ensure that while in <xml> tag, none of the other rules kick in.
        // For example <xml><book><title>hello</title></book></xml> should not affect the title of the page.
        State xml = new State();
        html.addRule(new StateTransitionRule("xml", xml));

        // Additional rules - designed to be tweaked.
        addUserDefinedRules(html, xml, this);

        processor.process();
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

    public Property getBody() {
        return getProperty(BODY);
    }

    public Property getHead() {
        return getProperty(HEAD);
    }

    public Property getTitle() {
        return getProperty(TITLE);
    }
}
