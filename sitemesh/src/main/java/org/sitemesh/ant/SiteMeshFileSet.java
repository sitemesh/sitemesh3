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

    public SiteMeshFileSet() {
    }

    public SiteMeshFileSet(FileSet fileSet) {
        super(fileSet);
    }

    public String getDecorator() {
        return decorator;
    }

    public void setDecorator(String decorator) {
        this.decorator = decorator;
    }

    public File getDestdir() {
        return destdir;
    }

    public void setDestdir(File destdir) {
        this.destdir = destdir;
    }

    public boolean hasDecorator() {
        return decorator != null;
    }

    public boolean hasDestdir(){
        return destdir != null;
    }
    
}
