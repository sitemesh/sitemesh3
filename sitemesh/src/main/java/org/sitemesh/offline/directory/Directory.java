package org.sitemesh.offline.directory;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.List;

/**
 * Abstraction over a directory of files. SiteMeshOfflineGenerator to be plugged
 * into different sources/destinations.
 *
 * <p>Typically, you'd use {@link FileSystemDirectory} that reads/writes to disk.</p>
 *
 * @author Joe Walnes
 * @see org.sitemesh.offline.SiteMeshOfflineGenerator
 * @see FileSystemDirectory
 */
public interface Directory {

    /**
     * Load the contents from a file.
     */
    CharBuffer load(String path) throws IOException;

    /**
     * Save the contents to a file, overwriting any existing content.
     */
    void save(String path, CharBuffer contents) throws IOException;

    /**
     * Get a list of all file paths (relative to the Directory). Excludes directories.
     */
    List<String> listAllFilePaths() throws IOException;

}
