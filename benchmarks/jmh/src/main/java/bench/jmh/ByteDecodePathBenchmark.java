package bench.jmh;

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
import org.sitemesh.webapp.contentfilter.io.ByteBufferBuilder;
import org.sitemesh.webapp.contentfilter.io.TextEncoder;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Paired comparison, in one JVM, of the two ways to decode the same 8 KB
 * blocks: {@code toCharBuffer()} on a filled {@link Buffer} (whatever the jar
 * under test does) against gathering the blocks with
 * {@link ByteBufferBuilder#toByteBuffer()} and decoding the copy with
 * {@link TextEncoder#encode} — exactly what {@code toCharBuffer()} did before
 * it decoded in place. Many short forks spread both over the same stretch of
 * time, so load from elsewhere on the machine hits them alike.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 4, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 4, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(value = 6, jvmArgsAppend = {"-Xms1g", "-Xmx1g"})
public class ByteDecodePathBenchmark {

    @Param({"16384", "262144"})
    public int size;

    @Param({"UTF8_ASCII", "UTF8_SPARSE", "UTF8_MULTIBYTE", "ISO_8859_1"})
    public HtmlCorpus.Kind kind;

    private Buffer buffer;
    private ByteBufferBuilder builder;
    private String encoding;

    @Setup
    public void setUp() throws IOException {
        byte[] page = HtmlCorpus.page(kind, size);
        encoding = kind.charset.name();
        buffer = new Buffer(encoding);
        buffer.getOutputStream().write(page, 0, page.length);
        builder = new ByteBufferBuilder();
        builder.write(page, 0, page.length);
        if (!inPlace().toString().equals(gatherThenDecode().toString())) {
            throw new IllegalStateException("decode paths disagree");
        }
    }

    @Benchmark
    public CharBuffer inPlace() throws IOException {
        return buffer.toCharBuffer();
    }

    @Benchmark
    public CharBuffer gatherThenDecode() throws IOException {
        return TextEncoder.encode(builder.toByteBuffer(), encoding);
    }
}
