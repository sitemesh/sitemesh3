package org.sitemesh.offline.directory;

import static org.sitemesh.TestUtil.createTempDir;
import static org.sitemesh.TestUtil.delete;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.CharBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 * This test inherits tests from DirectoryTest and applies them
 * to FileSystemDirectory.
 *
 * @see DirectoryTest
 * @author Joe Walnes
 */
public class FileSystemDirectoryTest extends DirectoryTest {

    /**
     * List of temporary directories created during test, so they can
     * be deleted at tearDown.
     */
    private List<File> tempDirs = new LinkedList<File>();

    @Override
    protected FileSystemDirectory createDirectory(Charset encoding) {
        return new FileSystemDirectory(createTempDirForTest(), encoding);
    }

    private File createTempDirForTest() {
        File tempDir = createTempDir();
        tempDirs.add(tempDir);
        return tempDir;
    }

    public void testDoesNotWriteFileIfContentsNotModified() throws Exception {
        FileSystemDirectory directory = createDirectory(UTF8);

        // Save some files.
        directory.save("file1.txt", CharBuffer.wrap("original text 1"));
        directory.save("file2.txt", CharBuffer.wrap("original text 2"));

        // Find out the last modified time on disk.
        long lastModifiedOriginal1 = directory.getFileByPath("file1.txt").lastModified();
        long lastModifiedOriginal2 = directory.getFileByPath("file2.txt").lastModified();

        // URRRRGG! File.lastModified() is only accurate to the nearest second, so we need
        // to slow the test down for this test to be useful.
        Thread.sleep(1000);

        // Overwrite the files.
        directory.save("file1.txt", CharBuffer.wrap("new text 1"));
        directory.save("file2.txt", CharBuffer.wrap("original text 2")); // Should not require update.

        // Get new last modified time.
        long lastModifiedNew1 = directory.getFileByPath("file1.txt").lastModified();
        long lastModifiedNew2 = directory.getFileByPath("file2.txt").lastModified();

        // Check only file1.txt was overwritten.
        assertTrue("file1.txt should have been updated", lastModifiedNew1 > lastModifiedOriginal1);
        assertTrue("file2.txt should NOT have been updated", lastModifiedNew2 == lastModifiedOriginal2);
    }

    @Override
    protected void tearDown() throws Exception {
        for (File tempDir : tempDirs) {
            delete(tempDir);
        }
        super.tearDown();
    }

}
