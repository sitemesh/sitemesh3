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
 * <h3>Example</h3>
 * <pre>
 * Directory sourceDir = new FileSystemDirectory("/some/path");
 * ContentProcessor contentProcessor = // your ContentProcesor
 * DecoratorSelector decoratorSelector = // your DecoratorSelector
 *
 * SiteMeshOfflineGenerator generator = new SiteMeshOfflineGenerator(
 *     contentProcessor, decoratorSelector, sourceDirectory);
 *
 * print( generator.process("somecontent.html") );
 * print( generator.process("morecontent.html") );
 * </pre>
 *
 * @author Joe Walnes
 */
public class SiteMeshOfflineGenerator {

    private final ContentProcessor contentProcessor;
    private final DecoratorSelector<OfflineContext> decoratorSelector;
    private final Directory source;

    public SiteMeshOfflineGenerator(ContentProcessor contentProcessor,
                                    DecoratorSelector<OfflineContext> decoratorSelector,
                                    Directory sourceDirectory) {
        this.contentProcessor = contentProcessor;
        this.decoratorSelector = decoratorSelector;
        this.source = sourceDirectory;
    }

    /**
     * Process a file (loaded from source directory), applying decorators and returning
     * the result as a CharBuffer.
     */
    public CharBuffer process(String path) throws IOException {
        return process(path, source.load(path));
    }

    /**
     * Process content passed in, applying decorators and returning
     * the result as a CharBuffer.
     *
     * The path is required as the DecoratorSelector may use this to determine which
     * decorators should be applied.
     */
    public CharBuffer process(String path, CharBuffer original) throws IOException {
        OfflineContext context = new OfflineContext(contentProcessor, source, path);

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
