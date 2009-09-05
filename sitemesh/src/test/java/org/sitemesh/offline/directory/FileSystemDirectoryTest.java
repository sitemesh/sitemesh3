package org.sitemesh.offline.directory;

import static org.sitemesh.TestUtil.createTempDir;
import static org.sitemesh.TestUtil.delete;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

        File file1 = directory.getFileByPath("file1.txt");
        File file2 = directory.getFileByPath("file2.txt");

        // Save some files.
        directory.save("file1.txt", CharBuffer.wrap("original text 1"));
        directory.save("file2.txt", CharBuffer.wrap("original text 2"));

        // Find out the last modified time on disk.
        long lastModifiedOriginal1 = file1.lastModified();
        long lastModifiedOriginal2 = file2.lastModified();

        // URRRRGG! File.lastModified() is only accurate to the nearest second, so we need
        // to slow the test down for this test to be useful.
        Thread.sleep(1000);

        // Overwrite the files.
        directory.save("file1.txt", CharBuffer.wrap("new text 1"));
        directory.save("file2.txt", CharBuffer.wrap("original text 2")); // Should not require update.

        // Get new last modified time.
        long lastModifiedNew1 = file1.lastModified();
        long lastModifiedNew2 = file2.lastModified();

        // Check only file1.txt was overwritten.
        assertTrue("file1.txt should have been updated", lastModifiedNew1 > lastModifiedOriginal1);
        assertTrue("file2.txt should NOT have been updated", lastModifiedNew2 == lastModifiedOriginal2);

        // Check contents.
        assertEquals(CharBuffer.wrap("new text 1"), directory.load("file1.txt"));
        assertEquals(CharBuffer.wrap("original text 2"), directory.load("file2.txt"));
    }

    public void testDoesNotCopyBinaryFileIfContentsNotModified() throws Exception {
        FileSystemDirectory directory = createDirectory(UTF8);

        File fileOriginal1 = directory.getFileByPath("file1-orig.txt");
        File fileOriginal2 = directory.getFileByPath("file2-orig.txt");
        File fileNew1 = directory.getFileByPath("file1-new.txt");
        File fileNew2 = directory.getFileByPath("file2-new.txt");
        File fileOut1 = directory.getFileByPath("file1-out.txt");
        File fileOut2 = directory.getFileByPath("file2-out.txt");

        // Save some files.
        writeBinary(fileOriginal1, "original1".getBytes());
        writeBinary(fileOriginal2, "original2".getBytes());
        writeBinary(fileNew1, "new1".getBytes());
        writeBinary(fileNew2, "original2".getBytes()); // Should not require update.
        directory.copy("file1-orig.txt", directory, "file1-out.txt");
        directory.copy("file2-orig.txt", directory, "file2-out.txt");

        // Find out the last modified time on disk.
        long lastModifiedOriginal1 = fileOut1.lastModified();
        long lastModifiedOriginal2 = fileOut2.lastModified();

        // URRRRGG! File.lastModified() is only accurate to the nearest second, so we need
        // to slow the test down for this test to be useful.
        Thread.sleep(1000);

        // Overwrite the files
        directory.copy("file1-new.txt", directory, "file1-out.txt");
        directory.copy("file2-new.txt", directory, "file2-out.txt"); // Should require no change as contents are same.

        // Get new last modified time.
        long lastModifiedNew1 = fileOut1.lastModified();
        long lastModifiedNew2 = fileOut2.lastModified();

        // Check only file1.txt was overwritten.
        assertTrue("file1-out.txt should have been updated", lastModifiedNew1 > lastModifiedOriginal1);
        assertTrue("file2-out.txt should NOT have been updated", lastModifiedNew2 == lastModifiedOriginal2);
    }

    @Override
    protected void tearDown() throws Exception {
        for (File tempDir : tempDirs) {
            delete(tempDir);
        }
        super.tearDown();
    }

    private void writeBinary(File file, byte... bytes) throws IOException {
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
    }

}
