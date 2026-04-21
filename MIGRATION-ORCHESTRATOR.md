# Migration Orchestrator - Pure Java Implementation

## Overview

The `MigrationOrchestrator` class provides a pure Java implementation that replaces the shell script workflow (`Migration-Modeler-Start-Java.sh`). It orchestrates all migration phases by calling Java classes directly, eliminating the need for intermediate shell scripts.

## Benefits

### Cross-Platform Compatibility
- **No bash dependency**: Runs on Windows, Linux, and z/OS
- **No shell script parsing**: Eliminates platform-specific shell issues
- **Consistent behavior**: Same execution path on all platforms

### Improved Maintainability
- **Single codebase**: All logic in one Java class
- **Type safety**: Compile-time checking vs runtime errors
- **Easier debugging**: Standard Java debugging tools work
- **Better error handling**: Structured exception handling

### Performance
- **No process spawning**: Direct method calls instead of shell processes
- **Reduced overhead**: No intermediate script execution
- **Faster startup**: Single JVM instance

### Integration
- **Embeddable**: Can be called from other Java applications
- **Programmatic control**: Full API access for automation
- **Library usage**: Can be used as a dependency

## Build Artifacts

The build process creates two JAR files:

### 1. Fat JAR (Recommended)
- **File**: `build/libs/dbb-git-migration-modeler-1.0.0-all.jar`
- **Size**: ~512 KB
- **Contents**: Includes runtime dependencies (commons-cli, snakeyaml)
- **Excludes**: DBB and JZOS libraries (must be provided externally)
- **Usage**: Requires DBB_HOME in classpath

### 2. Thin JAR
- **File**: `build/libs/dbb-git-migration-modeler-1.0.0.jar`
- **Size**: ~125 KB
- **Contents**: Only project classes
- **Dependencies**: Requires `build/libs/lib/` directory with dependencies
- **Usage**: When you want to manage dependencies separately

## Usage

### Command Line (Fat JAR - Recommended)

**Important**: DBB libraries must be added to classpath as they are not included in the JAR.

```bash
# On Unix/Linux/z/OS - Full migration workflow
java -cp "build/libs/dbb-git-migration-modeler-1.0.0-all.jar:$DBB_HOME/lib/*" \
  com.ibm.dbb.migration.MigrationOrchestrator \
  -c /path/to/config.properties

# With application filter
java -cp "build/libs/dbb-git-migration-modeler-1.0.0-all.jar:$DBB_HOME/lib/*" \
  com.ibm.dbb.migration.MigrationOrchestrator \
  -c /path/to/config.properties \
  -a APP1,APP2,APP3

# On Windows
java -cp "build/libs/dbb-git-migration-modeler-1.0.0-all.jar;%DBB_HOME%/lib/*" ^
  com.ibm.dbb.migration.MigrationOrchestrator ^
  -c C:\path\to\config.properties

# Display help
java -cp "build/libs/dbb-git-migration-modeler-1.0.0-all.jar:$DBB_HOME/lib/*" \
  com.ibm.dbb.migration.MigrationOrchestrator -h
```

### Command Line (Thin JAR with Classpath)

```bash
# On Unix/Linux/z/OS
java -cp "build/libs/dbb-git-migration-modeler-1.0.0.jar:build/libs/lib/*:$DBB_HOME/lib/*" \
  com.ibm.dbb.migration.MigrationOrchestrator \
  -c /path/to/config.properties

# On Windows
java -cp "build/libs/dbb-git-migration-modeler-1.0.0.jar;build/libs/lib/*;%DBB_HOME%/lib/*" ^
  com.ibm.dbb.migration.MigrationOrchestrator ^
  -c C:\path\to\config.properties
```

### Programmatic Usage

```java
import com.ibm.dbb.migration.MigrationOrchestrator;

public class MyMigrationApp {
    public static void main(String[] args) {
        MigrationOrchestrator orchestrator = new MigrationOrchestrator();
        
        // Run with configuration file
        String[] migrationArgs = {
            "-c", "/path/to/config.properties",
            "-a", "APP1,APP2"
        };
        
        orchestrator.run(migrationArgs);
        
        // Check exit code
        int exitCode = orchestrator.getExitCode();
        if (exitCode != 0) {
            System.err.println("Migration failed with exit code: " + exitCode);
        }
    }
}
```

