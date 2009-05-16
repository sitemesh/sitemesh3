package org.sitemesh.offline.directory;

import java.nio.charset.Charset;

/**
 * This test inherits tests from DirectoryTest and applies them
 * to InMemoryDirectory.
 *
 * @see DirectoryTest
 * @author Joe Walnes
 */
public class InMemoryDirectoryTest extends DirectoryTest {

    @Override
    protected Directory createDirectory(Charset encoding) {
        return new InMemoryDirectory(encoding);
    }
}
