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

package org.sitemesh.examples.micronaut;

import org.sitemesh.BaseSiteMeshContext;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;

import java.io.IOException;
import java.io.Writer;

/**
 * {@link org.sitemesh.SiteMeshContext} for Micronaut, built on SiteMesh's
 * servlet-free core. Decorator templates are rendered through Micronaut's own
 * {@code ViewsRenderer} (supplied as a callback), so decorators are ordinary
 * view templates — no servlet API, no {@code RequestDispatcher}.
 */
public class MicronautSiteMeshContext extends BaseSiteMeshContext {

    /** Renders a decorator view template to a writer. */
    @FunctionalInterface
    public interface DecoratorRenderer {
        void render(String viewName, Writer out) throws IOException;
    }

    private final String path;
    private final DecoratorRenderer decoratorRenderer;

    public MicronautSiteMeshContext(ContentProcessor contentProcessor, String path,
                                    DecoratorRenderer decoratorRenderer) {
        super(contentProcessor);
        this.path = path;
        this.decoratorRenderer = decoratorRenderer;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    protected void decorate(String decoratorPath, Content content, Writer out) throws IOException {
        decoratorRenderer.render(decoratorPath, out);
    }
}
