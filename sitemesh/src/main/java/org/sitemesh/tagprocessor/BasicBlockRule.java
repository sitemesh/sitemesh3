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
 * {@link TagRule} helper class for dealing with blocks surrounded by an opening and closing tag.
 * e.g. <code>&lt;tag&gt;...&lt;/tag&gt;</code>.
 *
 * <p>Subclasses should implement {@link #processStart(Tag)} and {@link #processEnd(Tag, Object)},
 * which get called at the start and end of the block, respectively. Optionally, data may be passed
 * between the two by returning an object from {@link #processStart(Tag)} which will get passed to
 * the matching {@link #processEnd(Tag, Object)} call, even if there are nested tags. The class type
 * of data used matches the generic type T of this class.
 *
 * @author Joe Walnes
 */
public abstract class BasicBlockRule<T> extends BasicRule {

    private DataHolder<T> current;

    /**
     * Called when a block is started (i.e. <code>&lt;opening&gt;</code> tag is encountered).
     *
     * @param tag Opening tag.
     * @return Any data that needs to be passed to {@link #processEnd(Tag, Object)}. May be null.
     */
    protected abstract T processStart(Tag tag) throws IOException;

    /**
     * Called when a block is ended (i.e. <code>&lt;/closing&gt;</code> tag is encountered).
     *
     * @param tag Closing tag. This will not have any attributes available (as they are associated
     *            with an opening tag. To get access to the attributes, the {@link #processStart(Tag)} method
     *            should access them and return them as data.
     * @param data Data returned from {@link #processStart(Tag)}. May be null.
     */
    protected abstract void processEnd(Tag tag, T data) throws IOException;

    @Override
    public void process(Tag tag) throws IOException {
        switch (tag.getType()) {
            case OPEN: { // <tag>
                T data = processStart(tag);
                current = new DataHolder<T>(data, current);
                break;
            }
            case CLOSE: { // </tag>
                if (current != null) {
                    processEnd(tag, current.getData());
                    current = current.getPrevious();
                }
                break;
            }
            case EMPTY: { // <tag/>
                T data = processStart(tag);
                processEnd(tag, data);
                break;
            }
        }
    }

    /**
     * A one-way linked list to hold nested data.
     */
    private static class DataHolder<T> {
        private final T data;
        private final DataHolder<T> previous;

        public DataHolder(T data, DataHolder<T> previous) {
            this.data = data;
            this.previous = previous;
        }

        public T getData() {
            return data;
        }

        public DataHolder<T> getPrevious() {
            return previous;
        }
    }

}