## Migration Phases

The orchestrator executes the following phases in order:

### Phase 0: Cleanup Working Directories
- Removes previous migration artifacts
- Creates fresh working directories
- Prepares logs directory

### Phase 1: Extract Applications
- Reads application mapping files
- Creates application descriptors
- Generates DBB migration mapping files
- **Java Class**: `ExtractApplications`

### Phase 2: Run Migrations
- Executes DBB migration utility for each application
- Migrates datasets to USS file system
- Preserves file encodings and attributes
- **Java Class**: `MigrateDatasets`

### Phase 3: Classify and Assess Usage
- Scans application files for dependencies
- Analyzes include file and program usage
- Moves files to owning applications
- Updates application descriptors
- **Java Classes**: `ScanApplication`, `AssessUsage`

### Phase 4: Generate Build Properties
- Creates zBuilder configuration files
- Generates type-specific build properties
- Sets up build framework configuration
- **Java Class**: `GenerateZBuilderProperties`

### Phase 5: Initialize Application Repositories
- Initializes Git repositories
- Creates baseline references
- Sets up IDE project files
- Configures Git attributes
- **Java Class**: `InitApplicationRepository`

### Phase 6: Print Summary
- Displays dependency order
- Shows next steps for Git platform migration
- Provides pipeline configuration guidance
- **Java Class**: `CalculateDependenciesOrder`

## Interactive Mode

When `INTERACTIVE_RUN=true` in the configuration file, the orchestrator prompts for confirmation before each phase:

```
[PHASE] Cleanup working directories
Do you want to clean the working directory '/path/to/work' (Y/n): Y

[PHASE] Extract applications from Applications Mapping files
Do you want to run the application extraction (Y/n): Y

[PHASE] Execute migrations using DBB Migration mapping files
Do you want to execute the migration (Y/n): Y

[PHASE] Assess usage and perform classification
Do you want to perform usage assessment (Y/n): Y

[PHASE] Generate build configuration
Do you want to generate zBuilder configuration (Y/n): Y

[PHASE] Initialize application repositories
Do you want to initialize repositories (Y/n): Y
```

Press Enter or type 'Y' to proceed, or 'n' to skip a phase.

## Application Filtering

Use the `-a` option to process only specific applications:

```bash
# Process single application
java -jar dbb-git-migration-modeler-1.0.0-all.jar -c config.properties -a PAYROLL

# Process multiple applications (comma-separated, no spaces)
java -jar dbb-git-migration-modeler-1.0.0-all.jar -c config.properties -a PAYROLL,BILLING,INVENTORY

# Process all applications (omit -a option)
java -jar dbb-git-migration-modeler-1.0.0-all.jar -c config.properties
```

## Configuration File

The orchestrator reads all settings from the configuration file specified with `-c`:

```properties
# Required settings
DBB_MODELER_HOME=/path/to/dbb-git-migration-modeler
DBB_MODELER_WORK=/path/to/work
DBB_MODELER_APPCONFIG_DIR=/path/to/work/migration-configuration
DBB_MODELER_APPLICATION_DIR=/path/to/work/repositories
DBB_MODELER_LOGS=/path/to/work/logs
DBB_MODELER_BUILD_CONFIGURATION=/path/to/work/build-configuration
DBB_MODELER_APPMAPPINGS_DIR=/path/to/applications-mappings

# Metadata store configuration
DBB_MODELER_METADATASTORE_TYPE=file
DBB_MODELER_FILE_METADATA_STORE_DIR=/path/to/work/dbb-filemetadatastore

# Build framework
BUILD_FRAMEWORK=zBuilder

# Pipeline configuration
PIPELINE_CI=GitHubActions

# Interactive mode
INTERACTIVE_RUN=false

# Other settings...
```

## Exit Codes

- **0**: Success - all phases completed successfully
- **4**: Invalid command-line arguments
- **8**: Configuration error or phase failure

## Logging

Each phase creates detailed log files in the `DBB_MODELER_LOGS` directory:

