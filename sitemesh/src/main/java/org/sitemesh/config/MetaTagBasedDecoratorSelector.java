package org.sitemesh.config;

import org.sitemesh.DecoratorSelector;
import org.sitemesh.SiteMeshContext;
import org.sitemesh.content.Content;

import java.io.IOException;

/**
 * {@link DecoratorSelector} implementation that selects a decorator based on the
 * meta tag in the page header
 * 
 * <h2>Example</h2>
 * 
 * <pre>
 * &lt;meta name="decorator" content="/my-decorator,/my-other-decorator"&gt;
 * </pre>
 * 
 * @author Joe Walnes
 * @see PathMapper
 */
public class MetaTagBasedDecoratorSelector<C extends SiteMeshContext> extends PathBasedDecoratorSelector<C>{
    private String metaTagName = "decorator";

    public MetaTagBasedDecoratorSelector setMetaTagName(String metaTagName) {
        this.metaTagName = metaTagName;
        return this;
    }

    public MetaTagBasedDecoratorSelector put(String contentPath, String... decoratorPaths) {
        super.put(contentPath, decoratorPaths);
        return this;
    }

    public String[] selectDecoratorPaths(Content content, C siteMeshContext) throws IOException {
        // Fetch <meta name=decorator> value.
        // The default HTML processor already extracts these into 'meta.NAME' properties.
        String decorator = content.getExtractedProperties()
                .getChild("meta")
                .getChild(metaTagName)
                .getValue();

        if (decorator != null) {
            // If present, return it. 
            // Multiple chained decorators can be specified using commas.
            return convertPaths(decorator.split(","));
        }

        // Otherwise, fallback to the standard configuration
        return super.selectDecoratorPaths(content, siteMeshContext);
    }
}
