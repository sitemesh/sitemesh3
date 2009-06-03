package org.sitemesh.tagprocessor;

import junit.framework.TestCase;

import java.nio.CharBuffer;
import java.io.IOException;

/**
 * @author Joe Walnes
 */
public class TagTokenizerTest extends TestCase {

    private MockTokenHandler handler;

    protected void setUp() throws Exception {
        super.setUp();
        handler = new MockTokenHandler();
    }

    public void testSplitsTagsFromText() {
        // expectations
        handler.expectTag(Tag.Type.OPEN, "hello");
        handler.expectText("cruel");
        handler.expectTag(Tag.Type.OPEN, "world");
        handler.expectTag(Tag.Type.OPEN, "and");
        handler.expectText("some stuff");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("<hello>cruel<world><and>some stuff"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testDistinguishesBetweenOpenCloseAndEmptyTags() {
        // expectations
        handler.expectTag(Tag.Type.OPEN, "open");
        handler.expectTag(Tag.Type.CLOSE, "close");
        handler.expectTag(Tag.Type.EMPTY, "empty");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("<open></close><empty/>"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testTreatsCommentsAsText() {
        // expectations
        handler.expectText("hello world <!-- how are<we> \n -doing? --><!-- --><!---->good\n bye.");
        handler.expectTag(Tag.Type.OPEN, "br");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("hello world <!-- how are<we> \n -doing? --><!-- -->" +
                        "<!---->good\n bye.<br>"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testExtractsUnquotedAttributesFromTag() {
        // expectations
        handler.expectTag(Tag.Type.OPEN, "hello", new String[]{"name", "world", "foo", "boo"});
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("<hello name=world foo=boo>"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testExtractsQuotedAttributesFromTag() {
        // expectations
        handler.expectTag(Tag.Type.OPEN, "hello", new String[]{"name", "the world", "foo", "boo"});
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("<hello name=\"the world\" foo=\"boo\">"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testHandlesMixedQuoteTypesInAttributes() {
        // expectations
        handler.expectTag(Tag.Type.OPEN, "hello", new String[]{"name", "it's good", "foo", "say \"boo\""});
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("<hello name=\"it's good\" foo=\'say \"boo\"'>"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testHandlesHtmlStyleEmptyAttributes() {
        // expectations
        handler.expectTag(Tag.Type.OPEN, "hello", new String[]{"isgood", null, "and", null, "stuff", null});
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("<hello isgood and stuff>"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testSupportsWhitespaceInElements() {
        // expectations
        handler.expectTag(Tag.Type.OPEN, "hello", new String[]{"somestuff", "good", "foo", null, "x", "long\n string"});
        handler.expectTag(Tag.Type.EMPTY, "empty");
        handler.expectTag(Tag.Type.OPEN, "HTML",
                new String[]{"notonnewline", "yo", "newline", "hello", "anotherline", "bye"});
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap(""
                + "<hello \n somestuff = \ngood \n   foo \nx=\"long\n string\"   >"
                        + "<empty      />"
                        + "<HTML notonnewline=yo newline=\n"
                        + "hello anotherline=\n"
                        + "\"bye\">"), handler);
        tokenizer.start();
        // verify
        handler.verify();


    }

    public void testExposesOriginalTagToHandler() {
        // Should really use a mock library for this expectation, but I'd rather not
        // add a new dependency for the sake of a single test.
        final String originalTag = "<hello \n somestuff = \ngood \n   foo \nx=\"long\n string\"   >";
        final boolean[] called = {false}; // has to be final array so anonymous inner class can change the value.
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("some text" + originalTag + "more text"), new TagTokenizer.TokenHandler() {

            public boolean shouldProcessTag(String name) {
                return true;
            }

            public void tag(Tag tag) {
                assertEquals(originalTag, tag.toString());
                called[0] = true;
            }

            public void text(CharSequence text) {
                // ignoring text for this test
            }

            public void warning(String message, int line, int column) {
                fail("Encountered error " + message);
            }
        });

        tokenizer.start();

        assertTrue("tag() never called", called[0]);
    }

    public void testAllowsSlashInUnquotedAttribute() {
        // expectations
        handler.expectTag(Tag.Type.OPEN, "something", new String[]{"type", "text/html"});
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("<something type=text/html>"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testAllowsTrailingQuoteOnAttribute() {
        // expectations
        handler.expectTag(Tag.Type.OPEN, "something", new String[]{"type", "bl'ah\""});
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("<something type=bl'ah\">"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testAllowsAwkwardCharsInElementAndAttribute() {
        // expectations
        handler.expectTag(Tag.Type.OPEN, "name:space", new String[]{"foo:bar", "x:y%"});
        handler.expectTag(Tag.Type.EMPTY, "a_b-c$d", new String[]{"b_b-c$d", "c_b=c$d"});
        handler.expectTag(Tag.Type.OPEN, "a", new String[]{"href", "/exec/obidos/flex-sign-in/ref=pd_nfy_gw_si/" +
                "026-2634699-7306802?opt=a&page=misc/login/flex-sign-in-secure.html&response=tg/new-for-you" +
                "/new-for-you/-/main"});
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap(""
                + "<name:space foo:bar=x:y%>"
                        + "<a_b-c$d b_b-c$d=c_b=c$d />"
                        + "<a href=/exec/obidos/flex-sign-in/ref=pd_nfy_gw_si/026-2634699-7306802?opt=a&page=misc/" +
                        "login/flex-sign-in-secure.html&response=tg/new-for-you/new-for-you/-/main>"), handler);
        tokenizer.start();
        // verify
        handler.verify();

    }

    public void testTreatsXmpCdataScriptAndProcessingInstructionsAsText() {
        // expectations
        handler.expectText("<script language=jscript> if (a < b & > c)\n alert(); </script>"
                + "<xmp><evil \n<stuff<</xmp>"
                + "<?some stuff ?>"
                + "<![CDATA[ evil<>> <\n    ]]>"
                + "<SCRIPT>stuff</SCRIPT>"
                + "<!DOCTYPE html PUBLIC \\\"-//W3C//DTD HTML 4.01 Transitional//EN\\\">");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap(""
                + "<script language=jscript> if (a < b & > c)\n alert(); </script>"
                + "<xmp><evil \n<stuff<</xmp>"
                + "<?some stuff ?>"
                + "<![CDATA[ evil<>> <\n    ]]>"
                + "<SCRIPT>stuff</SCRIPT>"
                + "<!DOCTYPE html PUBLIC \\\"-//W3C//DTD HTML 4.01 Transitional//EN\\\">"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testTreatsUnterminatedTagAtEofAsText() {
        // expectations
        handler.expectText("hello<world");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("hello<world"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testTreatsLtAtEofAsText() {
        // expectations
        handler.expectText("hello<");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("hello<"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testTreatsUnterminatedAttributeNameAtEofAsText() {
        // expectations
        handler.expectText("hello<world x");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("hello<world x"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    /* TODO
    public void testTreatsUnterminatedQuotedAttributeValueAtEofAsText() {
        // expectations
        handler.expectText("hello<world x=\"fff");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("hello<world x=\"fff"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }
    */

    public void testTreatsUnterminatedAttributeAtEofAsText() {
        // expectations
        handler.expectText("hello<world x=");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("hello<world x="), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testTreatsUnterminatedUnquotedAttributeValueAtEofAsText() {
        // expectations
        handler.expectText("hello<world x=fff");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("hello<world x=fff"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testTreatsUnterminatedClosingTagAtEofAsText() {
        // expectations
        handler.expectText("hello<world /");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("hello<world /"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testTreatsEvilMalformedPairOfAngleBracketsAsText() {
        // expectations
        handler.expectText("<></>< >");
        handler.expectTag(Tag.Type.OPEN, "good");
        handler.expectText("<>END<><");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("<></>< ><good><>END<><"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testDoesNotTryToParseTagsUnlessTheHandlerCares() {
        // setup
        handler = new MockTokenHandler() {
            public boolean shouldProcessTag(String name) {
                return name.equals("good");
            }
        };
        // expectations
        handler.expectTag(Tag.Type.OPEN, "good");
        handler.expectText("<bad>");
        handler.expectTag(Tag.Type.CLOSE, "good");
        handler.expectText("<![bad]--><unfinished");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("<good><bad></good><![bad]--><unfinished"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    public void testParsesMagicCommentBlocks() {
        // expectations
        handler.expectTag(Tag.Type.OPEN_CONDITIONAL_COMMENT, "if", new String[]{"gte", null, "mso", null, "9", null});
        handler.expectTag(Tag.Type.OPEN, "stuff");
        handler.expectTag(Tag.Type.CLOSE_CONDITIONAL_COMMENT, "endif");
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("<!--[if gte mso 9]><stuff><![endif]-->"), handler);
        tokenizer.start();
        // verify
        handler.verify();

    }

    public void testToleratesExtraQuoteClosingAttributeValue() {
        // expectations
        handler = new MockTokenHandler() {
            public void warning(String message, int line, int column) {
                // warning ok!
            }
        };
        handler.expectTag(Tag.Type.OPEN, "a", new String[]{"href", "something-with-a-naughty-quote"});
        // execute
        TagTokenizer tokenizer = new TagTokenizer(CharBuffer.wrap("<a href=\"something-with-a-naughty-quote\"\">"), handler);
        tokenizer.start();
        // verify
        handler.verify();
    }

    static class MockTokenHandler implements TagTokenizer.TokenHandler {

        private StringBuffer expected = new StringBuffer();
        private StringBuffer actual = new StringBuffer();

        public void expectText(String tag) {
            expected.append("{{").append(tag).append("}}");
        }

        public void expectTag(Tag.Type type, String tag) {
            expectTag(type, tag, new String[0]);
        }

        public void expectTag(Tag.Type type, String tag, String[] attributes) {
            expected.append("{{TAG : ").append(tag);
            for (int i = 0; i < attributes.length; i += 2) {
                expected.append(' ').append(attributes[i]).append("=\"").append(attributes[i + 1]).append('"');
            }
            expected.append(' ').append(typeAsString(type)).append("}}");
        }

        public boolean shouldProcessTag(String name) {
            assertNotNull("Name should not be null", name);
            return true;
        }

        public void tag(Tag tag) {
            actual.append("{{TAG : ").append(tag.getName());
            for (int i = 0; i < tag.getAttributeCount(); i++) {
                actual.append(' ').append(tag.getAttributeName(i)).append("=\"")
                        .append(tag.getAttributeValue(i)).append('"');
            }
            actual.append(' ').append(typeAsString(tag.getType())).append("}}");
        }

        public void text(CharSequence text) throws IOException {
            actual.append("{{").append(text).append("}}");
        }

        public void warning(String message, int line, int column) {
            fail("Encountered error: " + message);
        }

        public void verify() {
            assertEquals(expected.toString(), actual.toString());
        }

        private String typeAsString(Tag.Type type) {
            switch (type) {
                case OPEN:
                    return "*open*";
                case CLOSE:
                    return "*close*";
                case EMPTY:
                    return "*empty*";
                default:
                    return "*unknown*";
            }
        }

    }
}