```
logs/
├── 2-APP1.migration.log          # Migration phase
├── 3-APP1-scan.log                # Scan phase
├── 3-APP1-assessUsage.log         # Assessment phase
├── 4-APP1-generateProperties.log  # Property generation
└── 5-APP1-initApplicationRepository.log  # Repository initialization
```

## Comparison with Shell Script

| Feature | Shell Script | Java Orchestrator |
|---------|-------------|-------------------|
| Platform | Unix/Linux/z/OS only | All platforms |
| Dependencies | bash, grep, awk, sed | Java 8+ only |
| Process overhead | High (multiple processes) | Low (single JVM) |
| Error handling | Basic | Comprehensive |
| Debugging | Difficult | Standard Java tools |
| Integration | Limited | Full API access |
| Deployment | Multiple files | Single JAR |

## Building

```bash
# Build both thin and fat JARs
./gradlew clean build

# Build only fat JAR
./gradlew fatJar

# Display build information
./gradlew info
```

## Troubleshooting

### NoSuchElementException During Interactive Prompts

**Problem**: `java.util.NoSuchElementException: No line found` when running the orchestrator

**Cause:**
- Running in a non-interactive environment (e.g., background job, CI/CD pipeline)
- Standard input is closed or redirected
- Input stream exhausted after first prompt

**Solution:**
The orchestrator now handles this gracefully by:
- Reusing a single Scanner instance across all prompts
- Detecting when input is unavailable and defaulting to "Yes"
- Catching exceptions and proceeding with safe defaults

To avoid prompts entirely, set `INTERACTIVE_RUN=false` in your configuration file:
```properties
INTERACTIVE_RUN=false
```

### ClassNotFoundException for Apache Commons

**Problem**: `java.lang.NoClassDefFoundError: org.apache.commons.cli.ParseException`

**Solution**: Use the fat JAR (`-all.jar`) which includes commons-cli and snakeyaml:
```bash
java -cp "build/libs/dbb-git-migration-modeler-1.0.0-all.jar:$DBB_HOME/lib/*" \
  com.ibm.dbb.migration.MigrationOrchestrator -c config.properties
```

### DBB Classes Not Found

**Problem**: `NoClassDefFoundError` for DBB classes (e.g., `com.ibm.dbb.metadata.*`)

**Solution**: DBB libraries are NOT included in the JAR. You must add them to the classpath:

```bash
# Set DBB_HOME environment variable
export DBB_HOME=/var/dbb

# Add DBB libraries to classpath
java -cp "build/libs/dbb-git-migration-modeler-1.0.0-all.jar:$DBB_HOME/lib/*" \
  com.ibm.dbb.migration.MigrationOrchestrator -c config.properties
```

**Why**: DBB is a provided dependency that must be installed separately on the system.

### JZOS Classes Not Found (z/OS only)

**Problem**: `NoClassDefFoundError` for JZOS classes

**Solution**: Add JZOS to classpath:
```bash
java -cp "dbb-git-migration-modeler-1.0.0-all.jar:/usr/lpp/IBM/izoda/v1r1/IBM/jzos/*" \
  com.ibm.dbb.migration.MigrationOrchestrator -c config.properties
```

## Migration from Shell Script

To migrate from the shell script to the Java orchestrator:

**Before (Shell Script):**
```bash
./src/scripts/Migration-Modeler-Start-Java.sh \
  -c /path/to/config.properties \
  -a APP1,APP2
```

**After (Java Orchestrator):**
```bash
java -cp "build/libs/dbb-git-migration-modeler-1.0.0-all.jar:$DBB_HOME/lib/*" \
  com.ibm.dbb.migration.MigrationOrchestrator \
  -c /path/to/config.properties \
  -a APP1,APP2
```

Both approaches produce identical results, but the Java orchestrator offers:
- Better cross-platform support
- Easier deployment (single file)
- Improved error handling
- Programmatic integration capabilities

## See Also

- [Main README](README.md) - Project overview and setup
- [Usage Guide](USAGE.md) - Detailed usage instructions
- [Configuration Guide](docs/03-Configuration.md) - Configuration file reference