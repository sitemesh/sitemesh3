package org.sitemesh.builder;

import org.sitemesh.offline.SiteMeshOffline;
import org.sitemesh.offline.OfflineContext;
import org.sitemesh.offline.directory.Directory;
import org.sitemesh.offline.directory.FileSystemDirectory;

import java.io.File;

/**
 * Functionality for building a {@link org.sitemesh.offline.SiteMeshOffline}.
 * Inherits common functionality from {@link BaseSiteMeshBuilder}.
 *
 * <p>Clients should use the concrete {@link SiteMeshOfflineBuilder} implementation.</p>
 *
 * @author Joe Walnes
 * @param <BUILDER> The type to return from the builder methods. Subclasses
 * should type this as their own class type.
 * @see BaseSiteMeshBuilder
 * @see org.sitemesh.offline.SiteMeshOffline
 */
public abstract class BaseSiteMeshOfflineBuilder<BUILDER extends BaseSiteMeshOfflineBuilder>
        extends BaseSiteMeshBuilder<BUILDER, OfflineContext, SiteMeshOffline> {

    private Directory sourceDirectory;
    private Directory destinationDirectory;

    /**
     * Create the SiteMeshOfflineGenerator.
     */
    @Override
    public abstract SiteMeshOffline create();

    // --------------------------------------------------------------
    // Source Directory setup.

    /**
     * Set source directory, were the unprocessed files will be read from.
     *
     * <p>The {@link Directory} abstraction allows for plugging into other storage
     * mechanisms than a standard file system (e.g. in memory, database, etc).</p>
     */
    public BUILDER setSourceDirectory(Directory sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
        return self();
    }

    /**
     * Set source directory, were the processed files will be read from.
     * This should be a readable directory.
     */
    public BUILDER setSourceDirectory(File sourceDirectory) {
        setSourceDirectory(new FileSystemDirectory(sourceDirectory));
        return self();
    }

    /**
     * Set source directory, were the processed files will be read from.
     * This should be a readable directory path, relative to the directory
     * the process is running in.
     */
    public BUILDER setSourceDirectory(String sourceDirectoryPath) {
        setSourceDirectory(new File(sourceDirectoryPath));
        return self();
    }

    /**
     * Get the source directory.
     *
     * @throws IllegalStateException if directory has not been set.
     */
    public Directory getSourceDirectory() throws IllegalStateException {
        if (sourceDirectory == null) {
            throw new IllegalStateException("Source directory required. Call "
                    + getClass().getSimpleName() + ".setSourceDirectory()");
        }
        return sourceDirectory;
    }

    // --------------------------------------------------------------
    // Destination Directory setup.

    /**
     * Set destination directory, were the processed files will be written to.
     *
     * <p>The {@link Directory} abstraction allows for plugging into other storage
     * mechanisms than a standard file system (e.g. in memory, database, etc).</p>
     */
    public BUILDER setDestinationDirectory(Directory destinationDirectory) {
        this.destinationDirectory = destinationDirectory;
        return self();
    }

    /**
     * Set destination directory, were the processed files will be written to.
     * This should be a writable directory.
     */
    public BUILDER setDestinationDirectory(File destinationDirectory) {
        setDestinationDirectory(new FileSystemDirectory(destinationDirectory));
        return self();
    }

    /**
     * Set destination directory, were the processed files will be written to.
     * This should be a writable directory path, relative to the directory
     * the process is running in.
     */
    public BUILDER setDestinationDirectory(String destinationDirectoryPath) {
        setDestinationDirectory(new File(destinationDirectoryPath));
        return self();
    }

    /**
     * Get the destination directory.
     *
     * @throws IllegalStateException if directory has not been set.
     */
    public Directory getDestinationDirectory() throws IllegalStateException {
        if (destinationDirectory == null) {
            throw new IllegalStateException("Destination directory required. Call "
                    + getClass().getSimpleName() + ".setDestinationDirectory()");
        }
        return destinationDirectory;
    }

    // --------------------------------------------------------------

}
