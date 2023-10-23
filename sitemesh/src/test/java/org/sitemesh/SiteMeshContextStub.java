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

package org.sitemesh;

import java.io.IOException;

import org.sitemesh.content.Content;
import org.sitemesh.content.ContentProcessor;

/**
 * Stub {@link SiteMeshContext} implementation, for use in tests.
 *
 * @author Joe Walnes
 */
public class SiteMeshContextStub implements SiteMeshContext {

    private String path;
    private Content contentToMerge;
    private ContentProcessor contentProcessor;

    public String getPath() {
        return path;
    }

    public SiteMeshContextStub withPath(String path) {
        this.path = path;
        return this;
    }

    public Content decorate(String decoratorName, Content content) throws IOException {
        throw new UnsupportedOperationException("Not supported by SiteMeshContextStub");
    }

    public Content getContentToMerge() {
        return contentToMerge;
    }

    public void setContentToMerge(Content contentToMerge) {
        this.contentToMerge = contentToMerge;
    }
    
    public ContentProcessor getContentProcessor() {
        return contentProcessor;
    }
    
    public void setContentProcessor(ContentProcessor contentProcessor) {
        this.contentProcessor = contentProcessor;
    }
}
