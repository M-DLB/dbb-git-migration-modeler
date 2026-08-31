# DBB Git Migration Modeler — Java Implementation

This directory contains the Java implementation of the DBB Git Migration Modeler.

## Overview

The DBB Git Migration Modeler is a Java application that extracts applications from mainframe datasets, migrates source files to z/OS UNIX System Services folders, assesses cross-application dependencies, generates build configurations, and initializes Git repositories.

All phases of the migration workflow are implemented as Java classes coordinated by the `MigrationOrchestrator` class. The application is invoked through the shell scripts at the root of the repository.

## Project Structure

```
src/java/
└── com/ibm/dbb/migration/
    ├── MigrationOrchestrator.java                    # Main entry point — coordinates all migration phases
    ├── RefreshApplicationDescriptorOrchestrator.java # Entry point — coordinates the refresh workflow
    ├── Setup.java                                    # Interactive setup wizard
    ├── ValidateConfiguration.java                    # Phase 0: configuration validation
    ├── ExtractApplications.java                      # Phase 1: extract applications from datasets
    ├── MigrateDatasets.java                          # Phase 2: migrate members from MVS to USS
    ├── ScanApplication.java                          # Phase 3a: scan application files with DBB scanner
    ├── AssessUsage.java                              # Phase 3b: assess Include File and Program usage
    ├── RecreateApplicationDescriptor.java            # Refresh: reset and rebuild Application Descriptor files
    ├── GenerateZBuilderProperties.java               # Phase 4: generate zBuilder build properties
    ├── InitApplicationRepository.java                # Phase 5: initialize Git repositories
    ├── CalculateDependenciesOrder.java               # Summary: calculate application dependency order
    ├── model/                              # Data model classes
    │   ├── ApplicationDescriptor.java
    │   ├── ApplicationMappingConfiguration.java
    │   ├── RepositoryPathsMapping.java
    │   └── TypesMapping.java
    └── utils/                              # Utility classes
        ├── ApplicationDescriptorRepresenter.java
        ├── ApplicationDescriptorUtils.java
        ├── ConfigurationUtility.java
        ├── FileUtility.java
        ├── Logger.java
        ├── MetadataStoreUtility.java
        └── ZappUtility.java
```

## Key Features

- **Pure Java**: No Groovy runtime required — runs on a standard Java JVM
- **Single JAR deployment**: All phases bundled into one JAR file
- **Command-line Interface**: Consistent `-c`, `-a`, `-l` arguments across all classes
- **YAML Support**: Uses SnakeYAML for parsing configuration files
- **z/OS Integration**: Full JZOS and ZFile support for dataset access
- **DBB Scanner Integration**: Supports file classification via DBB scanner
- **Logging**: Per-phase log files written to `DBB_MODELER_LOGS`

## Prerequisites

1. **Java 21 or higher** (at least version 21.0.11)
2. **Gradle** (wrapper included, no installation required)
3. **z/OS Environment** with:
   - IBM JZOS libraries
   - IBM DBB installation (3.0.4.1 or later)

## Key Libraries

- **Apache Commons CLI 1.5.0**: Command-line argument parsing
- **SnakeYAML 2.0**: YAML configuration file parsing

## Building the Application

### Option 1: Using the Gradle Wrapper

The project includes a Gradle Wrapper, so no separate Gradle installation is required.

```bash
# Build the project (Unix/Linux/z/OS)
./gradlew clean build

# Build the project (Windows)
gradlew.bat clean build
```

### Option 2: Using an Installed Gradle on z/OS (Without Wrapper)

If using a pre-installed Gradle installation on z/OS:

1. Configure environment variables in your shell profile or session:
   ```bash
   export JAVA_HOME=/usr/lpp/java/J21.0_64           # Path to IBM Semeru / 64-bit Java SDK (at least 21.0.11)
   export GRADLE_HOME=/usr/lpp/gradle/gradle-8.5   # Path to installed Gradle
   export PATH=$GRADLE_HOME/bin:$JAVA_HOME/bin:$PATH
   export _BPXK_AUTOCVT=ON
   export _TAG_REDIR_IN=TXT
   export _TAG_REDIR_OUT=TXT
   export _TAG_REDIR_ERR=TXT

   # Bind Gradle daemon communication explicitly to the local loopback interface (required on z/OS when using Gradle daemon)
   export GRADLE_DAEMON_BIND_ADDRESS=127.0.0.1
   ```

