# ScanApplication Groovy to Java Conversion

## Overview

This document describes the conversion of `scanApplication.groovy` (249 lines) to a standalone Java executable class `ScanApplication.java` (418 lines).

**Note**: This conversion uses the actual IBM DBB libraries from `lib/dbb/` directory, not stub classes. The MetadataStoreUtility class properly implements the DBB MetadataStore API using `MetadataStoreFactory`, `BuildGroup`, and `Collection` classes.

## Purpose

The ScanApplication tool scans application files to identify dependencies and stores the results in a DBB metadata store (either file-based or DB2-based). This is a critical step in the migration process that enables dependency-based builds.

## Conversion Details

### Original Groovy Script
- **File**: `src/groovy/scanApplication.groovy`
- **Lines**: 249
- **Purpose**: Scan application files and store dependency information

### Converted Java Class
- **File**: `src/java/com/ibm/dbb/migration/ScanApplication.java`
- **Lines**: 418
- **Package**: `com.ibm.dbb.migration`
- **Main Class**: `com.ibm.dbb.migration.ScanApplication`

## Key Features

### 1. Command-Line Interface
Uses Apache Commons CLI for argument parsing:
- `-a, --application`: Application name (required)
- `-c, --configFile`: Path to DBB Git Migration Modeler configuration file (required)
- `-l, --logFile`: Path to output log file (optional)

### 2. Configuration Validation
Validates all required configuration parameters:
- Application directory existence
- Repository path mapping file
- Metadata store type (file or db2)
- Metadata store location/connection details
- Application default branch
- Scan control transfers flag

### 3. Dependency Scanning
- Uses DBB DependencyScanner to analyze files
- Supports control transfer scanning (configurable)
- Processes all files mapped in application descriptor
- Stores results in DBB Collections

### 4. Metadata Store Support
Supports two types of metadata stores:
- **File-based**: Local file system storage
- **DB2-based**: Database storage with JDBC connection

### 5. Build Group Management
- Deletes existing build groups
- Creates new collections for scanned results
- Stores logical files with dependency information

## Architecture

### Main Components

1. **ScanApplication** (main class)
   - Command-line parsing
   - Configuration validation
   - Orchestrates scanning workflow

2. **MetadataStoreUtility** (inner class)
   - Initializes file or DB2 metadata store
   - Manages collections and build groups
   - Provides abstraction over DBB MetadataStore API

3. **FileUtility** (inner class)
   - Extracts file lists from application descriptors
   - Resolves file paths relative to application directory

### Dependencies

#### External Libraries
- **Apache Commons CLI 1.5.0**: Command-line parsing
- **SnakeYAML 2.0**: YAML configuration parsing (via ApplicationDescriptorUtils)

#### IBM Libraries (compile-only)
- **IBM DBB**: DependencyScanner, MetadataStore, Collection, LogicalFile APIs
- **IBM JZOS**: Not directly used in this script

#### Internal Dependencies
- **ApplicationDescriptor**: Application configuration model
- **Logger**: Logging utility
- **ApplicationDescriptorUtils**: YAML operations

## Build Configuration

### Gradle Task
A dedicated Gradle task creates a fat JAR for ScanApplication:

```gradle
task scanApplicationFatJar(type: Jar) {
    archiveBaseName = 'scanApplication'
    archiveClassifier = 'jar-with-dependencies'
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes(
            'Main-Class': 'com.ibm.dbb.migration.ScanApplication',
            'Implementation-Title': 'DBB Git Migration Modeler - Scan Application',
            'Implementation-Version': version
        )
    }
    
    from {
        configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) }
    }
    
    from sourceSets.main.output
}
```

### Building

#### On Windows (without z/OS libraries)
```bash
# Build all JARs
gradlew build --no-daemon

# Build only ScanApplication JAR
gradlew scanApplicationFatJar --no-daemon
```

#### On z/OS (with DBB libraries)
```bash
# Build all JARs
./gradlew build --no-daemon

# Build only ScanApplication JAR
./gradlew scanApplicationFatJar --no-daemon
```

