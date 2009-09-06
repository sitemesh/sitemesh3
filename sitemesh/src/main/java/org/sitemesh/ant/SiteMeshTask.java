package org.sitemesh.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.types.FileSet;
import org.sitemesh.builder.SiteMeshOfflineBuilder;
import org.sitemesh.config.ObjectFactory;
import org.sitemesh.config.xml.XmlOfflineConfigurator;
import org.sitemesh.offline.SiteMeshOffline;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.List;
import java.util.LinkedList;

/**
 * @author Richard L. Burton III - SmartCode LLC
 * @author Joe Walnes
 */
public class SiteMeshTask extends MatchingTask {

    /**
     * The destination directory where the decorated files will be placed.
     */
    private File destDir;

    /**
     * The sitemesh configuration file.
     */
    private File config;

    private File srcdir;

    /**
     * The files to decorate with SiteMesh.
     */
    private List<FileSet> resources = new LinkedList<FileSet>();

    @Override
    public void execute() throws BuildException {
        verify();

        List<FileSet> implicitFileSet = getImplicitAndExplicitFileSet();
        processFileSet(implicitFileSet);
    }

    /**
     * Returns the all implicit and explicit FileSets.
     *
     * @return The FileSets to process.
     */
    protected List<FileSet> getImplicitAndExplicitFileSet() {
        Vector<FileSet> vfss = new Vector<FileSet>();
        FileSet fs = (FileSet) getImplicitFileSet().clone();

        if (srcdir != null) {
            fs.setDir(srcdir);
        }

        if (fs.getDir() != null) {
            vfss.addElement(fs);
        }

        for (FileSet rc : resources) {
            log("Adding FileSet to the list of FileSets to be processed " + rc.getDescription());
            vfss.addElement(rc);
        }
        return vfss;
    }

    /**
     * Verify the required parameters.
     *
     * @throws BuildException If either the source or destination directories are not set.
     */
    protected void verify() throws BuildException {
        if (destDir == null) {
            throw new BuildException("dest not specified");
        }
    }

    /**
     * Processes the FileSet to decorate all of the provided files.
     *
     * @param filesets The set of files to be processed.
     * @throws BuildException When there's a problem decorating the files.
     */
    protected void processFileSet(List<FileSet> filesets) {
        for (FileSet fileset : filesets) {
            DirectoryScanner directoryScanner = fileset.getDirectoryScanner(getProject());
            directoryScanner.setBasedir(fileset.getDir(getProject()));
            for (String path : directoryScanner.getIncludedFiles()) {
                SiteMeshOffline siteMeshOffline = createSiteMeshOffline(fileset);
                try {
                    log("Processing '" + path + "'");
                    siteMeshOffline.process(path);
                } catch (IOException ioe) {
                    throw new BuildException(ioe);
                }
            }
        }
    }

    /**
     * Creates The SiteMeshOffline generator for the given FileSet.
     *
     * @param fileset The FileSet to be processed.
     * @return An instance of SiteMeshOffline.
     */
    protected SiteMeshOffline createSiteMeshOffline(FileSet fileset) {
        File destintationDir = destDir;

        SiteMeshOfflineBuilder builder = new SiteMeshOfflineBuilder();
        if (isSiteMeshFileSet(fileset)) {
            SiteMeshFileSet sfs = (SiteMeshFileSet) fileset;
            if (hasDestdir(fileset)) {
                destintationDir = sfs.getDestdir();
            }

            if (sfs.hasDecorator()) {
                String[] includes = fileset.mergeIncludes(getProject());
                for (String include : includes) {
                    builder.addDecoratorPath(include, sfs.getDecorator());
                }
            }
        }

        builder.setSourceDirectory(fileset.getDir())
               .setDestinationDirectory(destintationDir);

        if (isSiteMeshFileSetWithoutDecorator(fileset) || config != null) {
            new XmlOfflineConfigurator(new ObjectFactory.Default(), parseSiteMeshXmlConfig(config))
                    .configureOffline(builder);
        }



        applyCustomConfiguration(builder);

        return builder.create();
    }

    /**
     * A helper method that checks if the given FileSet is an instance of
     * SiteMesh and no decorator was set on the given FileSet.
     *
     * @param fileset The FileSet to inspect.
     * @return True if the FileSet is an instance of SiteMeshFileSet and no decorator was set,
     *         false otherwise.
     */
    protected boolean isSiteMeshFileSetWithoutDecorator(FileSet fileset) {
        return isSiteMeshFileSet(fileset) && !((SiteMeshFileSet) fileset).hasDecorator();
    }

    /**
     * Checks to if the FileSet has a destdir set.
     *
     * @param fileset The FileSet to check.
     * @return true if the destdir is set, false otherwise.
     */
    protected boolean hasDestdir(FileSet fileset) {
        return isSiteMeshFileSet(fileset) && ((SiteMeshFileSet) fileset).hasDestdir();
    }

    protected boolean isSiteMeshFileSet(FileSet fileset) {
        return fileset instanceof SiteMeshFileSet;
    }

    protected Element parseSiteMeshXmlConfig(File config) throws BuildException {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(config).getDocumentElement();
        } catch (Exception e) {
            throw new BuildException("Could not parse " + config.getAbsolutePath() + " : " + e.getMessage(), e);
        }
    }

    /**
     * Override this to apply custom configuration after after the default configuration mechanisms.
     *
     * @param builder The SitemeshOfflineBuilder used in creating the SiteMeshOfflineGenerator.
     */
    @SuppressWarnings("UnusedDeclaration")
    protected void applyCustomConfiguration(SiteMeshOfflineBuilder builder) {
    }

    /**
     * Destination directory where decorated content will be output to.
     */
    public void setDestdir(File destDir) {
        this.destDir = destDir;
    }


    /**
     * Source directory containing undecorated content and decorators.
     */
    public void setSrcdir(File srcDir) {
        this.srcdir = srcDir;
    }

    /**
     * Optional path to SiteMesh XML config file.
     */
    public void setConfig(File config) {
        this.config = config;
    }

    public void addSiteMeshfileset(SiteMeshFileSet fileset) {
        resources.add(fileset);
    }

}