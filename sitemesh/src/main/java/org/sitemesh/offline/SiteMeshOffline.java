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

import org.sitemesh.DecoratorSelector;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.offline.directory.Directory;

import java.io.IOException;
import java.nio.CharBuffer;

/**
 * Tools for applying decorators to offline web pages (static content).
 *
 * <h2>Example</h2>
 * <pre>
 * Directory sourceDir = new FileSystemDirectory("src/html");
 * Directory dirDir = new FileSystemDirectory("build/html");
 * ContentProcessor contentProcessor = // your ContentProcesor
 * DecoratorSelector decoratorSelector = // your DecoratorSelector
 *
 * SiteMeshOffline siteMeshOffline = new SiteMeshOffline(
 *     contentProcessor, decoratorSelector, sourceDir, destinationDir);
 *
 * siteMeshOffline.process("somecontent.html");
 * siteMeshOffline.process("morecontent.html");
 * </pre>
 *
 * @author Joe Walnes
 */
public class SiteMeshOffline {

    private final ContentProcessor contentProcessor;
    private final DecoratorSelector<OfflineContext> decoratorSelector;
    private final Directory sourceDirectory;
    private final Directory destinationDirectory;

    public SiteMeshOffline(ContentProcessor contentProcessor,
                                    DecoratorSelector<OfflineContext> decoratorSelector,
                                    Directory sourceDirectory,
                                    Directory destinationDirectory) {
        this.contentProcessor = contentProcessor;
        this.decoratorSelector = decoratorSelector;
        this.sourceDirectory = sourceDirectory;
        this.destinationDirectory = destinationDirectory;
    }

    /**
     * Directory the generator reads the source (undecorated) files from.
     */
    public Directory getSourceDirectory() {
        return sourceDirectory;
    }

    /**
     * Directory the generator writes the destination (decorated) files from.
     */
    public Directory getDestinationDirectory() {
        return destinationDirectory;
    }

    /**
     * Process a file (loaded from source directory), applying decorators and returning
     * the result as a CharBuffer.
     */
    public void process(String path) throws IOException {
        CharBuffer input = sourceDirectory.load(path);
        CharBuffer output = processContent(path, input);
        destinationDirectory.save(path, output);
    }

    /**
     * Process content passed in, applying decorators and returning
     * the result as a CharBuffer.
     *
     * The path is required as the DecoratorSelector may use this to determine which
     * decorators should be applied.
     */
    public CharBuffer processContent(String path, CharBuffer original) throws IOException {
        OfflineContext context = new OfflineContext(contentProcessor, sourceDirectory, path);

        // Process data into a Content object.
        Content content = contentProcessor.build(original, context);
        if (content == null) {
            return original;
        }

        // Apply all decorators.
        String[] decoratorPaths = decoratorSelector.selectDecoratorPaths(content, context);
        for (String decoratorPath : decoratorPaths) {
            content = context.decorate(decoratorPath, content);
        }

        // Convert Content back to data and return.
        if (content == null) {
            return original;
        } else {
            // TODO: content.getData().getNonNullValue() does not work here - figure out why.
            StringBuilder out = new StringBuilder();
            content.getData().writeValueTo(out);
            return CharBuffer.wrap(out.toString());
        }
    }

}
