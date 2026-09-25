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

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.views.View;

import java.util.Map;

/**
 * Routes whose {@link View} responses are decorated by {@link SiteMeshViewsRenderer}.
 */
@Controller
public class HelloController {

    /** Creates the controller; instantiated by Micronaut. */
    public HelloController() {
    }

    /**
     * Greets the given name via the {@code hello} view.
     *
     * @param name the name to greet, {@code "World"} if not supplied
     * @return the view model
     */
    @Get("/")
    @View("hello")
    public Map<String, Object> hello(@QueryValue(value = "name", defaultValue = "World") String name) {
        return Map.of("name", name);
    }

    /**
     * Renders the {@code meta} view, which picks its decorator with a
     * {@code <meta name="decorator">} tag.
     *
     * @return an empty view model
     */
    @Get("/meta")
    @View("meta")
    public Map<String, Object> meta() {
        return Map.of();
    }
}
