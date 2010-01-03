package org.sitemesh.ant;

import junit.framework.Test;
import junit.framework.TestSuite;
import static junit.framework.Assert.assertEquals;
import org.apache.tools.ant.BuildFileTest;
import org.apache.tools.ant.util.FileUtils;
import org.sitemesh.TestUtil;

import java.io.*;

/**
 * Ant target test suite.
 *
 * It looks for all subdirectories of testproject/expected, treating each directory as a test.
 * For each of these, it:
 * - runs the ant target: ant [testname]
 * - asserts that the directory tree of out/[testname] is identical to expected/[testname]
 * - runs the ant target: ant clean
 *
 * Steps for creating a new test
 * - All files live in the testproject.
 * - Create a target in test-build.xml called 'mytest' (use another name!).
 * - Perform necessary steps in this test to process files and output to the directory out/mytest.
 * - Create a directory a directory structure called expected/mytest that contains the files you expect to see in out/mytest.
 * - Run this suite. You should not need to write any Java code.
 *
 * @author Richard L. Burton III - SmartCode LLC
 * @author Joe Walnes
 */
public class SiteMeshTaskTestSuite {

    public static Test suite() throws FileNotFoundException {
        final File baseDir = TestUtil.findDir("sitemesh/src/test/java/org/sitemesh/ant/testproject");

        final FileFilter fileFilter = new FileFilter() {
            public boolean accept(File file) {
                return !file.getName().startsWith(".");
            }
        };

        TestSuite suite = new TestSuite(SiteMeshTaskTestSuite.class.getName());
        final String buildFile = new File(baseDir, "test-build.xml").getAbsolutePath();
        for (final File expectedDir : new File(baseDir, "expected").listFiles(fileFilter)) {
            final String target = expectedDir.getName();
            final File actualDir = new File(new File(baseDir, "out"), expectedDir.getName());
            suite.addTest(new BuildFileTest(target) {
                @Override
                protected void runTest() throws Throwable {
                    configureProject(buildFile);
                    executeTarget(target);
                    try {
                        assertDirectoriesAreSame(fileFilter, expectedDir, actualDir);
                    } finally {
                        executeTarget("clean");
                    }
                }
            });
        }
        return suite;
    }

    private static void assertDirectoriesAreSame(FileFilter fileFilter, File expectedDir, File actualDir) throws IOException {
        // Dump the entire contents of each dir to a string and assert that they match.
        // Crude, but makes it easy to diagnose differences.
        StringBuilder expectedContents = new StringBuilder();
        StringBuilder actualContents = new StringBuilder();
        dumpDirectory(fileFilter, "", expectedDir, expectedContents);
        dumpDirectory(fileFilter, "", actualDir, actualContents);
        assertEquals("Directory contents differ between:\n    " + expectedDir.getAbsolutePath() +
                "\nand:\n    " + actualDir.getAbsolutePath(),
                expectedContents.toString(), actualContents.toString());
    }

    private static void dumpDirectory(FileFilter fileFilter, String path, File currentDir, StringBuilder out) throws IOException {
        for (File file : currentDir.listFiles(fileFilter)) {
            if (file.isDirectory()) {
                dumpDirectory(fileFilter, path + "/" + file.getName(), file, out);
            } else {
                out.append("=== File: ").append(path).append("/").append(file.getName()).append(" ===\n");
                Reader reader = new BufferedReader(new FileReader(file));
                try {
                    out.append(FileUtils.readFully(reader).trim());
                } finally {
                    reader.close();
                }
                out.append('\n');
            }
        }
    }

}