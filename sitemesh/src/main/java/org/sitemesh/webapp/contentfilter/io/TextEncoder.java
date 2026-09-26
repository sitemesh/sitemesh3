/*
 *    Copyright 2009-2026 SiteMesh authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.sitemesh.webapp.contentfilter.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Converts text as bytes to chars using specified encoding.
 *
 * @author Scott Farquhar
 * @author Hani Suleiman
 * @author Joe Walnes
 */
public class TextEncoder {

    private static final String DEFAULT_ENCODING = System.getProperty("file.encoding");

    /**
     * Decode the given bytes to characters using the specified encoding.
     *
     * @param data The bytes to decode.
     * @param encoding Character encoding to use. If null, the platform default is used.
     * @return The decoded characters.
     * @throws IOException If the encoding is unsupported or decoding fails.
     */
    public static CharBuffer encode(ByteBuffer data, String encoding) throws IOException {
        CharsetDecoder decoder = createDecoder(encoding);
        int encodedLength = capacityFor(decoder, data.limit());
        CharBuffer charBuffer = CharBuffer.allocate(encodedLength);
        return complete(decoder, charBuffer, decoder.decode(data, charBuffer, true));
    }

    /**
     * Decode the bytes held by a {@link ByteBufferBuilder}. Same result, and
     * same exceptions, as {@code encode(data.toByteBuffer(), encoding)}, but
     * decodes the builder's blocks in place rather than first copying them all
     * into one intermediate buffer.
     *
     * @param data The bytes to decode.
     * @param encoding Character encoding to use. If null, the platform default is used.
     * @return The decoded characters.
     * @throws IOException If the encoding is unsupported or decoding fails.
     */
    static CharBuffer encode(ByteBufferBuilder data, String encoding) throws IOException {
        CharsetDecoder decoder = createDecoder(encoding);
        int encodedLength = capacityFor(decoder, data.size());
        CharBuffer charBuffer = CharBuffer.allocate(encodedLength);
        return complete(decoder, charBuffer, data.decodeTo(decoder, charBuffer));
    }

    private static CharBuffer complete(CharsetDecoder decoder, CharBuffer charBuffer, CoderResult coderResult)
            throws IOException {
        if (!coderResult.isUnderflow()) {
            coderResult.throwException();
        }
        coderResult = decoder.flush(charBuffer);
        if (!coderResult.isUnderflow()) {
            coderResult.throwException();
        }
        charBuffer.flip();
        return charBuffer;
    }

    /**
     * The most chars {@code byteCount} bytes can decode to. Computed in double: a float
     * (maxCharsPerByte() * byteCount) cannot represent every int above 2^24, and rounding
     * down left the buffer too small, so decoding e.g. 16,777,217 ASCII bytes overflowed.
     */
    private static int capacityFor(CharsetDecoder decoder, int byteCount) {
        return (int) Math.ceil((double) decoder.maxCharsPerByte() * byteCount);
    }

    private static CharsetDecoder createDecoder(String encoding) throws IOException {
        return charsetFor(encoding).newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    /**
     * The charset bytes are decoded with for the given encoding.
     *
     * @param encoding Character encoding name. If null, the platform default is used.
     * @return The charset.
     * @throws IOException If the encoding is unsupported.
     */
    static Charset charsetFor(String encoding) throws IOException {
        if (encoding == null) {
            encoding = DEFAULT_ENCODING;
        }
        Charset charset;
        // Fast path for the overwhelmingly common case — avoids the two
        // Charset registry lookups (isSupported + forName) on every response.
        if ("UTF-8".equalsIgnoreCase(encoding)) {
            charset = StandardCharsets.UTF_8;
        } else {
            try {
                charset = Charset.forName(encoding);
            } catch (UnsupportedCharsetException | IllegalCharsetNameException e) {
                throw new IOException("Unsupported encoding " + encoding, e);
            }
        }
        return charset;
    }

}