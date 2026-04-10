# DBB Git Migration Modeler - Java Tools Usage Guide

## Overview

This guide explains how to use the Java-converted migration tools. All three tools are packaged in a single JAR file with their dependencies in a separate lib directory.

## Build Output

After running `gradlew build`, you will have:
- **Main JAR**: `build/libs/dbb-git-migration-modeler-1.0.0.jar` (contains all three tools)
- **Dependencies**: `build/libs/lib/` (runtime dependencies)

## Available Tools

The JAR contains three main classes:

1. **ExtractApplications** - Extract applications from datasets to Git repositories
2. **MigrateDatasets** - Migrate datasets with encoding detection
3. **ScanApplication** - Scan files for dependencies

## Usage

### General Syntax

```bash
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* <MainClass> [options]
```

**On Windows**, use semicolon (`;`) instead of colon (`:`) for classpath separator:
```cmd
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar;build/libs/lib/* <MainClass> [options]
```

### 1. ExtractApplications

Extract applications from z/OS datasets to Git repositories.

**Syntax:**
```bash
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* \
  com.ibm.dbb.migration.ExtractApplications \
  -c <config-file> \
  [-l <log-file>]
```

**Options:**
- `-c, --configFile` - Path to DBB Git Migration Modeler configuration file (required)
- `-l, --logFile` - Path to output log file (optional)

**Example:**
```bash
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* \
  com.ibm.dbb.migration.ExtractApplications \
  -c /u/user/config/migration-config.properties \
  -l /u/user/logs/extract.log
```

### 2. MigrateDatasets

Migrate datasets to Git repositories with character encoding detection.

**Syntax:**
```bash
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* \
  com.ibm.dbb.migration.MigrateDatasets \
  -a <application-name> \
  -c <config-file> \
  [-l <log-file>]
```

**Options:**
- `-a, --application` - Application name (required)
- `-c, --configFile` - Path to DBB Git Migration Modeler configuration file (required)
- `-l, --logFile` - Path to output log file (optional)

**Example:**
```bash
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* \
  com.ibm.dbb.migration.MigrateDatasets \
  -a MYAPP \
  -c /u/user/config/migration-config.properties \
  -l /u/user/logs/migrate-myapp.log
```

### 3. ScanApplication

Scan application files for dependencies and store in metadata store.

**Syntax:**
```bash
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* \
  com.ibm.dbb.migration.ScanApplication \
  -a <application-name> \
  -c <config-file> \
  [-l <log-file>]
```

**Options:**
- `-a, --application` - Application name (required)
- `-c, --configFile` - Path to DBB Git Migration Modeler configuration file (required)
- `-l, --logFile` - Path to output log file (optional)

**Example:**
```bash
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* \
  com.ibm.dbb.migration.ScanApplication \
  -a MYAPP \
  -c /u/user/config/migration-config.properties \
  -l /u/user/logs/scan-myapp.log
```

## Configuration File

All tools require a configuration file with the following properties:

### Required Properties

```properties
# Directories
DBB_MODELER_APPLICATION_DIR=/path/to/applications
DBB_MODELER_LOGS=/path/to/logs
DBB_MODELER_BUILD_CONFIGURATION=/path/to/build-config

# Configuration files
REPOSITORY_PATH_MAPPING_FILE=/path/to/repositoryPathsMapping.yaml
APPLICATION_TYPES_MAPPING=/path/to/typesMapping.yaml
TYPE_CONFIGURATIONS_FILE=/path/to/typesConfigurations.yaml

# Metadata store (choose one)
DBB_MODELER_METADATASTORE_TYPE=file
DBB_MODELER_FILE_METADATA_STORE_DIR=/path/to/metadatastore

# OR for DB2
DBB_MODELER_METADATASTORE_TYPE=db2
DBB_MODELER_DB2_METADATASTORE_JDBC_ID=userid
DBB_MODELER_DB2_METADATASTORE_CONFIG_FILE=/path/to/db2Connection.conf
DBB_MODELER_DB2_METADATASTORE_JDBC_PASSWORDFILE=/path/to/password.txt

# Application settings
APPLICATION_DEFAULT_BRANCH=main
SCAN_CONTROL_TRANSFERS=true

# Dataset settings (for ExtractApplications)
APPLICATION_ARTIFACTS_HLQ=EXTRACT.PROD
SCAN_DATASET_MEMBERS=false
SCAN_DATASET_MEMBERS_ENCODING=IBM-1047
```

## Migration Workflow

Use the tools in this sequence:

