/********************************************************************************
 * Licensed Materials - Property of IBM                                          *
 * (c) Copyright IBM Corporation 2018, 2024. All Rights Reserved.                *
 *                                                                               *
 * Note to U.S. Government Users Restricted Rights:                              *
 * Use, duplication or disclosure restricted by GSA ADP Schedule                 *
 * Contract with IBM Corp.                                                       *
 ********************************************************************************/

package com.ibm.dbb.migration.utils;

import com.ibm.dbb.build.BuildException;
import com.ibm.dbb.dependency.LogicalFile;
import com.ibm.dbb.metadata.BuildGroup;
import com.ibm.dbb.metadata.Collection;
import com.ibm.dbb.metadata.MetadataStore;
import com.ibm.dbb.metadata.MetadataStoreFactory;

import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * Utility class for DBB MetadataStore operations.
 * Provides methods to initialize and interact with file-based or DB2-based metadata stores.
 */
public class MetadataStoreUtility {
    private MetadataStore metadataStore;
    
    /**
     * Initialize a file-based metadata store
     * @param directory Directory path for the file metadata store
     * @throws BuildException if initialization fails
     */
    public void initializeFileMetadataStore(String directory) throws BuildException {
        metadataStore = MetadataStoreFactory.createFileMetadataStore(directory);
    }
    
    /**
     * Initialize a DB2-based metadata store with password file
     * @param jdbcId JDBC user ID
     * @param passwordFile File containing the password
     * @param db2Props DB2 connection properties
     * @throws BuildException if initialization fails
     */
    public void initializeDb2MetadataStoreWithPasswordFile(String jdbcId, File passwordFile,
                                                           Properties db2Props) throws BuildException {
        metadataStore = MetadataStoreFactory.createDb2MetadataStore(jdbcId, passwordFile, db2Props);
    }
    
    /**
     * Delete a build group if it exists
     * @param name Build group name
     * @throws BuildException if deletion fails
     */
    public void deleteBuildGroup(String name) throws BuildException {
        if (metadataStore != null && metadataStore.buildGroupExists(name)) {
            metadataStore.deleteBuildGroup(name);
        }
    }
    
    /**
     * Create a collection within a build group
     * @param buildGroupName Build group name
     * @param collectionName Collection name
     * @return The created collection
     * @throws BuildException if creation fails
     */
    public Collection createCollection(String buildGroupName, String collectionName) throws BuildException {
        if (metadataStore == null) {
            throw new IllegalStateException("MetadataStore not initialized");
        }
        
        BuildGroup buildGroup;
        if (!metadataStore.buildGroupExists(buildGroupName)) {
            buildGroup = metadataStore.createBuildGroup(buildGroupName);
        } else {
            buildGroup = metadataStore.getBuildGroup(buildGroupName);
        }
        
        if (buildGroup.collectionExists(collectionName)) {
            buildGroup.deleteCollection(collectionName);
        }
        
        return buildGroup.createCollection(collectionName);
    }
    
    /**
     * Get all build groups from the metadata store
     * @return List of build groups
     * @throws BuildException if retrieval fails
     */
    public List<BuildGroup> getBuildGroups() throws BuildException {
        if (metadataStore == null) {
            throw new IllegalStateException("MetadataStore not initialized");
        }
        return metadataStore.getBuildGroups();
    }
    
    /**
     * Get a logical file from a specific collection
     * @param file File path
     * @param buildGroup Build group name
     * @param collection Collection name
     * @return The logical file
     * @throws BuildException if retrieval fails
     */
    public LogicalFile getLogicalFile(String file, String buildGroup, String collection) throws BuildException {
        if (metadataStore == null) {
            throw new IllegalStateException("MetadataStore not initialized");
        }
        return metadataStore.getBuildGroup(buildGroup).getCollection(collection).getLogicalFile(file);
    }
    
    /**
     * Get the underlying metadata store instance
     * @return The metadata store
     */
    public MetadataStore getMetadataStore() {
        return metadataStore;
    }
}

// Made with Bob