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

package org.sitemesh.builder;

import org.sitemesh.config.MetaTagBasedDecoratorSelector;
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
     *
     * @return the built SiteMeshOffline.
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
     *
     * @param sourceDirectory the source Directory to read the unprocessed files from.
     * @return this builder instance, for method chaining.
     */
    public BUILDER setSourceDirectory(Directory sourceDirectory) {
        this.sourceDirectory = sourceDirectory;
        return self();
    }

    /**
     * Set source directory, were the processed files will be read from.
     * This should be a readable directory.
     *
     * @param sourceDirectory the source directory to read the unprocessed files from.
     * @return this builder instance, for method chaining.
     */
    public BUILDER setSourceDirectory(File sourceDirectory) {
        setSourceDirectory(new FileSystemDirectory(sourceDirectory));
        return self();
    }

    /**
     * Set source directory, were the processed files will be read from.
     * This should be a readable directory path, relative to the directory
     * the process is running in.
     *
     * @param sourceDirectoryPath the path of the source directory to read the
     *                            unprocessed files from.
     * @return this builder instance, for method chaining.
     */
    public BUILDER setSourceDirectory(String sourceDirectoryPath) {
        setSourceDirectory(new File(sourceDirectoryPath));
        return self();
    }

    /**
     * Get the source directory.
     *
     * @return the configured source directory.
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
     *
     * @param destinationDirectory the destination Directory to write the processed files to.
     * @return this builder instance, for method chaining.
     */
    public BUILDER setDestinationDirectory(Directory destinationDirectory) {
        this.destinationDirectory = destinationDirectory;
        return self();
    }

    /**
     * Set destination directory, were the processed files will be written to.
     * This should be a writable directory.
     *
     * @param destinationDirectory the destination directory to write the processed files to.
     * @return this builder instance, for method chaining.
     */
    public BUILDER setDestinationDirectory(File destinationDirectory) {
        setDestinationDirectory(new FileSystemDirectory(destinationDirectory));
        return self();
    }

    /**
     * Set destination directory, were the processed files will be written to.
     * This should be a writable directory path, relative to the directory
     * the process is running in.
     *
     * @param destinationDirectoryPath the path of the destination directory to
     *                                 write the processed files to.
     * @return this builder instance, for method chaining.
     */
    public BUILDER setDestinationDirectory(String destinationDirectoryPath) {
        setDestinationDirectory(new File(destinationDirectoryPath));
        return self();
    }

    /**
     * Get the destination directory.
     *
     * @return the configured destination directory.
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
