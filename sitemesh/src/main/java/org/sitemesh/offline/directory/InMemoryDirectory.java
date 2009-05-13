package org.sitemesh.offline.directory;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Simple {@link Directory} implementation that stores contents in memory.
 *
 * <p>Can be useful for testing or providing an intermediate input/output for
 * processing.</p>
 *
 * @author Joe Walnes
 */
public class InMemoryDirectory implements Directory {

    // TreeMap to preserve file name ordering.
    private final Map<String, String> files = new TreeMap<String, String>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public CharBuffer load(String path) throws IOException {
        String content;
        lock.readLock().lock();
        try {
            content = files.get(path);
        } finally {
            lock.readLock().unlock();
        }
        if (content == null) {
            throw new IOException("Not found: " + path);
        } else {
            return CharBuffer.wrap(content);
        }
    }

    @Override
    public void save(String path, CharBuffer contents) throws IOException {
        lock.writeLock().lock();
        try {
            files.put(path, contents.toString());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<String> listAllFilePaths() throws IOException {
        lock.readLock().lock();
        try {
            return new ArrayList<String>(files.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }
}
