package org.sitemesh.maven2Plugin;

/**
 * @author nichole
 */
public class DecoratorMapping {

    /**
     * @parameter property="decoratorFileName"
     * @required
     */
    private String decoratorFileName = null;

    /**
     * @parameter property="contentFilePattern"
     * @required
     */
    private String contentFilePattern = null;

    /**
     * @return the decoratorFileName
     */
    public String getDecoratorFileName() {
        return decoratorFileName;
    }

    /**
     * @param decoratorFilePath the decoratorFilePath to set
     */
    public void setDecoratorFileName(String decoratorFileName) {
        this.decoratorFileName = decoratorFileName;
    }

    /**
     * @return the contentFilePattern
     */
    public String getContentFilePattern() {
        return contentFilePattern;
    }

    /**
     * @param contentFilePattern the contentFile to set
     */
    public void setContentFilePattern(String filePattern) {
        this.contentFilePattern = filePattern;
    }
    
}