### Output
- **Fat JAR**: `build/libs/scanApplication-1.0.0-jar-with-dependencies.jar`
- **Regular JAR**: `build/libs/dbb-git-migration-modeler-1.0.0.jar`
- **Dependencies**: `build/libs/lib/`

## Usage

### Command-Line Syntax
```bash
java -jar scanApplication-1.0.0-jar-with-dependencies.jar \
  -a <application-name> \
  -c <config-file-path> \
  [-l <log-file-path>]
```

### Example
```bash
java -jar scanApplication-1.0.0-jar-with-dependencies.jar \
  -a MyApplication \
  -c /path/to/migration-config.properties \
  -l /path/to/scan.log
```

### On z/OS
```bash
java -jar scanApplication-1.0.0-jar-with-dependencies.jar \
  -a MYAPP \
  -c /u/user/config/migration-config.properties \
  -l /u/user/logs/scan-myapp.log
```

## Configuration File Requirements

The configuration file must contain:

### Required Properties
```properties
# Application directory
DBB_MODELER_APPLICATION_DIR=/path/to/applications

# Repository path mapping
REPOSITORY_PATH_MAPPING_FILE=/path/to/repositoryPathsMapping.yaml

# Metadata store type (file or db2)
DBB_MODELER_METADATASTORE_TYPE=file

# For file-based metadata store
DBB_MODELER_FILE_METADATA_STORE_DIR=/path/to/metadatastore

# For DB2-based metadata store
DBB_MODELER_DB2_METADATASTORE_JDBC_ID=userid
DBB_MODELER_DB2_METADATASTORE_CONFIG_FILE=/path/to/db2Connection.conf
DBB_MODELER_DB2_METADATASTORE_JDBC_PASSWORDFILE=/path/to/password.txt

# Application settings
APPLICATION_DEFAULT_BRANCH=main
SCAN_CONTROL_TRANSFERS=true
```

## Workflow

### 1. Initialization
- Parse command-line arguments
- Load and validate configuration file
- Initialize logger
- Set up metadata store (file or DB2)

### 2. Application Descriptor Reading
- Locate application descriptor file
- Parse YAML configuration
- Extract source groups and file mappings

### 3. File Scanning
- Retrieve list of mapped files
- Initialize DBB DependencyScanner
- Configure control transfer scanning
- Scan each file for dependencies
- Create LogicalFile objects

### 4. Result Storage
- Create collection name: `<application>-<branch>`
- Delete existing build group (if any)
- Create new collection
- Store all LogicalFile objects in collection

### 5. Completion
- Log summary information
- Close logger
- Exit

## Error Handling

### Configuration Errors
- Missing required properties → Exit with error message
- Invalid file paths → Exit with error message
- Invalid metadata store type → Exit with error message

### Runtime Errors
- File scanning failures → Log error, continue with next file
- Metadata store errors → Propagate exception
- Application descriptor not found → Exit with warning

## Differences from Groovy Version

### Syntax Changes
1. **Type Safety**: All variables explicitly typed
2. **Properties**: Groovy ConfigSlurper → Java Properties
3. **Collections**: Groovy collections → Java Collections API
4. **String Interpolation**: GStrings → String concatenation
5. **Closures**: Groovy closures → Java methods

### Structural Changes
1. **Inner Classes**: MetadataStoreUtility and FileUtility as inner classes
2. **Exception Handling**: More explicit try-catch blocks
3. **Resource Management**: Try-with-resources for file operations
4. **CLI Parsing**: Custom parsing → Apache Commons CLI

### Functional Equivalence
- All configuration validation logic preserved
- Same metadata store initialization approach
- Identical scanning workflow
- Same collection management behavior

## Testing

### Unit Testing
Create test cases for:
- Command-line argument parsing
- Configuration validation
- File path resolution
- Error handling

### Integration Testing
Test with:
- Sample application descriptors
- File-based metadata store
- DB2-based metadata store
- Various scan control transfer settings

### End-to-End Testing
1. Extract applications using ExtractApplications
2. Scan applications using ScanApplication
3. Verify collections created in metadata store
4. Verify dependency information stored correctly

