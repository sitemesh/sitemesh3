package com.opensymphony.sitemesh3.acceptance;

import junit.framework.TestSuite;
import junit.framework.TestCase;
import com.opensymphony.sitemesh3.webapp.WebEnvironment;

import java.util.Map;
import java.util.TreeMap;
import java.util.Scanner;
import java.io.File;
import java.io.IOException;

/**
 * @author Joe Walnes
 */
public class AcceptanceTestSuiteBuilder {

    private static final File BASE_DIR = new File("src/test/java/com/opensymphony/sitemesh3/acceptance");

    public static TestSuite buildWebAppSuite(String suiteName, final WebEnvironment webEnvironment) throws IOException {
        TestSuite suite = new TestSuite(suiteName);

        File expectedDir = getExpectedDir(suiteName);
        final Map<String, String> expected = AcceptanceTestSuiteBuilder.readFiles(expectedDir);
        for (final String fileName : expected.keySet()) {
            suite.addTest(new TestCase(fileName) {
                @Override
                protected void runTest() throws Throwable {
                    webEnvironment.doGet("/" + fileName);
                    assertEquals(reduceWhitespace(expected.get(fileName)),
                            reduceWhitespace(webEnvironment.getBody()));
                }
            });
        }
        return suite;
    }

    private static String reduceWhitespace(String text) {
        return text.replaceAll("\n\\s+", "\n");
    }

    public static Map<String, String> readFiles(File dir) throws IOException {
        Map<String, String> map = new TreeMap<String, String>(); // Sorted by key.
        for (File file : dir.listFiles()) {
            String contents = new Scanner(file).useDelimiter("\\e").next();
            map.put(file.getName(), contents);
        }
        return map;
    }

    private static File getExpectedDir(String suiteName) {
        return new File(new File(BASE_DIR, suiteName), "expected");
    }

    public static File getInputDir(String suiteName) {
        return new File(new File(BASE_DIR, suiteName), "input");
    }
}
