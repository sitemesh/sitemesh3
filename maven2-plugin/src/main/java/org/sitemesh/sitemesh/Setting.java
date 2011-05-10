package org.sitemesh.sitemesh;

import java.io.File;
import java.util.List;

/**
 * directories and paths for one run of the offline builder
 *
 * @author nichole
 */
public class Setting {

    /**
     * @parameter sourceDirectory
     * @required
     */
    private File sourceDirectory = null;

    /**
     * @parameter destinationDirectory
     * @required
     */
    private File destinationDirectory = null;

    /**
     * @parameter decoratorFilePaths
     * @required
     */
    private List<DecoratorMapping> decoratorMappings = null;

    /**
     * @return the sourceDirectory
     */
    public File getSourceDirectory() {
        return sourceDirectory;
    }

    /**
     * @param sourceDirectory the sourceDirectory to set
     */
    public void setSourceDirectory(File sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
    }

    /**
     * @return the destinationDirectory
     */
    public File getDestinationDirectory() {
        return destinationDirectory;
    }

    /**
     * @param destinationDirectory the destinationDirectory to set
     */
    public void setDestinationDirectory(File destinationDirectory) {
        this.destinationDirectory = destinationDirectory;
    }

    /**
     * @return the decoratorMappings
     */
    public List<DecoratorMapping> getDecoratorMappings() {
        return decoratorMappings;
    }

    /**
     * @param decoratorMappings the decoratorMappings to set
     */
    public void setDecoratorMappings(List<DecoratorMapping> decoratorMappings) {
        this.decoratorMappings = decoratorMappings;
    }

}
