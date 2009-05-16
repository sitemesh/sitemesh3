package org.sitemesh.offline.directory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple {@link Directory} implementation that stores contents in memory.
 *
 * <p>Can be useful for testing or providing an intermediate input/output for
 * processing.</p>
 *
 * @author Joe Walnes
 */
public class InMemoryDirectory implements Directory {

    private final Charset encoding;
    private final Map<String, ByteBuffer> files = new ConcurrentHashMap<String, ByteBuffer>();

    public InMemoryDirectory() {
        this(Charset.defaultCharset());
    }

    public InMemoryDirectory(Charset encoding) {
        this.encoding = encoding;
    }

    @Override
    public CharBuffer load(String path) throws IOException {
        return encoding.newDecoder().decode(getDataByPath(path));
    }

    @Override
    public void save(String path, CharBuffer contents) throws IOException {
        files.put(path, encoding.newEncoder().encode(contents));
    }

    @Override
    public List<String> listAllFilePaths() throws IOException {
        return new ArrayList<String>(files.keySet());
    }

    @Override
    public void load(String path, WritableByteChannel channelToWriteTo) throws IOException {
        channelToWriteTo.write(getDataByPath(path));
    }

    @Override
    public void save(String path, ReadableByteChannel channelToReadFrom, int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        channelToReadFrom.read(buffer);
        buffer.flip();
        files.put(path, buffer);
    }

    @Override
    public void copy(String path, Directory destinationDirectory, String destionationPath) throws IOException {
        final ByteBuffer sourceData = getDataByPath(path);
        destinationDirectory.save(destionationPath, new ReadableByteChannel() {
            @Override
            public int read(ByteBuffer dst) throws IOException {
                ByteBuffer buffer = dst.put(sourceData);
                return buffer.position();
            }

            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public void close() throws IOException {
            }
        }, sourceData.limit());
    }

    private ByteBuffer getDataByPath(String path) throws IOException {
        ByteBuffer data = files.get(path);
        if (data == null) {
            throw new IOException("Not found: " + path);
        }
        return data.duplicate();
    }

}
