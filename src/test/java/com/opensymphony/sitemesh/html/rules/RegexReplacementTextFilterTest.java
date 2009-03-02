package com.opensymphony.sitemesh.html.rules;

import com.opensymphony.sitemesh.tagprocessor.TagProcessor;
import com.opensymphony.sitemesh.tagprocessor.util.CharArray;
import junit.framework.TestCase;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * @author Joe Walnes
 */
public class RegexReplacementTextFilterTest extends TestCase {

    public void testReplacesTextContentMatchedByRegularExpression() throws IOException {
        CharBuffer in = CharBuffer.wrap("<hello>Today is DATE so hi</hello>");
        CharArray out = new CharArray();

        TagProcessor processor = new TagProcessor(in, out);
        processor.addTextFilter(new RegexReplacementTextFilter("DATE", "1-jan-2009"));

        processor.process();
        assertEquals("<hello>Today is 1-jan-2009 so hi</hello>", out.toString());
    }

    public void testAllowsMatchedGroupToBeUsedInSubsitution() throws IOException {
        CharBuffer in = CharBuffer.wrap("<hello>I think JIRA:SIM-1234 is the way forward</hello>");
        CharArray out = new CharArray();

        TagProcessor processor = new TagProcessor(in, out);
        processor.addTextFilter(new RegexReplacementTextFilter(
                "JIRA:([A-Z]+\\-[0-9]+)",
                "<a href='http://jira.opensymhony.com/browse/$1'>$1</a>"));

        processor.process();
        assertEquals(
                "<hello>I think <a href='http://jira.opensymhony.com/browse/SIM-1234'>" +
                        "SIM-1234</a> is the way forward</hello>",
                out.toString());
    }

}
