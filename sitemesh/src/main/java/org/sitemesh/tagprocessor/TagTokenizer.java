/*
 * IF YOU ARE HAVING TROUBLE COMPILING THIS CLASS, IT IS PROBABLY BECAUSE Lexer.java IS MISSING.
 *
 * To regenerate Lexer.java, run 'mvn jflex:generate' from the sitemesh directory
 * (this will be run automatically on other mvn goals such as 'compile', 'package', etc).
 */

package org.sitemesh.tagprocessor;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

/**
 * Splits a chunk of HTML into 'text' and 'tag' tokens, for easy processing. Is very tolerant to badly formed HTML.
 * <h3>Usage</h3>
 * <p>You need to supply a custom {@link TokenHandler} that will receive callbacks as text and tags are processed.</p>
 * <pre>char[] input = ...;
 * TokenHandler handler = new MyTokenHandler();
 * HTMLTagTokenizer tokenizer = new HTMLTagTokenizer(input, handler);
 * tokenizer.start();</pre>
 *
 * @author Joe Walnes
 */
public class TagTokenizer {

    /**
     * Handler that will receive callbacks as 'tags' and 'text' are encountered.
     */
    public static interface TokenHandler {

        /**
         * Before attempting to parse a tag, the tokenizer will ask the handler whether the tag should be processed -
         * avoiding additional tag parsing makes the tokenizer quicker.
         * <p/>
         * If true is returned, the tokenizer will fully parse the tag and pass it into the
         * {@link #tag(Tag)} method.
         * Otherwise, the tokenizer will not try to parse the tag and pass it to the
         * {@link #text(CharSequence)} method, untouched.
         */
        boolean shouldProcessTag(String name);

        /**
         * Called when tokenizer encounters an HTML tag (open, close or empty).
         * <p/>The Tag instance passed in should not be kept beyond the scope of this method as the tokenizer will
         * attempt to reuse it.</p>
         */
        void tag(Tag tag) throws IOException;

        /**
         * Called when tokenizer encounters anything other than a well-formed HTML tag.
         */
        void text(CharSequence text) throws IOException;

        /**
         * Called when tokenizer encounters something it cannot correctly parse. Typically the parsing will continue
         * and the unparseable will be treated as a plain text block, however this callback provides indication of this.
         *
         * @param message Error message
         * @param line    Line number in input that error occured
         * @param column  Column number in input that error occured
         */
        void warning(String message, int line, int column);

    }

    private final Lexer lexer; // If you are getting a compilation error on this line, see comment at top of this file.
    private final ReusableToken reusableToken = new ReusableToken();

    private Token pushbackToken = Token.UNKNOWN;
    private String pushbackText;

    public static enum Token {
        UNKNOWN, SLASH, WHITESPACE, EQUALS, QUOTE, WORD, TEXT, QUOTED, LT, GT,
        LT_OPEN_MAGIC_COMMENT, LT_CLOSE_MAGIC_COMMENT, EOF
    }

    private final CharSequence input;

    private int position;
    private int length;

    private boolean bufferingText;
    private int bufferedTextStart;
    private int bufferedTextEnd;
    
    private String name;
    private Tag.Type type;
    private final TokenHandler handler;

    public TagTokenizer(final CharBuffer input, TokenHandler handler) {
        this.handler = handler;
        lexer = new Lexer(new CharBufferReader(input));
        lexer.setHandler(handler);
        this.input = input;
    }

