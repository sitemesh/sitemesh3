package bench.jmh;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Deterministic HTML pages of a requested encoded size, shaped like a real
 * content page (head with title/meta/link/script, a body of repeated
 * sections with headings, paragraphs, links and lists) so the tag processor
 * sees a realistic tag density.
 */
public final class HtmlCorpus {

    /** The text flavours the byte path has to decode. */
    public enum Kind {
        /** UTF-8, every character ASCII: the common case, and the decoder's fast path. */
        UTF8_ASCII(StandardCharsets.UTF_8, "en",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                "Ut enim ad minim veniam"),
        /**
         * UTF-8, mostly ASCII with a few multibyte characters per paragraph
         * (typographic quotes and dashes, accented Latin letters): English or
         * Western European copy saved as UTF-8.
         */
        UTF8_SPARSE(StandardCharsets.UTF_8, "en",
                "It’s a “smart” page — the café serves crème brûlée; "
                        + "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                "Ut enim ad minim veniam"),
        /** UTF-8 dominated by 2-, 3- and 4-byte sequences (Cyrillic, CJK, emoji). */
        UTF8_MULTIBYTE(StandardCharsets.UTF_8, "ja",
                "日本語のテキストは三バイトで符号化されます。Русский текст занимает два байта на символ. 中文内容也很常见。🚀✨🎉 café",
                "項目の説明 — описание"),
        /** ISO-8859-1 Western European text with accented letters (all single bytes). */
        ISO_8859_1(StandardCharsets.ISO_8859_1, "de",
                "Größere Übersicht für Café und Crème brûlée: naïve Façade, señor años, àéîõü ÀÉÎÕÜ ß ç ñ.",
                "Straße Äpfel Öl");

        final Charset charset;
        final String lang;
        final String paragraph;
        final String item;

        Kind(Charset charset, String lang, String paragraph, String item) {
            this.charset = charset;
            this.lang = lang;
            this.paragraph = paragraph;
            this.item = item;
        }
    }

    private HtmlCorpus() {
    }

    /**
     * @return a page of exactly {@code targetBytes} bytes in {@code kind}'s
     *         charset (never splitting a character), padded with an ASCII
     *         comment or spaces to hit the size exactly.
     */
    static byte[] page(Kind kind, int targetBytes) {
        String head = "<!DOCTYPE html>\n<html lang=\"" + kind.lang + "\">\n<head>\n"
                + "  <meta charset=\"" + kind.charset.name() + "\">\n"
                + "  <title>Benchmark page</title>\n"
                + "  <meta name=\"description\" content=\"A realistic page used to benchmark buffering\">\n"
                + "  <link rel=\"stylesheet\" href=\"/css/site.css\">\n"
                + "  <script src=\"/js/app.js\"></script>\n"
                + "</head>\n<body class=\"page\" onload=\"init()\">\n";
        String tail = "</body>\n</html>\n";

        StringBuilder page = new StringBuilder(head);
        int used = bytes(head, kind) + bytes(tail, kind);
        for (int i = 0; ; i++) {
            String section = "<div class=\"row\" id=\"item-" + i + "\">\n"
                    + "  <h2>" + kind.item + " " + i + "</h2>\n"
                    + "  <p>" + kind.paragraph + " <a href=\"/items/" + i + "\">" + kind.item + "</a></p>\n"
                    + "  <ul><li>" + kind.item + "</li><li><em>" + i + "</em></li></ul>\n"
                    + "</div>\n";
            int size = bytes(section, kind);
            if (used + size > targetBytes) {
                break;
            }
            page.append(section);
            used += size;
        }
        int gap = targetBytes - used;
        if (gap >= 8) {
            page.append("<!--").append(" ".repeat(gap - 8)).append("-->\n");
        } else {
            page.append(" ".repeat(gap));
        }
        page.append(tail);

        byte[] encoded = page.toString().getBytes(kind.charset);
        if (encoded.length != targetBytes) {
            throw new IllegalStateException("Generated " + encoded.length + " bytes, wanted " + targetBytes);
        }
        return encoded;
    }

    private static int bytes(String s, Kind kind) {
        return s.getBytes(kind.charset).length;
    }
}
