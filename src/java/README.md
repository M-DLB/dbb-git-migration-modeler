# ExtractApplications - Java Conversion

This directory contains the Java conversion of the original `extractApplications.groovy` script.

## Overview

The `ExtractApplications` Java application extracts applications from mainframe datasets and generates application configuration files for the DBB Git Migration Modeler. This is a standalone Java executable that replaces the Groovy script with equivalent Java functionality.

## Project Structure

```
src/java/
├── com/ibm/dbb/migration/
│   ├── ExtractApplications.java          # Main executable class
│   ├── model/                             # Data model classes
│   │   ├── ApplicationDescriptor.java
│   │   ├── ApplicationMappingConfiguration.java
│   │   ├── RepositoryPathsMapping.java
│   │   └── TypesMapping.java
│   └── utils/                             # Utility classes
│       ├── ApplicationDescriptorUtils.java
│       └── Logger.java
```

## Key Features

- **Standalone Java Application**: No Groovy runtime required
- **Command-line Interface**: Same arguments as the original Groovy script
- **YAML Support**: Uses SnakeYAML for parsing configuration files
- **z/OS Integration**: Maintains full JZOS and ZFile support
- **DBB Scanner Integration**: Supports file classification via DMH scanner
- **Logging**: File and console logging capabilities

## Prerequisites

1. **Java 8 or higher**
2. **Gradle** (wrapper included, no installation required)
3. **z/OS Environment** with:
   - IBM JZOS libraries
   - IBM DBB installation
   - IBM DMH Scanner libraries

## Key Libraries

- **Apache Commons CLI 1.5.0**: Command-line argument parsing (same as Groovy script)
- **SnakeYAML 2.0**: YAML configuration file parsing

## Building the Application

### Using Gradle (Recommended)

The project includes Gradle Wrapper, so you don't need to install Gradle separately.

```bash
# Build the project (Unix/Linux/macOS)
./gradlew clean build --no-daemon

# Build the project (Windows)
gradlew.bat clean build --no-daemon

# Or use the build script (automatically uses --no-daemon)
./src/java/build.sh

# This creates:
# - build/libs/dbb-git-migration-modeler-1.0.0.jar (main JAR)
# - build/libs/dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar (fat JAR)
# - build/libs/lib/ (runtime dependencies)
```

### Build Outputs

- **Main JAR**: `build/libs/dbb-git-migration-modeler-1.0.0.jar`
  - Requires dependencies in classpath
  
- **Fat JAR**: `build/libs/dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar`
  - Includes all runtime dependencies (except provided scope)
  - Recommended for deployment

### Gradle Tasks

```bash
# Clean build artifacts
./gradlew clean --no-daemon

# Compile Java sources
./gradlew compileJava --no-daemon

# Build JAR files
./gradlew build --no-daemon

# Display project information
./gradlew info --no-daemon

# List all available tasks
./gradlew tasks --no-daemon
```

**Note**: The `--no-daemon` flag is used to prevent daemon processes, which is recommended for z/OS and CI/CD environments.

## Running the Application

### Command-line Syntax

```bash
java -cp <classpath> com.ibm.dbb.migration.ExtractApplications [options]
```

### Options

| Option | Long Form | Required | Description |
|--------|-----------|----------|-------------|
| `-c` | `--configFile` | Yes | Path to the DBB Git Migration Modeler Configuration file |
| `-a` | `--applications` | No | Comma-separated list of applications to extract |
| `-l` | `--logFile` | No | Relative or absolute path to an output log file |
| `-h` | `--help` | No | Display help message |

### Example Usage

```bash
# Extract all applications
java -cp "build/libs/dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar:$DBB_HOME/lib/*:$JZOS_HOME/lib/*" \
  com.ibm.dbb.migration.ExtractApplications \
  -c /path/to/config.properties \
  -l /path/to/extract.log

# Extract specific applications
java -cp "build/libs/dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar:$DBB_HOME/lib/*:$JZOS_HOME/lib/*" \
  com.ibm.dbb.migration.ExtractApplications \
  -c /path/to/config.properties \
  -a "APP1,APP2,APP3" \
  -l /path/to/extract.log
```

