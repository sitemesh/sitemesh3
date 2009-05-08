/*
 * This is the definition (lexer.flex) for the auto-generated lexer (Lexer.java) created by JFlex <http://jflex.de/>.
 * To regenerate Lexer.java, run 'mvn jflex:generate' from the sitemesh directory
 * (this will be run automatically on other mvn goals such as 'compile', 'package', etc).
 *
 * @author Joe Walnes
 */

// class headers
package org.sitemesh.tagprocessor;
%%

// class and lexer definitions
%class Lexer
%type TagTokenizer.Token
%function nextToken
%final
%unicode
%byaccj
%char
%ignorecase

// useful for debugging, but adds overhead
//%line
//%column

// Profiling showed that this mode was slightly faster than %pack or %table.
%switch
// Profiling showed this as an optimal size buffer that was often filled but rarely exceeded.
%buffer 2048

%{
    // Additional methods to add to generated Lexer to aid in error reporting.
    private TagTokenizer.TokenHandler handler;
    public void setHandler(TagTokenizer.TokenHandler handler) { this.handler = handler; }
    public int position() { return yychar; }
    public int length()   { return yylength(); }
    public int line()     { return -1; /*yyline;*/ }   // useful for debugging, but adds overhead
    public int column()   { return -1; /*yycolumn;*/ } // useful for debugging, but adds overhead
    public void resetLexerState() { yybegin(YYINITIAL); }
%}

/* Additional states that the lexer can switch into. */
%state ELEMENT

%%

/* Initial state of lexer. */
<YYINITIAL> {
    "<!--" [^\[] ~"-->" { return TagTokenizer.Token.TEXT; } /* All comments unless they start with <!--[ */
    "<!---->"           { return TagTokenizer.Token.TEXT; }
    "<?" ~"?>"          { return TagTokenizer.Token.TEXT; }
    "<!" [^\[\-] ~">"     { return TagTokenizer.Token.TEXT; }
    "<![CDATA[" ~"]]>"  { return TagTokenizer.Token.TEXT; }
    "<xmp" ~"</xmp" ~">" { return TagTokenizer.Token.TEXT; }
    "<script" ~"</script" ~">" { return TagTokenizer.Token.TEXT; }
    [^<]+               { return TagTokenizer.Token.TEXT; }
    "<"                 { yybegin(ELEMENT); return TagTokenizer.Token.LT; }
    "<!--["             { yybegin(ELEMENT); return TagTokenizer.Token.LT_OPEN_MAGIC_COMMENT; }
    "<!["               { yybegin(ELEMENT); return TagTokenizer.Token.LT_CLOSE_MAGIC_COMMENT; }
}

/* State of lexer when inside an element/tag. */
<ELEMENT> {
    "/"                 { return TagTokenizer.Token.SLASH; }
    [\n\r \t\b\012]+    { return TagTokenizer.Token.WHITESPACE; }
    "="                 { return TagTokenizer.Token.EQUALS; }
    "\"" ~"\""          { return TagTokenizer.Token.QUOTED; }
    "'" ~"'"            { return TagTokenizer.Token.QUOTED; }
    [^>\]/=\"\'\n\r \t\b\012][^>\]/=\n\r \t\b\012]* { return TagTokenizer.Token.WORD; }
    ">"                 { yybegin(YYINITIAL); return TagTokenizer.Token.GT; }
    "]>"                { yybegin(YYINITIAL); return TagTokenizer.Token.GT; }
    "]-->"              { yybegin(YYINITIAL); return TagTokenizer.Token.GT; }
}

/* Fallback rule - if nothing else matches. */
.|\n                    { handler.warning("Illegal character <"+ yytext() +">",
                              line(), column()); return TagTokenizer.Token.TEXT; }

/* End of file. */
<<EOF>>                 { return TagTokenizer.Token.EOF; }
