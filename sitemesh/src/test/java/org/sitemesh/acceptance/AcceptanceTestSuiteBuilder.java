package org.sitemesh.acceptance;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.sitemesh.offline.SiteMeshOfflineGenerator;
import org.sitemesh.webapp.WebEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * @author Joe Walnes
 */
public class AcceptanceTestSuiteBuilder {

    private static final File BASE_DIR = new File("src/test/java/org/sitemesh/acceptance");

    public static TestSuite buildWebAppAndOfflineSuite(String suiteName,
                                                       WebEnvironment webEnvironment,
                                                       SiteMeshOfflineGenerator offlineGenerator) throws IOException {
        TestSuite suite = new TestSuite(suiteName);
        suite.addTest(buildWebAppSuite(suiteName, webEnvironment));
        suite.addTest(buildOfflineSuite(suiteName, offlineGenerator));
        return suite;
    }

    public static TestSuite buildWebAppSuite(String suiteName, final WebEnvironment webEnvironment) throws IOException {
        TestSuite suite = new TestSuite(suiteName + "-webapp");

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

    public static TestSuite buildOfflineSuite(String suiteName, final SiteMeshOfflineGenerator generator) throws IOException {
        TestSuite suite = new TestSuite(suiteName + "-offline");

        File expectedDir = getExpectedDir(suiteName);
        final Map<String, String> expected = AcceptanceTestSuiteBuilder.readFiles(expectedDir);
        for (final String fileName : expected.keySet()) {
            suite.addTest(new TestCase(fileName) {
                @Override
                protected void runTest() throws Throwable {
                    assertEquals(reduceWhitespace(expected.get(fileName)),
                            reduceWhitespace(generator.process(fileName).toString()));
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
