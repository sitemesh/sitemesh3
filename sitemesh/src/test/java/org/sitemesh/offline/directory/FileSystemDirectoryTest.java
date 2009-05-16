package org.sitemesh.offline.directory;

import static org.sitemesh.TestUtil.createTempDir;
import static org.sitemesh.TestUtil.delete;

import java.io.File;
import java.nio.charset.Charset;
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
    protected Directory createDirectory(Charset encoding) {
        return new FileSystemDirectory(createTempDirForTest(), encoding);
    }

    private File createTempDirForTest() {
        File tempDir = createTempDir();
        tempDirs.add(tempDir);
        return tempDir;
    }

    @Override
    protected void tearDown() throws Exception {
        for (File tempDir : tempDirs) {
            delete(tempDir);
        }
        super.tearDown();
    }

}
