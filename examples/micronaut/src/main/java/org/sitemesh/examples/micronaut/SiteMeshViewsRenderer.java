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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.io.Writable;
import io.micronaut.http.HttpRequest;
import io.micronaut.views.ViewsRenderer;
import io.micronaut.views.thymeleaf.ThymeleafViewsRenderer;
import jakarta.inject.Singleton;
import org.sitemesh.config.MetaTagBasedDecoratorSelector;
import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;
import org.sitemesh.content.tagrules.decorate.DecoratorTagRuleBundle;
import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;

import java.io.StringWriter;
import java.nio.CharBuffer;

/**
 * A filterless SiteMesh integration for Micronaut, analogous to the Spring
 * Boot starter's view-resolver mode ({@code SiteMeshView}): a decorating
 * {@link ViewsRenderer} registered at highest precedence, so the views
 * locator picks it over the engine renderer it delegates to.
 *
 * <p>The inner view is rendered into a plain in-memory buffer, parsed by
 * SiteMesh's servlet-free content processor, and merged into a decorator that
 * is itself rendered through the delegate {@link ViewsRenderer} — so
 * decorators are ordinary view templates under {@code views/decorators/}.
 * There is no filter, no response wrapper and no {@code RequestDispatcher}
 * anywhere, so nothing depends on container forward/include semantics.</p>
 *
 * <p>The decorator defaults to {@code decorators/default}; a page can pick a
 * different one with {@code <meta name="decorator" content="decorators/...">}.
 * Views under {@code decorators/} are never decorated themselves.</p>
 */
@Singleton
public class SiteMeshViewsRenderer implements ViewsRenderer<Object, HttpRequest<?>> {

    static final String DECORATOR_PREFIX = "decorators/";
    static final String DEFAULT_DECORATOR = DECORATOR_PREFIX + "default";

    private static final ContentProcessor CONTENT_PROCESSOR =
            new TagBasedContentProcessor(new CoreHtmlTagRuleBundle(), new DecoratorTagRuleBundle());

    private final ThymeleafViewsRenderer<Object> delegate;

    public SiteMeshViewsRenderer(ThymeleafViewsRenderer<Object> delegate) {
        this.delegate = delegate;
    }

    @Override
    @NonNull
    public Writable render(@NonNull String viewName, @Nullable Object data,
                           @Nullable HttpRequest<?> request) {
        Writable inner = delegate.render(viewName, data, request);
        if (viewName.startsWith(DECORATOR_PREFIX)) {
            return inner; // Decorator templates must not themselves be decorated.
        }
        return writer -> {
            StringWriter buffer = new StringWriter();
            inner.writeTo(buffer);

            String path = request != null ? request.getPath() : "/" + viewName;
            MicronautSiteMeshContext context = new MicronautSiteMeshContext(CONTENT_PROCESSOR, path,
                    (decoratorView, out) -> delegate.render(decoratorView, data, request).writeTo(out));

            Content content = CONTENT_PROCESSOR.build(CharBuffer.wrap(buffer.getBuffer()), context);
            if (content == null) {
                writer.write(buffer.toString());
                return;
            }

            MetaTagBasedDecoratorSelector<MicronautSiteMeshContext> decoratorSelector =
                    new MetaTagBasedDecoratorSelector<>();
            decoratorSelector.put("/*", DEFAULT_DECORATOR);

            Content decorated = content;
            for (String decoratorPath : decoratorSelector.selectDecoratorPaths(content, context)) {
                if (decoratorPath == null) {
                    continue;
                }
                decorated = context.decorate(decoratorPath, decorated);
                if (decorated == null) {
                    break;
                }
            }
            if (decorated == null) {
                decorated = content; // Decorator failed; fall back to the undecorated page.
            }
            decorated.getData().writeValueTo(writer);
        };
    }

    @Override
    public boolean exists(@NonNull String viewName) {
        return delegate.exists(viewName);
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
