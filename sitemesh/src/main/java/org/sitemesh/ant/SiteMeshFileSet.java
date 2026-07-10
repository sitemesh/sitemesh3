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

package org.sitemesh.ant;

import org.apache.tools.ant.types.FileSet;

import java.io.File;

/**
 * A SiteMesh {@link FileSet} that is aware of what decorator to
 * apply for the given set of resource files.
 * @author Richard L. Burton III - SmartCode LLC
 */
public class SiteMeshFileSet extends FileSet {

    /** The decorator to use when decorating the set of files. */
    private String decorator;

    /** An optional destination directory. */
    private File destdir;

    /** Create an empty SiteMeshFileSet. */
    public SiteMeshFileSet() {
    }

    /**
     * Create a SiteMeshFileSet from an existing FileSet.
     *
     * @param fileSet FileSet to copy the configuration from
     */
    public SiteMeshFileSet(FileSet fileSet) {
        super(fileSet);
    }

    /**
     * @return the decorator to use when decorating the set of files
     */
    public String getDecorator() {
        return decorator;
    }

    /**
     * @param decorator the decorator to use when decorating the set of files
     */
    public void setDecorator(String decorator) {
        this.decorator = decorator;
    }

    /**
     * @return the destination directory, or null if not set
     */
    public File getDestdir() {
        return destdir;
    }

    /**
     * @param destdir optional destination directory for the decorated files
     */
    public void setDestdir(File destdir) {
        this.destdir = destdir;
    }

    /**
     * @return true if a decorator was set on this FileSet
     */
    public boolean hasDecorator() {
        return decorator != null;
    }

    /**
     * @return true if a destination directory was set on this FileSet
     */
    public boolean hasDestdir(){
        return destdir != null;
    }
    
}
