package org.sitemesh.html;

import org.sitemesh.TestUtil;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.ContentProperty;
import org.sitemesh.content.Content;
import org.sitemesh.SiteMeshContextStub;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.ByteArrayInputStream;
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
    public static void buildSuite(TestSuite suite, ContentProcessor processor, String... inputFileNames)
            throws IOException {
        File testDataDir = TestUtil.findDir("sitemesh/src/test/java/org/sitemesh/html/testdata");

        for (String inputFileName : inputFileNames) {
            File inputFile = new File(testDataDir, inputFileName);

            Map<String, String> expectedBlocks = readBlocks(new FileReader(inputFile));

            Content content = processor.build(CharBuffer.wrap(expectedBlocks.get("INPUT")), new SiteMeshContextStub());
            ContentProperty contentProperty = content.getExtractedProperties();

            TestSuite inputSuite = new TestSuite(inputFile.getName().replace('.', '_'));
            inputSuite.addTest(new AssertTrimmedTest(
                    inputSuite.getName() + "_testTitle",
                    expectedBlocks.get("TITLE"),
                    contentProperty.getChild("title").getValue()));
            inputSuite.addTest(new AssertTrimmedTest(
                    inputSuite.getName() + "_testBody",
                    expectedBlocks.get("BODY"),
                    contentProperty.getChild("body").getValue()));
            inputSuite.addTest(new AssertTrimmedTest(
                    inputSuite.getName() + "_testHead",
                    expectedBlocks.get("HEAD"),
                    contentProperty.getChild("head").getValue()));
            inputSuite.addTest(new AssertTrimmedTest(
                    inputSuite.getName() + "_testOriginal",
                    expectedBlocks.get("INPUT"),
                    content.getData().getValue()));
            inputSuite.addTest(new AssertTrimmedTest(
                    inputSuite.getName() + "_testProperties",
                    cleanExpectedProperties(expectedBlocks.get("PROPERTIES")),
                    cleanActualProperties(contentProperty)));

            suite.addTest(inputSuite);
        }
    }

    private static String cleanActualProperties(ContentProperty content) {
        Map<String, String> actualProperties = new HashMap<String, String>();
        for (ContentProperty property : content.getDescendants()) {
            String fullName = getFullName(property);
            if (!property.hasValue() || fullName.equals("body") || fullName.equals("head") || fullName.equals("")) {
                continue;
            }
            String actualValue = trimSafely(property.getValue());
            actualProperties.put(fullName, actualValue);
        }

        return sortMapAndDumpAsString(actualProperties);
    }

    private static String getFullName(ContentProperty property) {
        StringBuilder result = new StringBuilder();
        for (ContentProperty item : property.getFullPath()) {
            if (result.length() > 0) {
                result.append('.');
            }
            result.append(item.getName());
        }
        return result.toString();
    }

    private static String cleanExpectedProperties(String string) throws IOException {
        Properties expectedProperties = new Properties();
        expectedProperties.load(new ByteArrayInputStream(trimSafely(string).getBytes()));
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
            assertEquals("Assertion failed for test " + getName(), trimSafely(expected), trimSafely(actual));
        }

    }

}
