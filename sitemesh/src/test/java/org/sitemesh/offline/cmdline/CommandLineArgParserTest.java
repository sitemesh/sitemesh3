package org.sitemesh.offline.cmdline;

import junit.framework.TestCase;

import java.util.Map;
import java.util.HashMap;

/**
 * @author Joe Walnes
 */
public class CommandLineArgParserTest extends TestCase {

    private CommandLineArgParser parser = new CommandLineArgParser();
    private Map<String,String> expected = new HashMap<String, String>();

    public void testParsesDoubleDashedCmdLineArgs() {
        expected.put("a",  "A value");
        expected.put("bee",  "Bee value");

        assertEquals(expected, parser.parseCommandLine("--a", "A value", "--bee", "Bee value"));
    }

    public void testParsesSingleDashedCmdLineArgs() {
        expected.put("a",  "A value");
        expected.put("bee",  "Bee value");

        assertEquals(expected, parser.parseCommandLine("-a", "A value", "-bee", "Bee value"));
    }

    public void testParsesEqualsDelimitedCmdLineArgs() {
        expected.put("a",  "A value");
        expected.put("bee",  "Bee value=something");

        assertEquals(expected, parser.parseCommandLine("-a=A value", "--bee=Bee value=something"));
    }

    public void testTreatsMissingValuesAsEmpty() {
        expected.put("a",  "");
        expected.put("bee",  "");
        expected.put("c",  "C value");
        expected.put("d",  "");

        assertEquals(expected, parser.parseCommandLine("-a", "--bee", "-c", "C value", "--d"));
    }

    public void testIgnoresLeadingValue() {
        expected.put("a",  "");

        assertEquals(expected, parser.parseCommandLine("ignore me", "-a"));
    }
}
