package org.sitemesh.offline.directory;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * @author Joe Walnes
 */
public class FileSystemDirectoryTest extends TestCase {

    private File tempDir;

    public void testReadsAndWritesFilesToDisk() throws IOException {
        Directory directory = new FileSystemDirectory(tempDir, Charset.forName("UTF-8"));

        assertFalse(new File(tempDir, "/some/file.txt").exists());

        directory.save("some/file.txt", CharBuffer.wrap("Hello world"));
        assertTrue(new File(tempDir, "/some/file.txt").exists());

        assertEquals("Hello world", directory.load("some/file.txt").toString());
    }

    public void testListsAllFilePaths() throws IOException {
        Directory directory = new FileSystemDirectory(tempDir, Charset.forName("UTF-8"));

        directory.save("a.txt", CharBuffer.wrap("Hello world"));
        directory.save("some/file.txt", CharBuffer.wrap("Hello world"));
        directory.save("a/b/c/d/e.txt", CharBuffer.wrap("Hello world"));

        List<String> paths = directory.listAllFilePaths();
        assertEquals(join("a.txt", "some/file.txt", "a/b/c/d/e.txt"), join(paths));
    }

    // --- Test support ---

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tempDir = createTempDir();
    }

    @Override
    protected void tearDown() throws Exception {
        delete(tempDir);
        super.tearDown();
    }

    private File createTempDir() {
        String baseDir = System.getProperty("java.io.tmpdir");
        String newDirName = getClass().getName() + "-" + System.nanoTime();
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

    private void delete(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }
        if (file.exists()) {
            file.delete();
        }
    }

    private String join(String... strings) {
        return join(Arrays.asList(strings));
    }

    private String join(Iterable<String> strings) {
        StringBuilder result = new StringBuilder();
        for (String string : strings) {
            if (result.length() > 0) {
                result.append('|');
            }
            result.append(string);
        }
        return result.toString();
    }
}
