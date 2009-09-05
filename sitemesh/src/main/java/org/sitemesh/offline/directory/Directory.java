package org.sitemesh.offline.directory;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;

/**
 * Abstraction over a directory of files. Allows SiteMeshOffline to be plugged
 * into different sources/destinations.
 *
 * <p>Typically, you'd use {@link FileSystemDirectory} that reads/writes to disk.</p>
 *
 * @author Joe Walnes
 * @see org.sitemesh.offline.SiteMeshOffline
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

    /**
     * Load binary data.
     */
    void load(String path, WritableByteChannel channelToWriteTo) throws IOException;

    /**
     * Save binary data.
     */
    void save(String path, ReadableByteChannel channelToReadFrom, int length) throws IOException;

    /**
     * Copy a file from this directory to another location. Will copy raw bytes so
     * can deal with binary data (e.g. images).
     *
     * @param path                 Path of file to copy (from this Directory)
     * @param destinationDirectory Target directory (may be this)
     * @param destinationPath     Path under target directory.
     */
    void copy(String path, Directory destinationDirectory, String destinationPath) throws IOException;

}
