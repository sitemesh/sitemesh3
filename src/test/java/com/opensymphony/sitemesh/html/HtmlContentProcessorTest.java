package com.opensymphony.sitemesh.html;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.io.FilenameFilter;
import java.io.Reader;
import java.io.LineNumberReader;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.nio.CharBuffer;

import com.opensymphony.sitemesh.Content;

/**
 * Test case for HTMLPageParser implementations. See parser-tests/readme.txt.
 *
 * @author Joe Walnes
 */
public class HtmlContentProcessorTest extends TestCase {

    /**
     * This test case builds a custom suite, containing a collection of smaller suites
     * (one for each inputFile in src/parser-tests).
     */
    public static Test suite() throws IOException {
        TestSuite suite = new TestSuite(HtmlContentProcessorTest.class.getName());
        File testDataDir = new File("src/test/java/com/opensymphony/sitemesh/html/testdata");
        for (File inputFile : listParserTests(testDataDir)) {
            TestSuite suiteForFile = new TestSuite(inputFile.getName().replace('.', '_'));
            suiteForFile.addTest(new HtmlContentProcessorTest(inputFile, "testTitle"));
            suiteForFile.addTest(new HtmlContentProcessorTest(inputFile, "testBody"));
            suiteForFile.addTest(new HtmlContentProcessorTest(inputFile, "testHead"));
            suiteForFile.addTest(new HtmlContentProcessorTest(inputFile, "testFullPage"));
            suiteForFile.addTest(new HtmlContentProcessorTest(inputFile, "testProperties"));
            suite.addTest(suiteForFile);
        }
        return suite;
    }

    private Content content;
    private Map<String, String> expectedBlocks;
    private File inputFile;

    public HtmlContentProcessorTest(File inputFile, String testName) {
        super(testName);
        this.inputFile = inputFile;
    }

    protected void setUp() throws Exception {
        super.setUp();
        this.expectedBlocks = readBlocks(new FileReader(inputFile));
        HtmlContentProcessor processor = new HtmlContentProcessor();
        content = processor.build(CharBuffer.wrap(expectedBlocks.get("INPUT")), null);
    }

    public void testBody() throws Exception {
        assertBlock("BODY", content.getProperty("body").value());
    }

    public void testTitle() throws Exception {
        assertBlock("TITLE", content.getProperty("title").value());
    }

    public void testHead() throws Exception {
        assertBlock("HEAD", content.getProperty("head").value());
    }

    public void testFullPage() throws Exception {
        assertBlock("INPUT", content.getOriginal().value());
    }

    public void testProperties() throws Exception {
        Properties expectedProperties = new Properties();
        expectedProperties.load(new StringReader(trimSafely(expectedBlocks.get("PROPERTIES"))));

        Map<String,String> actualProperties = new HashMap<String,String>();
        for (Map.Entry<String, Content.Property> propertyEntry : content) {
            String name = propertyEntry.getKey();
            if (name.equals("body") || name.equals("head")) {
                continue;
            }
            String actualValue = trimSafely(propertyEntry.getValue().value());
            actualProperties.put(name, actualValue);
        }

        assertEquals(sortMapAndDumpAsString(expectedProperties), sortMapAndDumpAsString(actualProperties));
    }

    private String sortMapAndDumpAsString(Map m) {
        StringBuffer out = new StringBuffer();
        List keys = new ArrayList(m.keySet());
        Collections.sort(keys);
        for (Object key : keys) {
            Object value = m.get(key);
            out.append(key).append(" = ").append(value).append('\n');
        }
        return out.toString();
    }
    //-------------------------------------------------

    private static File[] listParserTests(File dir) throws IOException {
        return dir.listFiles(new FilenameFilter() {
            public boolean accept(File currentDir, String name) {
                return name.startsWith("test");
            }
        });
    }

    private void assertBlock(String blockName, String result) throws Exception {
        String expected = expectedBlocks.get(blockName);
        assertEquals(inputFile.getName() + " : Block did not match", trimSafely(expected), trimSafely(result));
    }

    private String trimSafely(String string) {
        return string == null ? "" : string.trim();
    }

    /**
     * Read input to test and break down into blocks. See parser-tests/readme.txt
     */
    private Map<String, String> readBlocks(Reader input) throws IOException {
        Map<String, String> blocks = new HashMap<String, String>();
        LineNumberReader reader = new LineNumberReader(input);
        String line;
        String blockName = null;
        StringBuffer blockContents = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("~~~ ") && line.endsWith(" ~~~")) {
                if (blockName != null) {
                    blocks.put(blockName, blockContents.toString());
                }
                blockName = line.substring(4, line.length() - 4);
                blockContents = new StringBuffer();
            } else {
                if (blockName != null) {
                    blockContents.append(line);
                    blockContents.append('\n');
                }
            }
        }

        if (blockName != null) {
            blocks.put(blockName, blockContents.toString());
        }

        return blocks;
    }

}
