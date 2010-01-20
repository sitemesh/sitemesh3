package org.sitemesh;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

/**
 * General set of helpers used by tests.
 *
 * @author Joe Walnes
 */
public class TestUtil {

    /**
     * Join a set of objects into a single string, (toStrings() delimited by ',').
     */
    public static String join(Object... items) {
        return join(Arrays.asList(items));
    }

    /**
     * Join a set of objects into a single string, (toStrings() delimited by ',').
     */
    public static String join(Iterable<?> items) {
        StringBuilder result = new StringBuilder();
        boolean seenFirst = false;
        for (Object item : items) {
            if (seenFirst) {
                result.append(',');
            } else {
                seenFirst = true;
            }
            result.append(item);
        }
        return result.toString();
    }

    /**
     * Creates a temporary directory. Callers should later call {@link #delete(java.io.File)}
     * to delete it. As an additional precaution, the directory will also be deleted
     * when the JVM shutdowns, but it's better to explicitly delete it before hand
     * in case the JVM does not terminate correctly.
     */
    public static File createTempDir() {
        String baseDir = System.getProperty("java.io.tmpdir");
        String newDirName = TestUtil.class.getName() + "-" + System.nanoTime();
        final File dir = new File(baseDir, newDirName);
        dir.mkdirs();
        Runtime.getRuntime().addShutdownHook(new Thread() { // Just in case test gets interrupted.

            @Override
            public void run() {
                delete(dir);
            }
        });
        return dir;
    }

    /**
     * Delete a file or directory. If the file is a directory it will
     * have its contents deleted (recursively).
     */
    public static void delete(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }
        if (file.exists()) {
            file.delete();
        }
    }

    public static File findDir(String path) throws FileNotFoundException {
        File dir1 = new File(path);
        if (dir1.exists()) {
            return dir1;
        }
        File dir2 = new File(path.replaceFirst("[^/]*/", ""));
        if (dir2.exists()) {
            return dir2;
        }
        throw new FileNotFoundException("Could not find " + dir1.getAbsolutePath() + " or " + dir2.getAbsolutePath());
    }

}
