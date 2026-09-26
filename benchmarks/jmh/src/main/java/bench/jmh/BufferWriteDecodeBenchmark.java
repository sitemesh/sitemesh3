package bench.jmh;

import jakarta.servlet.ServletOutputStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.sitemesh.webapp.contentfilter.io.Buffer;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.concurrent.TimeUnit;

/**
 * The whole byte-path cycle a buffered response goes through when the
 * application (or the container's static-file servlet) writes through
 * {@code getOutputStream()}: a fresh {@link Buffer}, the writes, then the
 * {@link Buffer#toCharBuffer()} decode that hands the text to the parser.
 *
 * <p>Write patterns mirror what the containers were observed doing:
 * {@code SINGLE} is Tomcat's DefaultServlet writing a cached static file in
 * one call, {@code CHUNK_4K} is Jetty 12's ResourceServlet copy loop, and
 * {@code SMALL_128} stands in for application code writing bytes piecemeal
 * (multibyte characters routinely straddle those writes).</p>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 3, jvmArgsAppend = {"-Xms1g", "-Xmx1g"})
public class BufferWriteDecodeBenchmark {

    public enum WritePattern {
        SINGLE(Integer.MAX_VALUE), CHUNK_4K(4096), SMALL_128(128);

        final int chunk;

        WritePattern(int chunk) {
            this.chunk = chunk;
        }
    }

    @Param({"1024", "16384", "262144", "2097152"})
    public int size;

    @Param({"UTF8_ASCII", "UTF8_MULTIBYTE", "ISO_8859_1"})
    public HtmlCorpus.Kind kind;

    @Param({"SINGLE", "CHUNK_4K", "SMALL_128"})
    public WritePattern writes;

    private byte[] page;
    private String encoding;
    private int chunk;

    @Setup
    public void setUp() {
        page = HtmlCorpus.page(kind, size);
        encoding = kind.charset.name();
        chunk = Math.min(writes.chunk, page.length);
    }

    @Benchmark
    public CharBuffer writeThenToCharBuffer() throws IOException {
        Buffer buffer = new Buffer(encoding);
        ServletOutputStream out = buffer.getOutputStream();
        byte[] bytes = page;
        int chunk = this.chunk;
        for (int off = 0; off < bytes.length; off += chunk) {
            out.write(bytes, off, Math.min(chunk, bytes.length - off));
        }
        return buffer.toCharBuffer();
    }
}
