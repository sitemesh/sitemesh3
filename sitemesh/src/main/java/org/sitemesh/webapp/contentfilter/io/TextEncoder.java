/*
 *    Copyright 2009-2023 SiteMesh authors.
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

/**
 * Converts text as bytes to chars using specified encoding.
 *
 * @author Scott Farquhar
 * @author Hani Suleiman
 * @author Joe Walnes
 */
public class TextEncoder {

    private static final String DEFAULT_ENCODING = System.getProperty("file.encoding");

    public static CharBuffer encode(ByteBuffer data, String encoding) throws IOException {
        CharsetDecoder decoder = createDecoder(encoding);
        int encodedLength = (int) (decoder.maxCharsPerByte() * data.limit());
        CharBuffer charBuffer = CharBuffer.allocate(encodedLength);
        CoderResult coderResult = decoder.decode(data, charBuffer, true);
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

    private static CharsetDecoder createDecoder(String encoding) throws IOException {
        if (encoding == null) {
            encoding = DEFAULT_ENCODING;
        }
        if (!Charset.isSupported(encoding)) {
            throw new IOException("Unsupported encoding " + encoding);
        }
        Charset charset = Charset.forName(encoding);
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

}