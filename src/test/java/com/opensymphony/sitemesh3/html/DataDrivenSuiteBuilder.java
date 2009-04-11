package com.opensymphony.sitemesh3.html;

import com.opensymphony.sitemesh3.Content;
import com.opensymphony.sitemesh3.ContentProcessor;
import com.opensymphony.sitemesh3.SiteMeshContextStub;
import com.opensymphony.sitemesh3.SiteMeshContext;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Builds a data driven JUnit test suite from a set of input files. See testdata/readme.txt.
 *
 * @author Joe Walnes
 */
public class DataDrivenSuiteBuilder {

    /**
     * Builds a TestSuite, containing a collection of smaller suites
     * (one for each file of testdata/text??.txt).
     */
    public static void buildSuite(TestSuite suite, ContentProcessor<SiteMeshContext> processor, String... inputFileNames)
            throws IOException {
        File testDataDir = new File("src/test/java/com/opensymphony/sitemesh3/html/testdata");

        for (String inputFileName : inputFileNames) {
            File inputFile = new File(testDataDir, inputFileName);

            Map<String, String> expectedBlocks = readBlocks(new FileReader(inputFile));
            Content content = processor.build(CharBuffer.wrap(expectedBlocks.get("INPUT")), new SiteMeshContextStub());

            TestSuite inputSuite = new TestSuite(inputFile.getName().replace('.', '_'));
            inputSuite.addTest(new AssertTrimmedTest(
                    "testTitle",
                    expectedBlocks.get("TITLE"),
                    content.getProperty("title").value()));
            inputSuite.addTest(new AssertTrimmedTest(
                    "testBody",
                    expectedBlocks.get("BODY"),
                    content.getProperty("body").value()));
            inputSuite.addTest(new AssertTrimmedTest(
                    "testHead",
                    expectedBlocks.get("HEAD"),
                    content.getProperty("head").value()));
            inputSuite.addTest(new AssertTrimmedTest(
                    "testOriginal",
                    expectedBlocks.get("INPUT"),
                    content.getOriginal().value()));
            inputSuite.addTest(new AssertTrimmedTest(
                    "testProperties",
                    cleanExpectedProperties(expectedBlocks.get("PROPERTIES")),
                    cleanActualProperties(content)));

            suite.addTest(inputSuite);
        }
    }

    private static String cleanActualProperties(Content content) {
        Map<String, String> actualProperties = new HashMap<String, String>();
        for (Map.Entry<String, Content.Property> propertyEntry : content) {
            String name = propertyEntry.getKey();
            if (name.equals("body") || name.equals("head")) {
                continue;
            }
            String actualValue = trimSafely(propertyEntry.getValue().value());
            actualProperties.put(name, actualValue);
        }

        return sortMapAndDumpAsString(actualProperties);
    }

    private static String cleanExpectedProperties(String string) throws IOException {
        Properties expectedProperties = new Properties();
        expectedProperties.load(new StringReader(trimSafely(string)));
        return sortMapAndDumpAsString(expectedProperties);
    }

    /**
     * Read input to test and break down into blocks. See parser-tests/readme.txt
     */
    private static Map<String, String> readBlocks(Reader input) throws IOException {
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

    private static String sortMapAndDumpAsString(Map m) {
        StringBuffer out = new StringBuffer();
        List keys = new ArrayList(m.keySet());
        Collections.sort(keys);
        for (Object key : keys) {
            Object value = m.get(key);
            out.append(key).append(" = ").append(value).append('\n');
        }
        return out.toString();
    }

    private static String trimSafely(String string) {
        return string == null ? "" : string.trim();
    }

    private static class AssertTrimmedTest extends TestCase {
        private final String expected;
        private final String actual;

        public AssertTrimmedTest(String testName, String expected, String actual) {
            setName(testName);
            this.expected = expected;
            this.actual = actual;
        }

        @Override
        protected void runTest() throws Throwable {
            assertEquals(trimSafely(expected), trimSafely(actual));
        }

    }

}
