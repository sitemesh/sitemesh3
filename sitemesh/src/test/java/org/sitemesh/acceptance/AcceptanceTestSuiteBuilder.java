/*
 *    Copyright 2009-2023 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.sitemesh.acceptance;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.sitemesh.TestUtil;
import org.sitemesh.offline.SiteMeshOffline;
import org.sitemesh.offline.directory.Directory;
import org.sitemesh.webapp.WebEnvironment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

/**
 * @author Joe Walnes
 */
public class AcceptanceTestSuiteBuilder {

    public static TestSuite buildWebAppAndOfflineSuite(String suiteName,
                                                       WebEnvironment webEnvironment,
                                                       SiteMeshOffline offline) throws IOException {
        TestSuite suite = new TestSuite(suiteName);
        suite.addTest(buildWebAppSuite(suiteName, webEnvironment));
        suite.addTest(buildOfflineSuite(suiteName, offline));
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

    public static TestSuite buildOfflineSuite(String suiteName, final SiteMeshOffline offline) throws IOException {
        TestSuite suite = new TestSuite(suiteName + "-offline");
        final Directory destinationDirectory = offline.getDestinationDirectory();

        File expectedDir = getExpectedDir(suiteName);
        final Map<String, String> expected = AcceptanceTestSuiteBuilder.readFiles(expectedDir);
        for (final String fileName : expected.keySet()) {
            suite.addTest(new TestCase(fileName) {
                @Override
                protected void runTest() throws Throwable {
                    offline.process(fileName);
                    CharBuffer result = destinationDirectory.load(fileName);
                    assertEquals(reduceWhitespace(expected.get(fileName)),
                            reduceWhitespace(result.toString()));
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

    private static File getExpectedDir(String suiteName) throws FileNotFoundException {
        return new File(new File(TestUtil.findDir("sitemesh/src/test/java/org/sitemesh/acceptance"), suiteName), "expected");
    }

    public static File getInputDir(String suiteName) throws FileNotFoundException {
        return new File(new File(TestUtil.findDir("sitemesh/src/test/java/org/sitemesh/acceptance"), suiteName), "input");
    }
}