    public void start() {
        try {
            while (true) {
                Token token;
                if (pushbackToken == Token.UNKNOWN) {
                    token = lexer.nextToken();
                } else {
                    token = pushbackToken;
                    pushbackToken = Token.UNKNOWN;
                }
                if (token == Token.EOF) {
                    flushText();
                    return;
                } else if (token == Token.TEXT) {
                    // Got some text
                    int start = lexer.position();
                    parsedText(start, start + lexer.length());
                } else if (token == Token.LT) {
                    // Token "<" - start of tag
                    parseTag(Tag.Type.OPEN);
                } else if (token == Token.LT_OPEN_MAGIC_COMMENT) {
                    // Token "<!--[" - start of open magic comment
                    parseTag(Tag.Type.OPEN_CONDITIONAL_COMMENT);
                } else if (token == Token.LT_CLOSE_MAGIC_COMMENT) {
                    // Token "<![" - start of close magic comment
                    parseTag(Tag.Type.CLOSE_CONDITIONAL_COMMENT);
                } else {
                    reportError("Unexpected token from lexer, was expecting TEXT or LT", lexer.line(), lexer.column());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String text() {
        if (pushbackToken == Token.UNKNOWN) {
            return lexer.yytext();
        } else {
            return pushbackText;
        }
    }

    private void skipWhiteSpace() throws IOException {
        while (true) {
            Token next;
            if (pushbackToken == Token.UNKNOWN) {
                next = lexer.nextToken();
            } else {
                next = pushbackToken;
                pushbackToken = Token.UNKNOWN;
            }
            if (next != Token.WHITESPACE) {
                pushBack(next);
                break;
            }
        }
    }

    private void pushBack(Token next) {
        if (pushbackToken != Token.UNKNOWN) {
            reportError("Cannot pushback more than once", lexer.line(), lexer.column());
        }
        pushbackToken = next;
        if (next == Token.WORD || next == Token.QUOTED || next == Token.SLASH || next == Token.EQUALS) {
            pushbackText = lexer.yytext();
        } else {
            pushbackText = null;
        }
    }

    private void parseTag(Tag.Type type) throws IOException {
        // Start parsing a TAG

        int start = lexer.position();
        skipWhiteSpace();
        Token token;
        if (pushbackToken == Token.UNKNOWN) {
            token = lexer.nextToken();
        } else {
            token = pushbackToken;
            pushbackToken = Token.UNKNOWN;
        }

        if (token == Token.SLASH) {
            // Token "/" - it's a closing tag
            type = Tag.Type.CLOSE;
            if (pushbackToken == Token.UNKNOWN) {
                token = lexer.nextToken();
            } else {
                token = pushbackToken;
                pushbackToken = Token.UNKNOWN;
            }
        }

        if (token == Token.WORD) {
            // Token WORD - name of tag
            String name = text();
            if (handler.shouldProcessTag(name)) {
                parseFullTag(type, name, start);
            } else {
                lexer.resetLexerState();
                pushBack(lexer.nextToken()); // take and replace the next token, so the position is correct
                parsedText(start, lexer.position());
            }
        } else if (token == Token.GT) {
            // Token ">" - an illegal <> or <  > tag. Treat as text.
            parsedText(start, lexer.position() + 1); // eof
        } else if (token == Token.EOF) {
            parsedText(start, lexer.position()); // eof
        } else {
            reportError("Could not recognise tag", lexer.line(), lexer.column());
        }
    }

    private void parseFullTag(Tag.Type type, String name, int start) throws IOException {
        Token token;
        while (true) {
            skipWhiteSpace();
            if (pushbackToken == Token.UNKNOWN) {
                token = lexer.nextToken();
            } else {
                token = pushbackToken;
                pushbackToken = Token.UNKNOWN;
            }
            pushBack(token);

            if (token == Token.SLASH || token == Token.GT) {
                break; // no more attributes here
            } else if (token == Token.WORD) {
                parseAttribute(); // start of an attribute
            } else if (token == Token.EOF) {
                parsedText(start, lexer.position()); // eof
                return;
            } else {
                reportError("Illegal tag", lexer.line(), lexer.column());
                break;
            }
        }

        if (pushbackToken == Token.UNKNOWN) {
            token = lexer.nextToken();
        } else {
            token = pushbackToken;
            pushbackToken = Token.UNKNOWN;
        }
        if (token == Token.SLASH) {
            // Token "/" - it's an empty tag
            type = Tag.Type.EMPTY;
            if (pushbackToken == Token.UNKNOWN) {
                token = lexer.nextToken();
            } else {
                token = pushbackToken;
                pushbackToken = Token.UNKNOWN;
            }
        }

        if (token == Token.GT) {
            // Token ">" - YAY! end of tag.. process it!
            parsedTag(type, name, start, lexer.position() - start + 1);
        } else if (token == Token.EOF) {
            parsedText(start, lexer.position()); // eof
        } else {
            reportError("Expected end of tag", lexer.line(), lexer.column());
            parsedTag(type, name, start, lexer.position() - start + 1);
        }
    }

    private void parseAttribute() throws IOException {
        if (pushbackToken == Token.UNKNOWN) {
            lexer.nextToken();
        } else {
            pushbackToken = Token.UNKNOWN;
        }
        // Token WORD - start of an attribute
        String attributeName = text();
        skipWhiteSpace();
        Token token;
        if (pushbackToken == Token.UNKNOWN) {
            token = lexer.nextToken();
        } else {
            token = pushbackToken;
            pushbackToken = Token.UNKNOWN;
        }
        if (token == Token.EQUALS) {
            // Token "=" - the attribute has a value
            skipWhiteSpace();
            if (pushbackToken == Token.UNKNOWN) {
                token = lexer.nextToken();
            } else {
                token = pushbackToken;
                pushbackToken = Token.UNKNOWN;
            }
            if (token == Token.QUOTED) {
                // token QUOTED - a quoted literal as the attribute value
                parsedAttribute(attributeName, text(), true);
            } else if (token == Token.WORD || token == Token.SLASH) {
                // unquoted word
                String attributeValue = text();
                while (true) {
                    Token next;
                    if (pushbackToken == Token.UNKNOWN) {
                        next = lexer.nextToken();
                    } else {
                        next = pushbackToken;
                        pushbackToken = Token.UNKNOWN;
                    }
                    if (next == Token.WORD || next == Token.EQUALS || next == Token.SLASH) {
                        // This is such a rare case, that it's more efficient to concatenate a string
                        // like this, rather than use a StringBuilder each time. 99.99% of the time,
                        // this will never be called.
                        attributeValue += text();
                    } else {
                        pushBack(next);
                        break;
                    }
                }
                parsedAttribute(attributeName, attributeValue, false);
            } else if (token == Token.SLASH || token == Token.GT) {
                // no more attributes
                pushBack(token);
            } else if (token != Token.EOF) {
                reportError("Illegal attribute value", lexer.line(), lexer.column());
            }
        } else if (token == Token.SLASH || token == Token.GT || token == Token.WORD) {
            // it was a value-less HTML style attribute
            parsedAttribute(attributeName, null, false);
            pushBack(token);
        } else if (token != Token.EOF) {
            reportError("Illegal attribute name", lexer.line(), lexer.column());
        }
    }

    private void flushText() throws IOException {
        if (bufferingText) {
            handler.text(input.subSequence(bufferedTextStart, bufferedTextEnd));
            bufferingText = false;
        }
    }

    private void parsedText(int start, int end) throws IOException {
        if (!bufferingText) {
            bufferingText = true;
            bufferedTextStart = start;
            bufferedTextEnd = end;
        } else {
            assert bufferedTextEnd == start : "Parser missed something. Please report this bug to SiteMesh team.";
            bufferedTextEnd = end;
        }
    }

    private void parsedTag(Tag.Type type, String name, int start, int length) throws IOException {
        flushText();
        this.type = type;
        this.name = name;
        this.position = start;
        this.length = length;
        handler.tag(reusableToken);
        reusableToken.attributeCount = 0;
    }

    private void parsedAttribute(String name, String value, boolean quoted) {
        if (reusableToken.attributeCount + 2 >= reusableToken.attributes.length) {
            String[] newAttributes = new String[reusableToken.attributeCount * 2];
            System.arraycopy(reusableToken.attributes, 0, newAttributes, 0, reusableToken.attributeCount);
            reusableToken.attributes = newAttributes;
        }
        reusableToken.attributes[reusableToken.attributeCount++] = name;
        if (quoted) {
            reusableToken.attributes[reusableToken.attributeCount++] = value.substring(1, value.length() - 1);
        } else {
            reusableToken.attributes[reusableToken.attributeCount++] = value;
        }
    }

    private void reportError(String message, int line, int column) {
        handler.warning(message, line, column);
    }

    public class ReusableToken implements Tag {

        public int attributeCount = 0;
        public String[] attributes = new String[10]; // name1, value1, name2, value2...

        public String getName() {
            return name;
        }

        public Tag.Type getType() {
            return type;
        }

        public void writeTo(Appendable out) throws IOException {
            out.append(input.subSequence(position, position + length));
        }

        public int getAttributeCount() {
            return attributeCount / 2;
        }

        public int getAttributeIndex(String name, boolean caseSensitive) {
            if (attributeCount == 0)
                return -1;
            final int len = attributeCount;
            for (int i = 0; i < len; i += 2) {
                final String current = attributes[i];
                if (caseSensitive ? name.equals(current) : name.equalsIgnoreCase(current)) {
                    return i / 2;
                }
            }
            return -1;
        }

        public String getAttributeName(int index) {
            return attributes[index * 2];
        }

        public String getAttributeValue(int index) {
            return attributes[index * 2 + 1];
        }

        public String getAttributeValue(String name, boolean caseSensitive) {
            if (attributeCount == 0)
                return null;
            final int len = attributeCount;
            for (int i = 0; i < len; i += 2) {
                final String current = attributes[i];
                if (caseSensitive ? name.equals(current) : name.equalsIgnoreCase(current)) {
                    return attributes[i + 1];
                }
            }
            return null;
        }

        public boolean hasAttribute(String name, boolean caseSensitive) {
            return getAttributeIndex(name, caseSensitive) > -1;
        }

        public String toString() {
            return input.subSequence(position, position + length).toString();
        }
    }

    /**
     * Reader that wraps a CharBuffer.
     */
    private static class CharBufferReader extends Reader {
        private final CharBuffer input;

        public CharBufferReader(CharBuffer input) {
            this.input = input.duplicate(); // Allow to move independently, but share the same underlying data.
        }

        @Override
        public int read(char[] chars, int offset, int length) throws IOException {
            int read = Math.min(input.remaining(), length);
            input.get(chars, offset, read);
            return read;
        }

        @Override
        public int read() throws IOException {
            return input.position() < input.limit() ? input.get() : -1;
        }

        @Override
        public void close() throws IOException {
            // No op.
        }
    }
}