2. Invoke Gradle directly:
   ```bash
   gradle clean build
   ```

This creates:
- `build/libs/dbb-git-migration-modeler-2.0.0.jar` — thin JAR (requires classpath)
- `build/libs/lib/` — runtime dependencies

### Gradle Tasks

Replace `./gradlew` with `gradle` if running with an installed Gradle version:

```bash
# Clean build artifacts
./gradlew clean

# Compile Java sources
./gradlew compileJava

# Build JAR files
./gradlew build

# Display project information and usage examples
./gradlew info
```

> **Note**: On z/OS, ensure `GRADLE_DAEMON_BIND_ADDRESS=127.0.0.1` is exported when using the Gradle daemon. If you prefer running without the daemon, pass `--no-daemon`.

## Running the Application

### Recommended: Use the shell scripts

The easiest way to run the application is through the provided shell scripts at the repository root:

```bash
# Run the full migration workflow
./Migration-Modeler-Start.sh -c /path/to/config.properties

# Run with an application filter
./Migration-Modeler-Start.sh -c /path/to/config.properties -a "APP1,APP2"

# Refresh Application Descriptor files for already-migrated applications
./Refresh-Application-Descriptor-Files.sh -c /path/to/config.properties

# Run setup interactively
./Setup.sh
```

### Direct Java invocation

```bash
java -cp "build/libs/dbb-git-migration-modeler-2.0.0.jar:build/libs/lib/*:$DBB_HOME/lib/*" \
  com.ibm.dbb.migration.MigrationOrchestrator \
  -c /path/to/config.properties
```

### Options for MigrationOrchestrator

| Option | Long Form | Required | Description |
|--------|-----------|----------|-------------|
| `-c` | `--configFile` | Yes | Path to the DBB Git Migration Modeler Configuration file |
| `-a` | `--applications` | No | Comma-separated list of applications to process |
| `-h` | `--help` | No | Display help message |

### Running individual phases

Each phase class can also be invoked independently. All phase classes support:

| Option | Long Form | Required | Description |
|--------|-----------|----------|-------------|
| `-c` | `--configFile` | Yes | Path to the DBB Git Migration Modeler Configuration file |
| `-a` | `--applications` | No | Comma-separated list of applications to process (where applicable) |
| `-l` | `--logFile` | No | Relative or absolute path to an output log file |
| `-h` | `--help` | No | Display help message |

```bash
# Example: run Extract Applications phase only
java -cp "build/libs/dbb-git-migration-modeler-2.0.0.jar:build/libs/lib/*:$DBB_HOME/lib/*" \
  com.ibm.dbb.migration.ExtractApplications \
  -c /path/to/config.properties \
  -l /path/to/logs/1-extractApplications.log
```

## Dependencies

### Runtime Dependencies (included in build output under `build/libs/lib/`)

- **SnakeYAML 2.0**: YAML parsing

### Provided Dependencies (must be in classpath at runtime)

- **IBM JZOS**: z/OS file and dataset operations
- **IBM DBB**: Build framework utilities and scanner

## Troubleshooting

### JAR file not found

Run the build before executing:
```bash
./gradlew clean build --no-daemon
```

### ClassNotFoundException

Ensure all required libraries are in the classpath:
```bash
export CLASSPATH="build/libs/dbb-git-migration-modeler-2.0.0.jar:build/libs/lib/*:$DBB_HOME/lib/*"
```

### UnsupportedEncodingException

Ensure the z/OS environment supports the required encodings (IBM-1047, UTF-8).

### File Access Errors

Verify that:
- Configuration directories exist and are accessible
- Dataset names are valid and accessible
- User has appropriate permissions

## License

Licensed Materials - Property of IBM  
(c) Copyright IBM Corporation 2018, 2025. All Rights Reserved.