```
1. ExtractApplications
   ↓ Extracts datasets to Git repositories
   ↓ Generates application descriptors
   ↓
2. MigrateDatasets (optional)
   ↓ Migrates additional datasets
   ↓ Handles encoding and character detection
   ↓
3. ScanApplication
   ↓ Scans files for dependencies
   ↓ Stores results in metadata store
   ↓
4. Build Process
   Uses dependency information for builds
```

## Platform-Specific Notes

### On z/OS

```bash
# Set up environment
export JAVA_HOME=/usr/lpp/java/J8.0_64
export PATH=$JAVA_HOME/bin:$PATH

# Run tool
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* \
  com.ibm.dbb.migration.ExtractApplications \
  -c /u/user/config/migration-config.properties
```

### On Windows

```cmd
REM Set up environment
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_xxx
set PATH=%JAVA_HOME%\bin;%PATH%

REM Run tool (note semicolon in classpath)
java -cp build\libs\dbb-git-migration-modeler-1.0.0.jar;build\libs\lib\* ^
  com.ibm.dbb.migration.ExtractApplications ^
  -c C:\config\migration-config.properties
```

## Troubleshooting

### ClassNotFoundException

**Problem**: `java.lang.ClassNotFoundException: com.ibm.dbb.migration.ExtractApplications`

**Solution**: Ensure you're using the correct classpath with both the main JAR and lib directory:
```bash
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* ...
```

### NoClassDefFoundError for DBB classes

**Problem**: `java.lang.NoClassDefFoundError: com/ibm/dbb/dependency/DependencyScanner`

**Solution**: Ensure DBB libraries are available:
- On z/OS: Set `DBB_HOME` environment variable
- On Windows: Copy DBB JARs to `lib/dbb/` directory

### Configuration File Not Found

**Problem**: `The DBB Git Migration Modeler Configuration file 'xxx' does not exist`

**Solution**: Verify the path to the configuration file is correct and the file exists.

### Application Directory Not Found

**Problem**: `Application Directory 'xxx' does not exist`

**Solution**: Ensure `DBB_MODELER_APPLICATION_DIR` is set correctly in the configuration file.

## Building from Source

### Prerequisites
- Java 8 or higher
- Gradle (or use included wrapper)

### Build Commands

```bash
# Clean and build
./gradlew clean build --no-daemon

# View build information
./gradlew info --no-daemon

# Clean only
./gradlew clean --no-daemon
```

### Build Output Location
- JAR: `build/libs/dbb-git-migration-modeler-1.0.0.jar`
- Dependencies: `build/libs/lib/`
- Distributions: `build/distributions/`

## Getting Help

### Command-Line Help

Each tool provides help when run without required arguments:

```bash
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* \
  com.ibm.dbb.migration.ExtractApplications
```

This will display usage information and available options.

### Documentation

- [Conversion Summary](CONVERSION-SUMMARY.md) - Overview of all conversions
- [ExtractApplications Details](JAVA_CONVERSION_SUMMARY.md)
- [MigrateDatasets Details](MIGRATE-DATASETS-CONVERSION.md)
- [ScanApplication Details](SCAN-APPLICATION-CONVERSION.md)
- [Gradle Wrapper Guide](GRADLE-WRAPPER-GUIDE.md)

## Examples

### Complete Migration Example

```bash
# 1. Extract applications
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* \
  com.ibm.dbb.migration.ExtractApplications \
  -c /u/user/config/migration-config.properties \
  -l /u/user/logs/extract.log

# 2. Migrate additional datasets (if needed)
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* \
  com.ibm.dbb.migration.MigrateDatasets \
  -a MYAPP \
  -c /u/user/config/migration-config.properties \
  -l /u/user/logs/migrate-myapp.log

# 3. Scan for dependencies
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* \
  com.ibm.dbb.migration.ScanApplication \
  -a MYAPP \
  -c /u/user/config/migration-config.properties \
  -l /u/user/logs/scan-myapp.log
```

### Batch Processing Multiple Applications

```bash
#!/bin/bash
APPS="APP1 APP2 APP3"
CONFIG="/u/user/config/migration-config.properties"
LOGDIR="/u/user/logs"

for APP in $APPS; do
  echo "Processing $APP..."
  
  # Migrate
  java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* \
    com.ibm.dbb.migration.MigrateDatasets \
    -a $APP -c $CONFIG -l $LOGDIR/migrate-$APP.log
  
  # Scan
  java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/* \
    com.ibm.dbb.migration.ScanApplication \
    -a $APP -c $CONFIG -l $LOGDIR/scan-$APP.log
done
```

---

**Version**: 1.0.0  
**Last Updated**: 2026-04-10  
**Java Version**: 1.8+