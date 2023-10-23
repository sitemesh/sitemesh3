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

package org.sitemesh.offline;

import org.sitemesh.BaseSiteMeshContext;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.offline.directory.Directory;

import java.io.IOException;
import java.io.Writer;

/**
 * SiteMesh {@link org.sitemesh.SiteMeshContext} implementation specifically for use
 * in offline site generation.
 *
 * <p>Reads resources (content, decorators, includes, etc) from the passed in
 * {@link Directory} implementation. This could be backed by a filesystem, memory,
 * or a custom source/target.</p>
 *
 * @author Joe Walnes
 * @see SiteMeshOffline
 * @see org.sitemesh.offline.directory.Directory
 * @see org.sitemesh.SiteMeshContext
 */
public class OfflineContext extends BaseSiteMeshContext {

    private final Directory baseDirectory;
    private final String path;

    public OfflineContext(ContentProcessor contentProcessor, Directory baseDirectory, String path) {
        super(contentProcessor);
        this.baseDirectory = baseDirectory;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    @Override
    protected void decorate(String decoratorPath, Content content, Writer out) throws IOException {
        out.append(baseDirectory.load(decoratorPath));
    }

}
