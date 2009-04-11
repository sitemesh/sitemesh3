package com.opensymphony.sitemesh3.microbenchmark.contentprocessor;

import com.opensymphony.sitemesh3.ContentProcessor;
import com.opensymphony.sitemesh3.webapp.contentfilter.io.TextEncoder;
import com.sun.japex.JapexDriverBase;
import com.sun.japex.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;

/**
 * Base implementation of a Japex driver for testing the speed of a {@link ContentProcessor}.
 *
 * See src/benchmark/README.txt and https://japex.dev.java.net/.
 *
 * @author Joe Walnes
 */
public abstract class BaseContentProcessorDriver extends JapexDriverBase {

    /**
     * ContentProcessor implementation to be benchmarked.
     */
    private ContentProcessor<?> contentProcessor;

    /**
     * Data used for benchmarking.
     */
    private CharBuffer data;

    /**
     * Factory method - subclasses should implement this to return a suitable
     * implementation.
     */
    protected abstract ContentProcessor<?> createProcessor();

    /**
     * Preparation phase: Load input data into CharBuffer.
     */
    @Override
    public void prepare(TestCase testCase) {
        try {
            contentProcessor = createProcessor();
            data = loadFile(new File(testCase.getParam("japex.inputFile")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Run phase: Build content from data. This method is called many times.
     */
    @Override
    public void run(TestCase testCase) {
        try {
            contentProcessor.build(data, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CharBuffer loadFile(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        try {
            FileChannel channel = inputStream.getChannel();
            ByteBuffer bytes = ByteBuffer.allocate((int) channel.size());
            channel.read(bytes);
            return TextEncoder.encode(bytes, null);
        } finally {
            inputStream.close();
        }
    }

}
