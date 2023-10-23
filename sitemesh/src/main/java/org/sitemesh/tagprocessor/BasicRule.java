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
 * Basic implementation of {@link TagRule}.
 *
 * @author Joe Walnes
 */
public abstract class BasicRule implements TagRule {

    protected TagProcessorContext tagProcessorContext;

    public abstract void process(Tag tag) throws IOException;

    public void setTagProcessorContext(TagProcessorContext tagProcessorContext) {
        this.tagProcessorContext = tagProcessorContext;
    }

}

