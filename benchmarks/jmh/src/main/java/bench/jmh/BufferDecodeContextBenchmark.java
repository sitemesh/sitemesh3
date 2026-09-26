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
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;
import org.sitemesh.webapp.contentfilter.io.Buffer;
import org.sitemesh.webapp.contentfilter.io.TextEncoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Puts the byte-path decode in context, on the same pages as
 * {@link BufferWriteDecodeBenchmark}:
 *
 * <ul>
 *   <li>{@code toCharBuffer} — decoding an already-filled stream-based
 *       {@link Buffer}, i.e. what each {@code getBuffer()} call costs.</li>
 *   <li>{@code decodeFloor} — {@link TextEncoder#encode} over bytes that are
 *       already contiguous: the decode work itself, with no buffering
 *       overhead. The gap between this and {@code toCharBuffer} is the most
 *       any rewrite of the buffer's decode path could recover.</li>
 *   <li>{@code build} — {@link TagBasedContentProcessor#build} with the
 *       default bundles (core HTML + decorator rules) over the decoded text:
 *       the parse every buffered response goes through next.</li>
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 3, jvmArgsAppend = {"-Xms1g", "-Xmx1g"})
public class BufferDecodeContextBenchmark {

    @Param({"1024", "16384", "262144", "2097152"})
    public int size;

    @Param({"UTF8_ASCII", "UTF8_MULTIBYTE", "ISO_8859_1"})
    public HtmlCorpus.Kind kind;

    private byte[] page;
    private String encoding;
    private Buffer filled;
    private CharBuffer decoded;
    private ContentProcessor processor;
    private SiteMeshContext context;

    @Setup
    public void setUp() throws IOException {
        page = HtmlCorpus.page(kind, size);
        encoding = kind.charset.name();

        filled = new Buffer(encoding);
        filled.getOutputStream().write(page, 0, page.length);
        decoded = filled.toCharBuffer();
        if (!decoded.toString().equals(new String(page, kind.charset))) {
            throw new IllegalStateException("Buffer decoded the page incorrectly");
        }

        processor = new TagBasedContentProcessor(new CoreHtmlTagRuleBundle(), new DecoratorTagRuleBundle());
        context = new SiteMeshContext() {
            public String getPath() {
                return "/bench.html";
            }

            public Content decorate(String decoratorName, Content content) {
                return null;
            }

            public Content getContentToMerge() {
                return null;
            }

            public ContentProcessor getContentProcessor() {
                return processor;
            }
        };
    }

    @Benchmark
    public CharBuffer toCharBuffer() throws IOException {
        return filled.toCharBuffer();
    }

    @Benchmark
    public CharBuffer decodeFloor() throws IOException {
        return TextEncoder.encode(ByteBuffer.wrap(page), encoding);
    }

    @Benchmark
    public Content build() throws IOException {
        return processor.build(decoded, context);
    }
}
