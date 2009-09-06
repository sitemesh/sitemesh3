package org.sitemesh.ant;

import org.apache.tools.ant.types.FileSet;

import java.io.File;

/**
 * A SiteMesh <tt>FileSet</tt> that is aware of what decorator to
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
