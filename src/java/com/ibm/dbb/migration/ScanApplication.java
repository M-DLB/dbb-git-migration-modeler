/********************************************************************************
 * Licensed Materials - Property of IBM                                          *
 * (c) Copyright IBM Corporation 2018, 2024. All Rights Reserved.                *
 *                                                                               *
 * Note to U.S. Government Users Restricted Rights:                              *
 * Use, duplication or disclosure restricted by GSA ADP Schedule                 *
 * Contract with IBM Corp.                                                       *
 ********************************************************************************/

package com.ibm.dbb.migration;

import com.ibm.dbb.dependency.DependencyScanner;
import com.ibm.dbb.dependency.LogicalFile;
import com.ibm.dbb.metadata.Collection;
import com.ibm.dbb.build.BuildException;
import com.ibm.dbb.migration.model.ApplicationDescriptor;
import com.ibm.dbb.migration.utils.Logger;
import com.ibm.dbb.migration.utils.ApplicationDescriptorUtils;
import com.ibm.dbb.migration.utils.MetadataStoreUtility;
import com.ibm.dbb.migration.utils.FileUtility;
import org.apache.commons.cli.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Standalone Java application to scan application files and store dependency
 * information in a DBB metadata store.
 * 
 * This is a Java conversion of the original scanApplication.groovy script.
 */
public class ScanApplication {
    
    private Properties props;
    private Logger logger;
    private MetadataStoreUtility metadataStoreUtils;
    private ApplicationDescriptorUtils applicationDescriptorUtils;
    private FileUtility fileUtils;
    
