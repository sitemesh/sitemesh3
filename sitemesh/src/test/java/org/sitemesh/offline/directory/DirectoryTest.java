package org.sitemesh.offline.directory;

import junit.framework.TestCase;
import static org.sitemesh.TestUtil.join;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.channels.Channels;
import java.util.Collections;
import java.util.List;

/**
 * Set of common tests that all {@link Directory} implementations
 * should pass.
 *
 * This test should be extended and have the {@link #createDirectory(Charset)}
 * method implemented.
 *
 * Subclasses may also add implementation specific tests.
 *
 * @author Joe Walnes
 */
public abstract class DirectoryTest extends TestCase {

    protected static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * Factory method that should be implemented by subclasses
     * to return the specific Directory implementation to be tested.
     */
    protected abstract Directory createDirectory(Charset encoding);

    public void testSavesAndLoadsCharBuffers() throws IOException {
        Directory directory = createDirectory(UTF8);

        directory.save("some/file.txt", CharBuffer.wrap("Hello world"));
        assertEquals("Hello world", directory.load("some/file.txt").toString());
    }

    public void testThrowsIOExceptionIfLoadingANonExistentFile() throws IOException {
        Directory directory = createDirectory(UTF8);

        try {
            directory.load("non/existent.txt");
            fail("Expected IOException");
        } catch (IOException expected) {
            // good
        }
    }

    public void testOverwritesExistingFile() throws IOException {
        Directory directory = createDirectory(UTF8);

        directory.save("some/file.txt", CharBuffer.wrap("Hello world"));
        directory.save("some/file.txt", CharBuffer.wrap("Yo world"));
        assertEquals("Yo world", directory.load("some/file.txt").toString());
    }

    public void testSavesAndLoadsBinaryChannels() throws IOException {
        Directory directory = createDirectory(UTF8);

        byte[] inBytes = {-128, 0, 127, 5};

        directory.save("some/file", Channels.newChannel(new ByteArrayInputStream(inBytes)), inBytes.length);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        directory.load("some/file", Channels.newChannel(out));
        byte[] outBytes = out.toByteArray();

        assertBytesEqual(inBytes, outBytes);
    }

    public void testCanLoadSameFileMultipleTimes() throws IOException {
        Directory directory = createDirectory(UTF8);

        String someString = "Hello world";
        byte[] someData = {1, 2, 3, 4};
        directory.save("some/file.txt", CharBuffer.wrap(someString));
        directory.save("some/file.dat", Channels.newChannel(new ByteArrayInputStream(someData)), someData.length);

        assertEquals(someString, directory.load("some/file.txt").toString());
        assertEquals(someString, directory.load("some/file.txt").toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        directory.load("some/file.dat", Channels.newChannel(out));
        assertBytesEqual(someData, out.toByteArray());

        out = new ByteArrayOutputStream();
        directory.load("some/file.dat", Channels.newChannel(out));
        assertBytesEqual(someData, out.toByteArray());
    }

    public void testCopiesBinaryDataAcrossDirectories() throws IOException {
        Directory sourceDirectory = createDirectory(UTF8);
        Directory destinationDirectory = createDirectory(UTF8);

        byte[] inBytes = {-128, 0, 127, 5};

        sourceDirectory.save("old", Channels.newChannel(new ByteArrayInputStream(inBytes)), inBytes.length);

        assertTrue(sourceDirectory.listAllFilePaths().contains("old"));
        assertFalse(destinationDirectory.listAllFilePaths().contains("old"));
        assertFalse(destinationDirectory.listAllFilePaths().contains("new"));

        sourceDirectory.copy("old", destinationDirectory, "new");
        assertTrue(sourceDirectory.listAllFilePaths().contains("old"));
        assertFalse(destinationDirectory.listAllFilePaths().contains("old"));
        assertTrue(destinationDirectory.listAllFilePaths().contains("new"));

        destinationDirectory.copy("new", destinationDirectory, "old");
        assertTrue(sourceDirectory.listAllFilePaths().contains("old"));
        assertTrue(destinationDirectory.listAllFilePaths().contains("old"));
        assertTrue(destinationDirectory.listAllFilePaths().contains("new"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        destinationDirectory.load("new", Channels.newChannel(out));
        assertBytesEqual(inBytes, out.toByteArray());
    }

    public void testEncodesCharBuffersUsingGivenEncoding() throws IOException {
        Charset shiftJis = Charset.forName("Shift_JIS");
        Directory directory = createDirectory(shiftJis);

        String string = "\u540d\u524d";
        directory.save("file", CharBuffer.wrap(string));
        assertEquals(string, directory.load("file").toString());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        directory.load("file", Channels.newChannel(out));
        assertBytesEqual(string.getBytes(shiftJis.name()), out.toByteArray());
    }

    public void testListsAllFilePaths() throws IOException {
        Directory directory = createDirectory(UTF8);

        directory.save("a.txt", CharBuffer.wrap("Hello world"));
        directory.save("some/file.txt", CharBuffer.wrap("Hello world"));
        directory.save("a/b/c/d/e.txt", CharBuffer.wrap("Hello world"));
        byte[] someData = {1, 2, 3, 4};
        directory.save("some/file.dat", Channels.newChannel(new ByteArrayInputStream(someData)), someData.length);

        List<String> paths = directory.listAllFilePaths();
        Collections.sort(paths);
        assertEquals(join("a.txt", "a/b/c/d/e.txt", "some/file.dat", "some/file.txt"), join(paths));
    }

    private void assertBytesEqual(byte[] expected, byte[] actual) {
        assertEquals("Byte[] lengths don't match.", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Byte[" + i + "] does not match", expected[i], actual[i]);
        }
    }

}
