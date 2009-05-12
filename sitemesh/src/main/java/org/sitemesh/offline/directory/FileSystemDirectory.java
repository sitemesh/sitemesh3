package org.sitemesh.offline.directory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

/**
 * Disk backed {@link Directory} implementation that uses java.io.File.
 *
 * @author Joe Walnes
 * @see Directory
 */
public class FileSystemDirectory implements Directory {

    private final File rootDir;
    private final Charset encoding;

    public FileSystemDirectory(File rootDir, Charset encoding) {
        this.rootDir = rootDir;
        this.encoding = encoding;
    }

    public FileSystemDirectory(String rootDir, Charset encoding) {
        this(new File(rootDir), encoding);
    }

    @Override
    public CharBuffer load(String path) throws IOException {
        File file = new File(rootDir, path);
        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
        try {
            CharBuffer contents = CharBuffer.allocate((int) file.length());
            reader.read(contents);
            contents.flip();
            return contents;
        } finally {
            reader.close();
        }
    }

    @Override
    public void save(String path, CharBuffer contents) throws IOException {
        File file = new File(rootDir, path);
        file.getParentFile().mkdirs();
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), encoding));
        try {
            out.append(contents);
        } finally {
            out.close();
        }
    }

    @Override
    public List<String> listAllFilePaths() throws IOException {
        List<String> result = new LinkedList<String>();
        listAllFilePaths("", result);
        return result;
    }

    private void listAllFilePaths(String path, List<String> result) {
        File file = new File(rootDir, path);
        if (file.isDirectory()) {
            for (String child : file.list()) {
                String childPath = (path.length() == 0)
                        ? child : path + "/" + child;
                listAllFilePaths(childPath, result);
            }
        }
        if (file.isFile() && file.canRead()) {
            result.add(path);
        }
    }

}