    public static void main(String[] args) {
        ScanApplication scanner = new ScanApplication();
        try {
            scanner.run(args);
        } catch (Exception e) {
            System.err.println("Error executing scan: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void run(String[] args) throws Exception {
        // Initialize
        props = new Properties();
        logger = new Logger();
        metadataStoreUtils = new MetadataStoreUtility();
        applicationDescriptorUtils = new ApplicationDescriptorUtils();
        fileUtils = new FileUtility();
        
        // Parse arguments
        parseArgs(args);
        
        // Initialize script parameters
        initScriptParameters();
        
        // Read application descriptor
        File applicationDescriptorFile = new File(props.getProperty("applicationDirectory") + "/applicationDescriptor.yml");
        
        logger.logMessage("** Reading the existing Application Descriptor file.");
        
        ApplicationDescriptor applicationDescriptor;
        if (applicationDescriptorFile.exists()) {
            applicationDescriptor = applicationDescriptorUtils.readApplicationDescriptor(applicationDescriptorFile);
        } else {
            logger.logMessage("*! [WARNING] The Application Descriptor file " + 
                applicationDescriptorFile.getPath() + " was not found. Exiting.");
            System.exit(1);
            return;
        }
        
        // Get mapped files
        logger.logMessage("** Retrieving the list of files mapped to Source Groups.");
        Set<String> files = fileUtils.getMappedFilesFromApplicationDescriptor(
            props.getProperty("applicationDirectory"), 
            applicationDescriptor, 
            logger);
        
        // Scan files
        logger.logMessage("** Scanning the files.");
        List<LogicalFile> logicalFiles = scanFiles(files);
        
        // Store results
        String collectionName = props.getProperty("application") + "-" + 
            props.getProperty("APPLICATION_DEFAULT_BRANCH");
        logger.logMessage("** Storing results in the '" + collectionName + "' DBB Collection.");
        
        // Manage Build Groups and Collections
        metadataStoreUtils.deleteBuildGroup(collectionName);
        Collection collection = metadataStoreUtils.createCollection(collectionName, collectionName);
        
        // Store results
        collection.addLogicalFiles(logicalFiles);
        
        logger.close();
    }
    
    private List<LogicalFile> scanFiles(Set<String> files) {
        List<LogicalFile> logicalFiles = new ArrayList<>();
        DependencyScanner scanner = new DependencyScanner();
        
        // Enabling Control Transfer flag in DBB Scanner
        boolean scanControlTransfers = Boolean.parseBoolean(props.getProperty("SCAN_CONTROL_TRANSFERS", "false"));
        scanner.setCollectControlTransfers(String.valueOf(scanControlTransfers));
        
        for (String file : files) {
            logger.logMessage("\tScanning file " + file);
            try {
                LogicalFile logicalFile = scanner.scan(file, props.getProperty("applicationDirectory"));
                logicalFiles.add(logicalFile);
            } catch (Exception e) {
                logger.logMessage("\t*! [ERROR] Something went wrong when scanning the file '" + file + "'.");
                logger.logMessage(e.getMessage());
            }
        }
        
        return logicalFiles;
    }
    
    private void parseArgs(String[] args) throws Exception {
        Properties configuration = new Properties();
        
        // Create CLI parser
        Options options = new Options();
        options.addOption(Option.builder("a")
            .longOpt("application")
            .hasArg()
            .required()
            .desc("Application name")
            .build());
        options.addOption(Option.builder("l")
            .longOpt("logFile")
            .hasArg()
            .desc("Relative or absolute path to an output log file")
            .build());
        options.addOption(Option.builder("c")
            .longOpt("configFile")
            .hasArg()
            .required()
            .desc("Path to the DBB Git Migration Modeler Configuration file (created by the Setup script)")
            .build());
        
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error parsing command line: " + e.getMessage());
            formatter.printHelp("scanApplication [options]", options);
            System.exit(1);
            return;
        }
        
        // Handle log file
        if (cmd.hasOption("l")) {
            props.setProperty("logFile", cmd.getOptionValue("l"));
            logger.create(props.getProperty("logFile"));
        }
        
        // Handle application name
        if (cmd.hasOption("a")) {
            props.setProperty("application", cmd.getOptionValue("a"));
        } else {
            logger.logMessage("*! [ERROR] The Application name (option -a/--application) must be provided. Exiting.");
            System.exit(1);
        }
        
        // Handle configuration file
        if (cmd.hasOption("c")) {
            String configFilePath = cmd.getOptionValue("c");
            props.setProperty("configurationFilePath", configFilePath);
            
            // Validate and load configuration using ValidateConfiguration
            logger.logMessage("** Validating configuration file...");
            try {
                configuration = ValidateConfiguration.validateAndLoadConfiguration(configFilePath);
            } catch (Exception e) {
                logger.logMessage("*! [ERROR] Configuration validation failed: " + e.getMessage());
                System.exit(1);
            }
        } else {
            logger.logMessage("*! [ERROR] The path to the DBB Git Migration Modeler Configuration file " +
                "was not specified ('-c/--configFile' parameter). Exiting.");
            System.exit(1);
        }
        
        // Validate and set application directory
        if (configuration.getProperty("DBB_MODELER_APPLICATION_DIR") != null) {
            String applicationDirectory = configuration.getProperty("DBB_MODELER_APPLICATION_DIR") + 
                "/" + props.getProperty("application");
            File directory = new File(applicationDirectory);
            if (directory.exists()) {
                props.setProperty("applicationDirectory", applicationDirectory);
            } else {
                logger.logMessage("*! [ERROR] Application Directory '" + applicationDirectory + 
                    "' does not exist. Exiting.");
                System.exit(1);
            }
        } else {
            logger.logMessage("*! [ERROR] The Applications directory must be specified in the " +
                "DBB Git Migration Modeler Configuration file. Exiting.");
            System.exit(1);
        }
        
        // Validate repository path mapping file
        if (configuration.getProperty("REPOSITORY_PATH_MAPPING_FILE") != null) {
            File file = new File(configuration.getProperty("REPOSITORY_PATH_MAPPING_FILE"));
            if (file.exists()) {
                props.setProperty("REPOSITORY_PATH_MAPPING_FILE", 
                    configuration.getProperty("REPOSITORY_PATH_MAPPING_FILE"));
            } else {
                logger.logMessage("*! [ERROR] The Repository Paths Mapping file '" + 
                    configuration.getProperty("REPOSITORY_PATH_MAPPING_FILE") + "' does not exist. Exiting.");
                System.exit(1);
            }
        } else {
            logger.logMessage("*! [ERROR] The path to the Repository Paths Mapping file must be " +
                "specified in the DBB Git Migration Modeler Configuration file. Exiting.");
            System.exit(1);
        }
        
        // Validate metadata store type
        if (configuration.getProperty("DBB_MODELER_METADATASTORE_TYPE") != null) {
            String metadataStoreType = configuration.getProperty("DBB_MODELER_METADATASTORE_TYPE");
            props.setProperty("DBB_MODELER_METADATASTORE_TYPE", metadataStoreType);
            if (!metadataStoreType.equals("file") && !metadataStoreType.equals("db2")) {
                logger.logMessage("*! [ERROR] The type of MetadataStore can only be 'file' or 'db2'. Exiting.");
                System.exit(1);
            }
        } else {
            logger.logMessage("*! [ERROR] The type of MetadataStore (file or db2) must be specified " +
                "in the DBB Git Migration Modeler Configuration file. Exiting.");
            System.exit(1);
        }
        
        // Handle file metadata store
        if (props.getProperty("DBB_MODELER_METADATASTORE_TYPE").equals("file")) {
            if (configuration.getProperty("DBB_MODELER_FILE_METADATA_STORE_DIR") != null) {
                File directory = new File(configuration.getProperty("DBB_MODELER_FILE_METADATA_STORE_DIR"));
                if (directory.exists()) {
                    props.setProperty("DBB_MODELER_FILE_METADATA_STORE_DIR", 
                        configuration.getProperty("DBB_MODELER_FILE_METADATA_STORE_DIR"));
                } else {
                    logger.logMessage("*! [ERROR] The location for the File MetadataStore '" + 
                        configuration.getProperty("DBB_MODELER_FILE_METADATA_STORE_DIR") + 
                        "' does not exist. Exiting.");
                    System.exit(1);
                }
            } else {
                logger.logMessage("*! [ERROR] The location of the File MetadataStore must be specified " +
                    "in the DBB Git Migration Modeler Configuration file. Exiting.");
                System.exit(1);
            }
        }
        // Handle DB2 metadata store
        else if (props.getProperty("DBB_MODELER_METADATASTORE_TYPE").equals("db2")) {
            if (configuration.getProperty("DBB_MODELER_DB2_METADATASTORE_JDBC_ID") != null) {
                props.setProperty("DBB_MODELER_DB2_METADATASTORE_JDBC_ID", 
                    configuration.getProperty("DBB_MODELER_DB2_METADATASTORE_JDBC_ID"));
            } else {
                logger.logMessage("*! [ERROR] The User ID for Db2 MetadataStore JDBC connection must be " +
                    "specified in the DBB Git Migration Modeler Configuration file. Exiting.");
                System.exit(1);
            }
            
            if (configuration.getProperty("DBB_MODELER_DB2_METADATASTORE_CONFIG_FILE") != null) {
                File file = new File(configuration.getProperty("DBB_MODELER_DB2_METADATASTORE_CONFIG_FILE"));
                if (file.exists()) {
                    props.setProperty("DBB_MODELER_DB2_METADATASTORE_CONFIG_FILE", 
                        configuration.getProperty("DBB_MODELER_DB2_METADATASTORE_CONFIG_FILE"));
                } else {
                    logger.logMessage("*! [ERROR] The Db2 Connection configuration file for Db2 MetadataStore " +
                        "JDBC connection '" + configuration.getProperty("DBB_MODELER_DB2_METADATASTORE_CONFIG_FILE") + 
                        "' does not exist. Exiting.");
                    System.exit(1);
                }
            } else {
                logger.logMessage("*! [ERROR] The path to the Db2 Connection configuration file for Db2 " +
                    "MetadataStore JDBC connection must be specified in the DBB Git Migration Modeler " +
                    "Configuration file. Exiting.");
                System.exit(1);
            }
            
            if (configuration.getProperty("DBB_MODELER_DB2_METADATASTORE_JDBC_PASSWORDFILE") == null) {
                logger.logMessage("*! [ERROR] The Password File for Db2 Metadatastore JDBC connection must be " +
                    "specified in the DBB Git Migration Modeler Configuration file. Exiting.");
                System.exit(1);
            } else {
                props.setProperty("DBB_MODELER_DB2_METADATASTORE_JDBC_PASSWORDFILE", 
                    configuration.getProperty("DBB_MODELER_DB2_METADATASTORE_JDBC_PASSWORDFILE"));
            }
        }
        
        // Validate application default branch
        if (configuration.getProperty("APPLICATION_DEFAULT_BRANCH") != null) {
            props.setProperty("APPLICATION_DEFAULT_BRANCH", 
                configuration.getProperty("APPLICATION_DEFAULT_BRANCH"));
        } else {
            logger.logMessage("*! [ERROR] The Application Default Branch must be specified in the " +
                "DBB Git Migration Modeler Configuration file. Exiting.");
            System.exit(1);
        }
        
        // Validate scan control transfers
        if (configuration.getProperty("SCAN_CONTROL_TRANSFERS") != null) {
            props.setProperty("SCAN_CONTROL_TRANSFERS", 
                configuration.getProperty("SCAN_CONTROL_TRANSFERS"));
        } else {
            logger.logMessage("*! [ERROR] The Scan Control Transfers parameter (SCAN_CONTROL_TRANSFERS) " +
                "must be specified in the DBB Git Migration Modeler Configuration file. Exiting.");
            System.exit(1);
        }
        
        // Log configuration
        logger.logMessage("** Script configuration:");
        for (String key : props.stringPropertyNames()) {
            logger.logMessage("\t" + key + " -> " + props.getProperty(key));
        }
    }
    
    private void initScriptParameters() throws Exception {
        // Initialize metadata store
        if (props.getProperty("DBB_MODELER_FILE_METADATA_STORE_DIR") != null) {
            metadataStoreUtils.initializeFileMetadataStore(
                props.getProperty("DBB_MODELER_FILE_METADATA_STORE_DIR"));
        } else {
            File db2ConnectionConfigurationFile = new File(
                props.getProperty("DBB_MODELER_DB2_METADATASTORE_CONFIG_FILE"));
            Properties db2ConnectionProps = new Properties();
            try (FileInputStream fis = new FileInputStream(db2ConnectionConfigurationFile)) {
                db2ConnectionProps.load(fis);
            }
            // Call correct Db2 MetadataStore constructor
            metadataStoreUtils.initializeDb2MetadataStoreWithPasswordFile(
                props.getProperty("DBB_MODELER_DB2_METADATASTORE_JDBC_ID"),
                new File(props.getProperty("DBB_MODELER_DB2_METADATASTORE_JDBC_PASSWORDFILE")),
                db2ConnectionProps);
        }
    }
    
    /**
     * Utility class for metadata store operations
     */
}

// Made with Bob