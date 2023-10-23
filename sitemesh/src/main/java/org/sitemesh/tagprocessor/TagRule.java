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

package org.sitemesh.tagprocessor;

import java.io.IOException;

/**
 * User defined rule for processing {@link Tag}s encountered by the {@link TagProcessor}.
 *
 * See {@link BasicRule} and {@link BasicBlockRule} for implementations that
 * provide basic functionality.
 *
 * @author Joe Walnes
 */
public interface TagRule {

    /**
     * Injected by the {@link TagProcessor} before any of the other TagRule methods.
     */
    void setTagProcessorContext(TagProcessorContext context);

    /**
     * Implementations can use this to do any necessary work on the {@link Tag} such as extracting
     * values or transforming it.
     */
    void process(Tag tag) throws IOException;
}
