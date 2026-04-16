/*
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2018, 2025. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 */

package com.ibm.dbb.migration;

import com.ibm.dbb.migration.utils.FileUtility;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Validates the DBB Git Migration Modeler configuration and environment.
 * Equivalent to the 0-validateConfiguration.sh shell script.
 */
public class ValidateConfiguration {
    
    private String configFilePath;
    private Properties configProperties;
    private int exitCode = 0;
    private String modelerHome;
    
    /**
     * Validates a configuration file and returns the loaded properties.
     * This is a static method that can be called by other tools.
     *
     * @param configFilePath Path to the configuration file
     * @return Properties object with validated configuration
     * @throws Exception if validation fails
     */
    public static Properties validateAndLoadConfiguration(String configFilePath) throws Exception {
        ValidateConfiguration validator = new ValidateConfiguration();
        validator.configFilePath = configFilePath;
        
        // Validate the configuration file exists
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            throw new Exception("DBB Git Migration Modeler configuration file not found: " + configFilePath);
        }
        
        // Load configuration properties
        Properties configProperties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            configProperties.load(fis);
        }
        
        validator.configProperties = configProperties;
        
        // Set modelerHome from DBB_MODELER_HOME property
        validator.modelerHome = configProperties.getProperty("DBB_MODELER_HOME");
        if (validator.modelerHome == null || validator.modelerHome.isEmpty()) {
            throw new Exception("DBB_MODELER_HOME property is not defined in the configuration file");
        }
        
        // Validate environment
        validator.validateEnvironment();
        if (validator.exitCode != 0) {
            throw new Exception("Environment validation failed");
        }
        
        // Validate DBB Toolkit version
        validator.validateDBBToolkitVersion();
        if (validator.exitCode != 0) {
            throw new Exception("DBB Toolkit version validation failed");
        }
        
        // Validate metadata store configuration
        String metadataStoreType = configProperties.getProperty("DBB_MODELER_METADATASTORE_TYPE");
        if ("db2".equals(metadataStoreType)) {
            validator.validateDb2Configuration();
        } else if ("file".equals(metadataStoreType)) {
            validator.validateFileMetadataStore();
        } else {
            throw new Exception("The specified DBB MetadataStore technology is not 'file' or 'db2'.");
        }
        
        if (validator.exitCode != 0) {
            throw new Exception("Configuration validation failed");
        }
        
        // Validate build framework - only zBuilder is supported
        String buildFramework = configProperties.getProperty("BUILD_FRAMEWORK");
        if (!"zBuilder".equals(buildFramework)) {
            throw new Exception("The specified Build Framework '" + buildFramework +
                "' is not valid. Only 'zBuilder' is supported.");
        }
        
        // Validate zBuilder directory
        String zBuilderPath = configProperties.getProperty("DBB_ZBUILDER");
        if (zBuilderPath == null || !new File(zBuilderPath).isDirectory()) {
            throw new Exception("The zBuilder instance '" + zBuilderPath + "' doesn't exist.");
        }
        
        // Validate DBB Community repository
        String dbbCommunityRepo = configProperties.getProperty("DBB_COMMUNITY_REPO");
        if (dbbCommunityRepo != null && !new File(dbbCommunityRepo).isDirectory()) {
            throw new Exception("The DBB Community repository instance '" + dbbCommunityRepo + "' doesn't exist.");
        }
        
        // Validate artifact repository configuration if publishing is enabled
        String publishArtifacts = configProperties.getProperty("PUBLISH_ARTIFACTS");
        if ("true".equals(publishArtifacts)) {
            validator.validateArtifactRepository();
            if (validator.exitCode != 0) {
                throw new Exception("Artifact repository validation failed");
            }
        }
        
        return configProperties;
    }
    
    public static void main(String[] args) {
        ValidateConfiguration validator = new ValidateConfiguration();
        try {
            validator.run(args);
        } catch (Exception e) {
            System.err.println("[ERROR] Validation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(8);
        }
    }
    
    public void run(String[] args) throws Exception {
        // modelerHome will be set from configuration file after loading
        
        // Parse command line options
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("[ERROR] Error parsing command line: " + e.getMessage());
            formatter.printHelp("ValidateConfiguration [options]", 
                "Validates DBB Git Migration Modeler configuration and environment", 
                options, "", true);
            System.exit(2);
            return;
        }
        
        // Process options
        if (cmd.hasOption("c")) {
            configFilePath = cmd.getOptionValue("c");
            validateOptions();
            if (exitCode == 0) {
                validateConfigurationFile();
            }
        } else if (cmd.hasOption("e")) {
            validateEnvironment();
        } else if (cmd.hasOption("f")) {
            configFilePath = cmd.getOptionValue("f");
            validateOptions();
            if (exitCode == 0) {
                initializeWorkDirectory();
            }
        } else {
            System.err.println("[ERROR] At least one option (-c, -e, or -f) is required.");
            formatter.printHelp("ValidateConfiguration [options]", options);
            System.exit(2);
        }
        
        if (exitCode != 0) {
            System.err.println("[ERROR] Failures detected while checking the DBB Git Migration Modeler configuration. rc=" + exitCode);
            System.exit(exitCode);
        }
    }
    
    private Options createOptions() {
        Options options = new Options();
        
        options.addOption(Option.builder("c")
            .longOpt("config")
            .hasArg()
            .argName("configFile")
            .desc("Validate the specified configuration file")
            .build());
            
        options.addOption(Option.builder("e")
            .longOpt("environment")
            .desc("Validate the environment (DBB_HOME, git availability)")
            .build());
            
        options.addOption(Option.builder("f")
            .longOpt("finalize")
            .hasArg()
            .argName("configFile")
            .desc("Finalize setup by initializing work directories")
            .build());
            
        options.addOption(Option.builder("h")
            .longOpt("help")
            .desc("Print this help message")
            .build());
            
        return options;
    }
    
    private void validateOptions() {
        if (configFilePath == null || configFilePath.isEmpty()) {
            exitCode = 8;
            System.err.println("[ERROR] Configuration file path is required.");
            return;
        }
        
        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            exitCode = 8;
            System.err.println("[ERROR] DBB Git Migration Modeler configuration file not found: " + configFilePath);
        }
    }
    
    private void validateEnvironment() {
        String dbbHome = System.getenv("DBB_HOME");
        if (dbbHome == null || dbbHome.isEmpty()) {
            exitCode = 8;
            System.err.println("[ERROR] Environment variable 'DBB_HOME' is not set.");
            return;
        }
        
        File dbbBin = new File(dbbHome, "bin/dbb");
        if (!dbbBin.exists()) {
            exitCode = 8;
            System.err.println("[ERROR] The 'dbb' program was not found in DBB_HOME '" + dbbHome + "'.");
        }
        
        // Check git availability
        try {
            Process process = Runtime.getRuntime().exec("git --version");
            int gitExitCode = process.waitFor();
            if (gitExitCode != 0) {
                exitCode = 8;
                System.err.println("[ERROR] The 'git' command is not available.");
            }
        } catch (Exception e) {
            exitCode = 8;
            System.err.println("[ERROR] The 'git' command is not available: " + e.getMessage());
        }
    }
    
    private void validateConfigurationFile() {
        try {
            configProperties = validateAndLoadConfiguration(configFilePath);
        } catch (Exception e) {
            exitCode = 8;
            System.err.println("[ERROR] " + e.getMessage());
        }
    }
    
    private void validateDb2Configuration() {
        String jdbcId = configProperties.getProperty("DBB_MODELER_DB2_METADATASTORE_JDBC_ID");
        if (jdbcId == null || jdbcId.isEmpty()) {
            exitCode = 8;
            System.err.println("[ERROR] The Db2 MetadataStore User is missing from the Configuration file.");
        }
        
        String configFile = configProperties.getProperty("DBB_MODELER_DB2_METADATASTORE_CONFIG_FILE");
        if (configFile == null || configFile.isEmpty()) {
            exitCode = 8;
            System.err.println("[ERROR] The Db2 Connection configuration file is missing from the Configuration file.");
        } else if (!new File(configFile).exists()) {
            exitCode = 8;
            System.err.println("[ERROR] The Db2 Connection configuration file '" + configFile + "' does not exist.");
        }
        
        String passwordFile = configProperties.getProperty("DBB_MODELER_DB2_METADATASTORE_JDBC_PASSWORDFILE");
        if (passwordFile == null || passwordFile.isEmpty()) {
            exitCode = 8;
            System.err.println("[ERROR] The Db2 MetadataStore Password File is missing from the Configuration file.");
        } else if (!new File(passwordFile).exists()) {
            exitCode = 8;
            System.err.println("[ERROR] The Db2 MetadataStore Password File '" + passwordFile + "' does not exist.");
        }
    }
    
    private void validateFileMetadataStore() {
        String fileMetadataStoreDir = configProperties.getProperty("DBB_MODELER_FILE_METADATA_STORE_DIR");
        if (fileMetadataStoreDir == null || fileMetadataStoreDir.isEmpty()) {
            exitCode = 8;
            System.err.println("[ERROR] The location of the DBB File-based MetadataStore must be specified.");
        }
    }
    
    private void validateArtifactRepository() {
        String serverUrl = configProperties.getProperty("ARTIFACT_REPOSITORY_SERVER_URL");
        if (serverUrl == null || serverUrl.isEmpty()) {
            exitCode = 8;
            System.err.println("[ERROR] The URL of the Artifact Repository Server was not specified.");
        } else {
            // Check if server is reachable
            try {
                URL url = new URL(serverUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                int responseCode = connection.getResponseCode();
                
                if (responseCode != 200 && responseCode != 302) {
                    exitCode = 8;
                    System.err.println("[ERROR] The Artifact Repository Server '" + serverUrl + 
                        "' is not reachable. HTTP response code: " + responseCode);
                }
            } catch (Exception e) {
                exitCode = 8;
                System.err.println("[ERROR] The Artifact Repository Server '" + serverUrl + 
                    "' is not reachable: " + e.getMessage());
            }
        }
        
        String user = configProperties.getProperty("ARTIFACT_REPOSITORY_USER");
        if (user == null || user.isEmpty()) {
            exitCode = 8;
            System.err.println("[ERROR] The User for the Artifact Repository Server was not specified.");
        }
        
        String password = configProperties.getProperty("ARTIFACT_REPOSITORY_PASSWORD");
        if (password == null || password.isEmpty()) {
            exitCode = 8;
            System.err.println("[ERROR] The Password of the User for the Artifact Repository Server was not specified.");
        }
        
        String suffix = configProperties.getProperty("ARTIFACT_REPOSITORY_SUFFIX");
        if (suffix == null || suffix.isEmpty()) {
            exitCode = 8;
            System.err.println("[ERROR] The Suffix for Artifact Repositories was not specified.");
        }
    }
    
    private void validateDBBToolkitVersion() {
        validateEnvironment();
        
        if (exitCode != 0) {
            System.err.println("[ERROR] The DBB Toolkit's version could not be verified.");
            return;
        }
        
        try {
            // Read required version from release.properties
            Properties releaseProps = new Properties();
            File releaseFile = new File(modelerHome, "release.properties");
            try (FileInputStream fis = new FileInputStream(releaseFile)) {
                releaseProps.load(fis);
            }
            
            String requiredVersion = releaseProps.getProperty("Minimal-DBB-version");
            if (requiredVersion == null) {
                exitCode = 8;
                System.err.println("[ERROR] Unable to read Minimal-DBB-version from release.properties");
                return;
            }
            
            // Get current DBB version
            String dbbHome = System.getenv("DBB_HOME");
            Process process = Runtime.getRuntime().exec(dbbHome + "/bin/dbb --version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String currentVersion = null;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("Dependency Based Build version")) {
                    String[] parts = line.split("\\s+");
                    currentVersion = parts[parts.length - 1];
                    break;
                }
            }
            
            process.waitFor();
            
            if (currentVersion == null) {
                exitCode = 8;
                System.err.println("[ERROR] Unable to determine current DBB Toolkit version.");
                return;
            }
            
            // Compare versions
            if (!isVersionSufficient(currentVersion, requiredVersion)) {
                exitCode = 8;
                System.err.println("[ERROR] The DBB Toolkit's version is " + currentVersion + 
                    ". The minimal recommended version for the DBB Toolkit is " + requiredVersion + ".");
            }
            
        } catch (Exception e) {
            exitCode = 8;
            System.err.println("[ERROR] Failed to validate DBB Toolkit version: " + e.getMessage());
        }
    }
    
    private boolean isVersionSufficient(String current, String required) {
        String[] currentParts = current.split("\\.");
        String[] requiredParts = required.split("\\.");
        
        for (int i = 0; i < Math.min(currentParts.length, requiredParts.length); i++) {
            int currentNum = Integer.parseInt(currentParts[i]);
            int requiredNum = Integer.parseInt(requiredParts[i]);
            
            if (currentNum < requiredNum) {
                return false;
            } else if (currentNum > requiredNum) {
                return true;
            }
        }
        
        return true; // Versions are equal
    }
    
    private void initializeWorkDirectory() {
        validateConfigurationFile();
        
        if (exitCode != 0) return;
        
        try {
            String workDir = configProperties.getProperty("DBB_MODELER_WORK");
            Path workPath = Paths.get(workDir);
            
            if (Files.exists(workPath)) {
                exitCode = 8;
                System.err.println("[ERROR] Directory '" + workDir + "' already exists.");
                return;
            }
            
            System.out.println("  [INFO] Creating the DBB Git Migration Modeler working folder '" + workDir + "'");
            Files.createDirectories(workPath);
            
            // Create application mappings directory
            String appMappingsDir = configProperties.getProperty("DBB_MODELER_APPMAPPINGS_DIR");
            Path appMappingsPath = Paths.get(appMappingsDir);
            
            if (!Files.exists(appMappingsPath)) {
                System.out.println("  [INFO] Creating the DBB Git Migration Modeler Applications Mappings folder '" + 
                    appMappingsDir + "'");
                Files.createDirectories(appMappingsPath);
            }
            
            // Copy sample files
            copySampleFiles(appMappingsDir);
            
        } catch (IOException e) {
            exitCode = 8;
            System.err.println("[ERROR] Failed to initialize work directory: " + e.getMessage());
        }
    }
    
    private void copySampleFiles(String appMappingsDir) throws IOException {
        // Copy application mappings samples
        System.out.println("  [INFO] Copying sample Applications Mappings files to '" + appMappingsDir + "'");
        copyDirectory(new File(modelerHome, "samples/applications-mapping"), new File(appMappingsDir));
        
        // Copy repository paths mapping
        String repoPathMapping = configProperties.getProperty("REPOSITORY_PATH_MAPPING_FILE");
        System.out.println("  [INFO] Copying sample Repository Paths Mapping file to '" + repoPathMapping + "'");
        Files.createDirectories(Paths.get(repoPathMapping).getParent());
        FileUtility.copyFileWithTags(
            Paths.get(modelerHome, "samples/repositoryPathsMapping.yaml"),
            Paths.get(repoPathMapping)
        );
        
        // Copy types mapping
        String typesMapping = configProperties.getProperty("APPLICATION_TYPES_MAPPING");
        System.out.println("  [INFO] Copying sample Types Mapping file to '" + typesMapping + "'");
        Files.createDirectories(Paths.get(typesMapping).getParent());
        FileUtility.copyFileWithTags(
            Paths.get(modelerHome, "samples/typesMapping.yaml"),
            Paths.get(typesMapping)
        );
        
        // Copy types configurations
        String typesConfig = configProperties.getProperty("TYPE_CONFIGURATIONS_FILE");
        System.out.println("  [INFO] Copying sample Types Configurations file to '" + typesConfig + "'");
        Files.createDirectories(Paths.get(typesConfig).getParent());
        FileUtility.copyFileWithTags(
            Paths.get(modelerHome, "samples/typesConfigurations.yaml"),
            Paths.get(typesConfig)
        );
        
        // Copy application repository configuration
        String defaultAppRepoConfig = configProperties.getProperty("DBB_MODELER_DEFAULT_APP_REPO_CONFIG");
        Path defaultAppRepoPath = Paths.get(defaultAppRepoConfig);
        
        if (!Files.exists(defaultAppRepoPath)) {
            System.out.println("  [INFO] Creating the sample Application Repository Configuration folder '" + 
                defaultAppRepoConfig + "'");
            Files.createDirectories(defaultAppRepoPath);
        }
        
        System.out.println("  [INFO] Copying sample Git Configuration files to '" + defaultAppRepoConfig + "'");
        copyDirectory(new File(modelerHome, "samples/application-repository-configuration"), 
            new File(defaultAppRepoConfig));
    }
    
    private void copyDirectory(File source, File target) throws IOException {
        if (!source.exists()) {
            throw new IOException("Source directory does not exist: " + source);
        }
        
        if (!target.exists()) {
            target.mkdirs();
        }
        
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                File targetFile = new File(target, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, targetFile);
                } else {
                    FileUtility.copyFileWithTags(file, targetFile);
                }
            }
        }
    }
}

// Made with Bob
