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
import org.sitemesh.webapp.contentfilter.io.TextEncoder;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Alternatives for turning contiguous UTF-8 bytes into the array-backed,
 * writable CharBuffer {@code Buffer.toCharBuffer()} returns (position 0,
 * capacity = byte count), independent of any SiteMesh change:
 *
 * <ul>
 *   <li>{@code decoder} — {@link TextEncoder#encode}, i.e. a
 *       {@code CharsetDecoder} (what SiteMesh uses).</li>
 *   <li>{@code stringGetChars} — {@code new String(bytes, UTF_8)}, then
 *       {@code getChars} into a byte-count-sized {@code char[]}.</li>
 *   <li>{@code asciiScan} — the cost of checking for any non-ASCII byte,
 *       eight bytes at a time.</li>
 *   <li>{@code hybrid} — that check, then the decoder for pure ASCII and the
 *       String route otherwise.</li>
 * </ul>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 3, jvmArgsAppend = {"-Xms1g", "-Xmx1g"})
public class Utf8DecodeStrategyBenchmark {

    private static final VarHandle LONGS = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());
    private static final long HIGH_BITS = 0x8080808080808080L;

    @Param({"1024", "16384", "262144"})
    public int size;

    @Param({"UTF8_ASCII", "UTF8_SPARSE", "UTF8_MULTIBYTE"})
    public HtmlCorpus.Kind kind;

    private byte[] page;

    @Setup
    public void setUp() throws IOException {
        page = HtmlCorpus.page(kind, size);
        String expected = TextEncoder.encode(ByteBuffer.wrap(page), "UTF-8").toString();
        if (!stringGetChars().toString().equals(expected) || !hybrid().toString().equals(expected)) {
            throw new IllegalStateException("strategies disagree");
        }
    }

    @Benchmark
    public CharBuffer decoder() throws IOException {
        return TextEncoder.encode(ByteBuffer.wrap(page), "UTF-8");
    }

    @Benchmark
    public CharBuffer stringGetChars() {
        return viaString(page);
    }

    @Benchmark
    public boolean asciiScan() {
        return isAscii(page);
    }

    @Benchmark
    public CharBuffer hybrid() throws IOException {
        return isAscii(page) ? TextEncoder.encode(ByteBuffer.wrap(page), "UTF-8") : viaString(page);
    }

    private static CharBuffer viaString(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        char[] chars = new char[bytes.length];
        text.getChars(0, text.length(), chars, 0);
        return CharBuffer.wrap(chars, 0, text.length());
    }

    private static boolean isAscii(byte[] bytes) {
        int i = 0;
        int end = bytes.length - 7;
        for (; i < end; i += 8) {
            if (((long) LONGS.get(bytes, i) & HIGH_BITS) != 0) {
                return false;
            }
        }
        for (; i < bytes.length; i++) {
            if (bytes[i] < 0) {
                return false;
            }
        }
        return true;
    }
}