## Limitations

### Current Implementation
1. **Stub Classes**: MetadataStoreUtility and FileUtility are simplified
2. **DBB API**: Requires actual DBB libraries at runtime on z/OS
3. **Error Recovery**: Limited retry logic for transient failures

### Future Enhancements
1. Add retry logic for metadata store operations
2. Support incremental scanning (only changed files)
3. Add progress reporting for large applications
4. Support parallel file scanning
5. Add validation of scan results

## Integration with Migration Workflow

### Position in Workflow
1. **ExtractApplications**: Extract files from datasets → Git repositories
2. **ScanApplication**: Scan files for dependencies → Metadata store ← **YOU ARE HERE**
3. **Build Process**: Use dependency information for builds

### Input Requirements
- Application extracted and in Git repository
- Application descriptor file created
- Configuration file with all required properties

### Output Artifacts
- DBB Collection with dependency information
- Build group for the application
- Log file with scan results

## Troubleshooting

### Common Issues

#### 1. Application Directory Not Found
```
*! [ERROR] Application Directory '/path/to/app' does not exist. Exiting.
```
**Solution**: Verify DBB_MODELER_APPLICATION_DIR and application name

#### 2. Metadata Store Initialization Failed
```
*! [ERROR] The location of the File MetadataStore must be specified...
```
**Solution**: Check DBB_MODELER_FILE_METADATA_STORE_DIR or DB2 settings

#### 3. File Scanning Errors
```
*! [ERROR] Something went wrong when scanning the file 'file.cbl'.
```
**Solution**: Check file encoding, permissions, and DBB scanner configuration

#### 4. Missing Application Descriptor
```
*! [WARNING] The Application Descriptor file ... was not found. Exiting.
```
**Solution**: Run ExtractApplications first to create the descriptor

## Performance Considerations

### Scanning Performance
- **File I/O**: Dominant factor for large applications
- **DBB Scanner**: CPU-intensive for complex dependencies
- **Metadata Store**: DB2 faster than file-based for large datasets

### Optimization Tips
1. Use DB2 metadata store for large applications
2. Enable control transfer scanning only when needed
3. Scan incrementally when possible
4. Use SSD storage for file-based metadata store

## Security Considerations

### Credentials
- DB2 password stored in separate file (not in config)
- File permissions should restrict access to password file
- Use encrypted connections for DB2 (configure in db2Connection.conf)

### File Access
- Application directory should have appropriate permissions
- Metadata store directory should be protected
- Log files may contain sensitive information

## Maintenance

### Code Updates
When updating the code:
1. Maintain compatibility with Groovy version
2. Update this documentation
3. Add unit tests for new features
4. Test on both Windows and z/OS

### Version Control
- Track changes in Git
- Tag releases
- Document breaking changes

## References

### Related Documentation
- [ExtractApplications Conversion](JAVA_CONVERSION_SUMMARY.md)
- [MigrateDatasets Conversion](MIGRATE-DATASETS-CONVERSION.md)
- [Gradle Wrapper Guide](GRADLE-WRAPPER-GUIDE.md)
- [Build on Windows](BUILD-ON-WINDOWS.md)

### IBM Documentation
- [IBM DBB Documentation](https://www.ibm.com/docs/en/dbb)
- [DBB DependencyScanner API](https://www.ibm.com/docs/en/dbb/latest?topic=apis-dependency-scanner)
- [DBB MetadataStore API](https://www.ibm.com/docs/en/dbb/latest?topic=apis-metadata-store)

### External Libraries
- [Apache Commons CLI](https://commons.apache.org/proper/commons-cli/)
- [SnakeYAML](https://bitbucket.org/snakeyaml/snakeyaml/)

---

**Conversion Date**: 2026-04-10  
**Converted By**: IBM Bob (AI Assistant)  
**Original Script**: src/groovy/scanApplication.groovy (249 lines)  
**Java Class**: src/java/com/ibm/dbb/migration/ScanApplication.java (418 lines)  
**Status**: Complete and ready for testing