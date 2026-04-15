/*
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2018, 2025. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 */

package com.ibm.dbb.migration;

import com.ibm.dbb.migration.model.ApplicationDescriptor;
import com.ibm.dbb.migration.model.TypesMapping;
import com.ibm.dbb.migration.utils.ApplicationDescriptorUtils;
import com.ibm.dbb.migration.utils.Logger;
import org.apache.commons.cli.*;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Generates zAppBuild language configuration properties files for an application.
 * Equivalent to generateZAppBuildProperties.groovy.
 */
public class GenerateZAppBuildProperties {
    
    private Properties props;
    private Logger logger;
    private ApplicationDescriptorUtils appDescUtils;
    private ApplicationDescriptor applicationDescriptor;
    private TypesMapping typesConfigurations;
    
    public static void main(String[] args) {
        GenerateZAppBuildProperties generator = new GenerateZAppBuildProperties();
        try {
            generator.run(args);
        } catch (Exception e) {
            System.err.println("[ERROR] Generation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void run(String[] args) throws Exception {
        props = new Properties();
        logger = new Logger();
        appDescUtils = new ApplicationDescriptorUtils();
        
        // Parse command line arguments
        parseArgs(args);
        
        // Read types configurations
        logger.logMessage("** Reading the Types Configurations definitions from '" + 
            props.getProperty("TYPE_CONFIGURATIONS_FILE") + "'.");
        File typesConfigFile = new File(props.getProperty("TYPE_CONFIGURATIONS_FILE"));
        if (!typesConfigFile.exists()) {
            logger.logMessage("!* [ERROR] the Types Configurations file '" + 
                props.getProperty("TYPE_CONFIGURATIONS_FILE") + "' does not exist. Exiting.");
            System.exit(1);
        }
        
        Yaml yaml = new Yaml();
        try (FileReader reader = new FileReader(typesConfigFile)) {
            typesConfigurations = yaml.loadAs(reader, TypesMapping.class);
        }
        
        // Read application descriptor
        String appDir = props.getProperty("DBB_MODELER_APPLICATION_DIR");
        String application = props.getProperty("application");
        File appDescFile = new File(appDir + "/" + application + "/applicationDescriptor.yml");
        
        if (!appDescFile.exists()) {
            logger.logMessage("!* [ERROR] The Application Descriptor file '" + 
                appDescFile.getPath() + "' does not exist. Exiting.");
            System.exit(1);
        }
        
        applicationDescriptor = appDescUtils.readApplicationDescriptor(appDescFile);
        
        // Setup zAppBuild directories
        String customZAppBuildPath = appDir + "/dbb-zappbuild";
        File originalZAppBuildFolder = new File(props.getProperty("DBB_ZAPPBUILD"));
        
        if (!originalZAppBuildFolder.exists() || !originalZAppBuildFolder.isDirectory()) {
            logger.logMessage("!* [ERROR] The original dbb-zAppBuild folder '" + 
                props.getProperty("DBB_ZAPPBUILD") + "' does not exist or is not a directory. Exiting.");
            System.exit(1);
        }
        
        // Copy zAppBuild if needed
        File customZAppBuildFolder = new File(customZAppBuildPath);
        if (!customZAppBuildFolder.exists()) {
            logger.logMessage("** Copying the zAppBuild from " + originalZAppBuildFolder + 
                " to " + customZAppBuildPath + ".");
            copyDirectory(originalZAppBuildFolder.toPath(), customZAppBuildFolder.toPath());
        }
        
        // Create language configuration folder
        String customLangConfigPath = customZAppBuildPath + "/build-conf/language-conf";
        File customLangConfigFolder = new File(customLangConfigPath);
        if (!customLangConfigFolder.exists()) {
            customLangConfigFolder.mkdirs();
        }
        
        // Setup application-conf directory
        String appConfPath = appDir + "/" + application + "/application-conf";
        String sampleAppConfPath = props.getProperty("DBB_ZAPPBUILD") + "/samples/application-conf";
        File appConfFolder = new File(appConfPath);
        
        if (!appConfFolder.exists()) {
            logger.logMessage("** Copying default application-conf directory to " + appConfPath);
            Files.createDirectories(Paths.get(appConfPath));
            copyDirectory(Paths.get(sampleAppConfPath), Paths.get(appConfPath));
        } else {
            logger.logMessage("** For " + application + " an application-conf directory already exists.");
        }
        
        // Generate language configurations
        logger.logMessage("** Getting the list of files.");
        
        List<FileToLanguageConfig> filesToLanguageConfigs = new ArrayList<>();
        Set<String> typesConfigsToCreate = new HashSet<>();
        Set<String> createdTypesConfigs = new HashSet<>();
        
        logger.logMessage("** Building a list of types to create.");
        
        if (applicationDescriptor.getSources() != null) {
            for (ApplicationDescriptor.Source sourceGroup : applicationDescriptor.getSources()) {
                String repositoryPath = sourceGroup.getRepositoryPath();
                String fileExtension = sourceGroup.getFileExtension();
                
                if (sourceGroup.getFiles() != null) {
                    for (ApplicationDescriptor.FileDef file : sourceGroup.getFiles()) {
                        if (file.getType() != null && !"UNKNOWN".equals(file.getType())) {
                            typesConfigsToCreate.add(file.getType());
                            filesToLanguageConfigs.add(new FileToLanguageConfig(
                                repositoryPath + "/" + file.getName() + "." + fileExtension,
                                file.getType()
                            ));
                        }
                    }
                }
            }
        }
        
        // Generate language configuration files
        if (!typesConfigsToCreate.isEmpty()) {
            logger.logMessage("** Generating/Validating Language Configuration properties files.");
            
            for (String typeToCreate : typesConfigsToCreate) {
                String langConfigFilePath = customLangConfigPath + "/" + typeToCreate + ".properties";
                File langConfigFile = new File(langConfigFilePath);
                
                if (!langConfigFile.exists()) {
                    logger.logMessage("\tGenerating new Language Configuration '" + 
                        langConfigFilePath + "' for type '" + typeToCreate + "'");
                    
                    Properties typeConfigProps = new Properties();
                    TypesMapping.TypeConfiguration matchingTypeConfig = findTypeConfiguration(typeToCreate);
                    
                    if (matchingTypeConfig != null) {
                        if (matchingTypeConfig.getVariables() != null) {
                            for (TypesMapping.Variable variable : matchingTypeConfig.getVariables()) {
                                if (typeConfigProps.containsKey(variable.getName())) {
                                    logger.logMessage("\t[WARNING] Property '" + variable.getName() + 
                                        "' was already found in the '" + typeToCreate + 
                                        "' property files. Overriding.");
                                }
                                typeConfigProps.setProperty(variable.getName(), variable.getValue());
                            }
                        }
                        
                        createdTypesConfigs.add(typeToCreate);
                        
                        // Save properties file
                        try (FileWriter writer = new FileWriter(langConfigFilePath)) {
                            typeConfigProps.store(writer, "Generated by the Migration-Modeler utility");
                        }
                    } else {
                        logger.logMessage("\t[WARNING] No definition found for type '" + typeToCreate + 
                            "' in '" + typesConfigFile.getAbsolutePath() + "'");
                    }
                } else {
                    logger.logMessage("\tFound existing Language Configuration '" + 
                        langConfigFilePath + "' for type '" + typeToCreate + "'");
                    createdTypesConfigs.add(typeToCreate);
                }
            }
        } else {
            logger.logMessage("\t[WARNING] No Type Configuration to create.");
        }
        
        // Generate mapping files
        if (!filesToLanguageConfigs.isEmpty()) {
            String langConfigMappingPath = appConfPath + "/languageConfigurationMapping.properties";
            String filePropertiesPath = appConfPath + "/file.properties";
            
            logger.logMessage("*** Generate the language configuration mapping file " + langConfigMappingPath + ".");
            
            List<String> fileList = new ArrayList<>();
            
            // Write language configuration mapping file
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(langConfigMappingPath), "IBM-1047"))) {
                
                for (FileToLanguageConfig fileToLangConfig : filesToLanguageConfigs) {
                    if (createdTypesConfigs.contains(fileToLangConfig.type)) {
                        writer.println(fileToLangConfig.file + "=" + fileToLangConfig.type);
                        fileList.add(fileToLangConfig.file);
                    } else {
                        logger.logMessage("\t[WARNING] File '" + fileToLangConfig.file + 
                            "' was discarded because no definition was found for type '" + 
                            fileToLangConfig.type + "'");
                    }
                }
            }
            
            // Set file tag for z/OS
            try {
                Runtime.getRuntime().exec("chtag -tc IBM-1047 " + langConfigMappingPath).waitFor();
            } catch (Exception e) {
                // Ignore on non-z/OS systems
            }
            
            // Update file.properties
            logger.logMessage("*** Generate loadLanguageConfigurationProperties configuration in " + 
                filePropertiesPath + ".");
            
            File filePropsFile = new File(filePropertiesPath);
            List<String> filePropsLines = Files.readAllLines(filePropsFile.toPath());
            
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(filePropertiesPath), "IBM-1047"))) {
                
                // Skip first line and existing loadLanguageConfigurationProperties lines
                for (int i = 1; i < filePropsLines.size(); i++) {
                    String[] lineSegments = filePropsLines.get(i).split("=", 2);
                    if (lineSegments.length == 2 && 
                        lineSegments[0].contains("loadLanguageConfigurationProperties")) {
                        // Skip this line
                    } else {
                        writer.println(filePropsLines.get(i));
                    }
                }
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                String currentDate = dateFormat.format(new Date());
                writer.println("\n\n### Generated by the Migration-Modeler utility on " + currentDate + " ###");
                writer.println("# loadLanguageConfigurationProperties=true :: " + String.join(",", fileList));
            }
            
            logger.logMessage("** INFO: Don't forget to enable the use of Language Configuration by " +
                "uncommenting the 'loadLanguageConfigurationProperties' property in '" + filePropertiesPath + "'");
        }
        
        logger.close();
    }
    
    private Options createOptions() {
        Options options = new Options();
        
        options.addOption(Option.builder("a")
            .longOpt("application")
            .hasArg()
            .required()
            .argName("application")
            .desc("Application name")
            .build());
            
        options.addOption(Option.builder("l")
            .longOpt("logFile")
            .hasArg()
            .argName("logFile")
            .desc("Relative or absolute path to an output log file")
            .build());
            
        options.addOption(Option.builder("c")
            .longOpt("configFile")
            .hasArg()
            .required()
            .argName("configFile")
            .desc("Path to the DBB Git Migration Modeler Configuration file")
            .build());
            
        options.addOption(Option.builder("h")
            .longOpt("help")
            .desc("Print this help message")
            .build());
            
        return options;
    }
    
    private void parseArgs(String[] args) throws Exception {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("[ERROR] Error parsing command line: " + e.getMessage());
            formatter.printHelp("GenerateZAppBuildProperties [options]",
                "Generates zAppBuild language configuration properties", 
                options, "", true);
            System.exit(1);
            return;
        }
        
        // Process log file option
        if (cmd.hasOption("l")) {
            String logFile = cmd.getOptionValue("l");
            props.setProperty("logFile", logFile);
            logger.create(logFile);
        }
        
        // Process application option
        if (cmd.hasOption("a")) {
            props.setProperty("application", cmd.getOptionValue("a"));
        } else {
            logger.logMessage("*! [ERROR] The Application name (option -a/--application) must be provided. Exiting.");
            System.exit(1);
        }
        
        // Process configuration file option
        if (cmd.hasOption("c")) {
            String configFilePath = cmd.getOptionValue("c");
            props.setProperty("configurationFilePath", configFilePath);
            
            // Validate and load configuration using ValidateConfiguration
            logger.logMessage("** Validating configuration file...");
            try {
                Properties configuration = ValidateConfiguration.validateAndLoadConfiguration(configFilePath);
                validateAndLoadConfiguration(configuration);
            } catch (Exception e) {
                logger.logMessage("*! [ERROR] Configuration validation failed: " + e.getMessage());
                System.exit(1);
            }
        } else {
            logger.logMessage("*! [ERROR] The path to the DBB Git Migration Modeler Configuration file was not specified. Exiting.");
            System.exit(1);
        }
        
        // Log configuration
        logger.logMessage("** Script configuration:");
        props.forEach((k, v) -> logger.logMessage("\t" + k + " -> " + v));
    }
    
    private void validateAndLoadConfiguration(Properties config) {
        // Validate DBB_MODELER_APPLICATION_DIR
        String appDir = config.getProperty("DBB_MODELER_APPLICATION_DIR");
        if (appDir == null || !new File(appDir).exists()) {
            logger.logMessage("*! [ERROR] The Applications directory must be specified and exist. Exiting.");
            System.exit(1);
        }
        props.setProperty("DBB_MODELER_APPLICATION_DIR", appDir);
        
        // Validate TYPE_CONFIGURATIONS_FILE
        String typeConfigFile = config.getProperty("TYPE_CONFIGURATIONS_FILE");
        if (typeConfigFile == null || !new File(typeConfigFile).exists()) {
            logger.logMessage("*! [ERROR] The Types Configurations file must be specified and exist. Exiting.");
            System.exit(1);
        }
        props.setProperty("TYPE_CONFIGURATIONS_FILE", typeConfigFile);
        
        // Validate DBB_ZAPPBUILD
        String zAppBuildDir = config.getProperty("DBB_ZAPPBUILD");
        if (zAppBuildDir == null || !new File(zAppBuildDir).exists()) {
            logger.logMessage("*! [ERROR] The dbb-zAppBuild instance must be specified and exist. Exiting.");
            System.exit(1);
        }
        props.setProperty("DBB_ZAPPBUILD", zAppBuildDir);
    }
    
    private TypesMapping.TypeConfiguration findTypeConfiguration(String typeToFind) {
        if (typesConfigurations.getTypesConfigurations() != null) {
            return typesConfigurations.getTypesConfigurations().stream()
                .filter(config -> typeToFind.equals(config.getTypeConfiguration()))
                .findFirst()
                .orElse(null);
        }
        return null;
    }
    
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy " + sourcePath, e);
            }
        });
    }
    
    // Helper class for file to language configuration mapping
    private static class FileToLanguageConfig {
        String file;
        String type;
        
        FileToLanguageConfig(String file, String type) {
            this.file = file;
            this.type = type;
        }
    }
}

// Made with Bob
