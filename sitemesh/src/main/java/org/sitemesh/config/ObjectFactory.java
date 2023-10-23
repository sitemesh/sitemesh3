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

package org.sitemesh.config;

/**
 * Responsible for instantiating objects - converting from strings in a config file to a real instance.
 *
 * <p>Typically you would use the {@link ObjectFactory.Default} implementation,
 * but this can be replaced. e.g. To connect to a dependency injection framework, lookup from
 * a registry, do custom classloading, etc.</p>
 *
 * @see org.sitemesh.config.ObjectFactory.Default
 * @author Joe Walnes
 */
public interface ObjectFactory {

    Object create(String name) throws IllegalArgumentException;

    /**
     * Default implementation of {@link ObjectFactory} that treats the object
     * name as a class name, loading it from the current ClassLoader and instantiating
     * with the default constructor.
     */
    public static class Default implements ObjectFactory {
        public Object create(String name) {
            try {
                Class cls = Class.forName(name);
                return cls.newInstance();
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Could not instantiate " + name, e);
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Could not instantiate " + name, e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Could not instantiate " + name, e);
            }
        }
    }
}