### z/OS Execution

On z/OS, you'll need to set up the Java environment properly:

```bash
# Set Java home
export JAVA_HOME=/usr/lpp/java/J8.0_64

# Set DBB home
export DBB_HOME=/var/dbb

# Set classpath
export CLASSPATH="build/libs/dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar:$DBB_HOME/lib/*"

# Run the application
java com.ibm.dbb.migration.ExtractApplications \
  -c /u/config/migration.properties \
  -l /u/logs/extract.log
```

## Configuration File

The configuration file should contain the following properties:

```properties
# Required directories
DBB_MODELER_APPCONFIG_DIR=/path/to/config
DBB_MODELER_APPMAPPINGS_DIR=/path/to/mappings
DBB_MODELER_APPLICATION_DIR=/path/to/applications

# Required files
REPOSITORY_PATH_MAPPING_FILE=/path/to/repositoryPathsMapping.yaml
APPLICATION_TYPES_MAPPING=/path/to/typesMapping.yaml

# Optional settings
SCAN_DATASET_MEMBERS=false
SCAN_DATASET_MEMBERS_ENCODING=IBM-1047
APPLICATION_DEFAULT_BRANCH=main
```

## Differences from Groovy Version

### Advantages

1. **No Groovy Runtime**: Runs on standard Java JVM
2. **Better Performance**: Compiled Java code is generally faster
3. **Type Safety**: Compile-time type checking
4. **IDE Support**: Better tooling and debugging support
5. **Easier Deployment**: Single JAR file deployment

### Compatibility

- **100% Functional Compatibility**: All features from the Groovy script are preserved
- **Same Command-line Interface**: Drop-in replacement for the Groovy script
- **Same Configuration Format**: Uses identical configuration files
- **Same Output Format**: Generates identical application descriptors and mapping files

## Dependencies

### Runtime Dependencies (included in fat JAR)

- **SnakeYAML 2.0**: YAML parsing

### Provided Dependencies (must be in classpath)

- **IBM JZOS**: z/OS file operations
- **IBM DBB**: Build framework utilities
- **IBM DMH Scanner**: File classification

## Troubleshooting

### ClassNotFoundException

Ensure all required libraries are in the classpath:
```bash
export CLASSPATH="app.jar:$DBB_HOME/lib/*:$JZOS_HOME/lib/*"
```

### UnsupportedEncodingException

Ensure the z/OS environment supports the required encodings (IBM-1047, UTF-8).

### File Access Errors

Verify that:
- Configuration directories exist and are accessible
- Dataset names are valid and accessible
- User has appropriate permissions

## Migration from Groovy

To migrate from the Groovy script to the Java version:

1. **Build the Java application**:
   ```bash
   ./gradlew clean build
   # or
   ./src/java/build.sh
   ```

2. **Update your shell scripts** to use Java instead of Groovy:
   ```bash
   # Old (Groovy)
   $DBB_HOME/bin/groovyz src/groovy/extractApplications.groovy -c config.properties
   
   # New (Java)
   java -cp "build/libs/dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar:$DBB_HOME/lib/*" \
     com.ibm.dbb.migration.ExtractApplications -c config.properties
   
   # Or use the wrapper script
   ./src/java/run-extract-applications.sh -c config.properties
   ```

3. **Test with a small dataset** to verify functionality

4. **Deploy to production** once validated

## Support

For issues or questions:
- Review the original Groovy script documentation
- Check the DBB Git Migration Modeler documentation
- Verify all dependencies are properly configured

## License

Licensed Materials - Property of IBM
(c) Copyright IBM Corporation 2018, 2024. All Rights Reserved.