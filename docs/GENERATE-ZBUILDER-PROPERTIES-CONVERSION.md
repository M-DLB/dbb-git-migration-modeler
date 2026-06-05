# GenerateZBuilderProperties Groovy to Java Conversion

## Overview

This document details the conversion of `generateZBuilderProperties.groovy` to `GenerateZBuilderProperties.java`. This script generates zBuilder configuration files including the main `dbb-app.yaml` application configuration and language-specific configuration YAML files.

## Original Groovy Script

**Location**: `src/groovy/generateZBuilderProperties.groovy`
**Lines**: 437
**Purpose**: Generate zBuilder configuration files for DBB builds

### Key Responsibilities

1. **Language Configuration Generation**: Creates YAML configuration files for each type (COBOL, ASM, etc.)
2. **Application Configuration**: Generates `dbb-app.yaml` with task definitions and file mappings
3. **Dependency Configuration**: Sets up dependency search paths for include files and macros
4. **Impact Analysis Configuration**: Configures impact query patterns for change detection

## Java Implementation

**Location**: `src/java/com/ibm/dbb/migration/GenerateZBuilderProperties.java`
**Lines**: 672
**Package**: `com.ibm.dbb.migration`

### Architecture

```
GenerateZBuilderProperties
├── Command-line argument parsing (Apache Commons CLI)
├── Configuration validation
├── Types configuration reading (SnakeYAML)
├── Application descriptor reading
├── Language configuration generation
├── Application YAML generation
└── Dependency configuration generation
```

### Key Components

#### 1. Main Entry Point

```java
public static void main(String[] args) {
    GenerateZBuilderProperties generator = new GenerateZBuilderProperties();
    try {
        generator.run(args);
    } catch (Exception e) {
        System.err.println("[ERROR] Generation failed: " + e.getMessage());
        e.printStackTrace();
        System.exit(1);
    }
}
```

#### 2. Configuration Processing

```java
private void validateAndLoadConfiguration(Properties config) {
    // Validates:
    // - DBB_MODELER_BUILD_CONFIGURATION (build config folder)
    // - DBB_ZBUILDER (zBuilder installation path)
    // - DBB_MODELER_APPLICATION_DIR (application directory)
    // - TYPE_CONFIGURATIONS_FILE (types configurations YAML)
}
```

#### 3. Language Configuration Generation

Generates individual YAML files for each type configuration:

```yaml
config:
  - name: variableName1
    value: variableValue1
  - name: variableName2
    value: variableValue2
```

**Process**:
1. Reads types configurations from `typesConfigurations.yaml`
2. For each unique type found in application files:
   - Looks up type configuration
   - Creates YAML file in build configuration folder
   - Sets UTF-8 encoding tag (z/OS)

#### 4. Application Configuration Generation

Generates `dbb-app.yaml` with structure:

```yaml
version: 1.0.0
application:
  name: applicationName
  tasks:
    - task: COBOL
      variables:
        - name: languageConfigurationSource
          value: ${DBB_BUILD}/build-configuration/COBOL.yaml
          forFiles:
            - app/cobol/*.cbl
    - task: ImpactAnalysis
      variables:
        - name: impactQueryPatterns
          value:
            - languageExt: cbl
              dependencyPatterns:
                - ${APP_DIR_NAME}/copybooks/*.cpy
```

#### 5. Dependency Configuration

Three language-specific generators:

**COBOL Dependencies**:
```java
private void generateCobolDependencyConfiguration(
    ApplicationDescriptor.Source sourceGroupWithPrograms,
    List<Map<String, Object>> impactQueryPatternsValue,
    List<Map<String, Object>> tasks)
```
- Adds BMS copybook patterns
- Adds COBOL copybook patterns
- Adds COBOL program patterns
- Creates dependency search paths for copybooks

**ASM Dependencies**:
```java
private void generateAsmDependencyConfiguration(
    ApplicationDescriptor.Source sourceGroupWithPrograms,
    List<Map<String, Object>> impactQueryPatternsValue,
    List<Map<String, Object>> tasks)
```
- Adds ASM macro patterns
- Adds ASM program patterns
- Creates dependency search paths for macros

**Link Dependencies**:
```java
private void generateLinkDependencyConfiguration(
    ApplicationDescriptor.Source sourceGroupWithPrograms,
    List<Map<String, Object>> impactQueryPatternsValue)
```
- Adds COBOL program patterns for link cards

### Helper Classes

#### FileToLanguageConfig

Internal class for mapping files to language configurations:

```java
private static class FileToLanguageConfig {
    String file;        // File path
    String task;        // Language processor task
    String type;        // Type configuration name
}
```

### Command-Line Interface

```bash
java -cp dbb-git-migration-modeler.jar \
  com.ibm.dbb.migration.GenerateZBuilderProperties \
  -a <application> \
  -c <configFile> \
  [-l <logFile>]
```

**Options**:
- `-a, --application`: Application name (required)
- `-c, --configFile`: Path to DBB Git Migration Modeler configuration file (required)
- `-l, --logFile`: Path to output log file (optional)
- `-h, --help`: Print help message

### Configuration Requirements

The configuration file must contain:

```properties
# Build configuration folder
DBB_MODELER_BUILD_CONFIGURATION=/path/to/build-configuration

# zBuilder installation
DBB_ZBUILDER=/path/to/zBuilder

# Application directory
DBB_MODELER_APPLICATION_DIR=/path/to/applications

# Types configurations file
TYPE_CONFIGURATIONS_FILE=/path/to/typesConfigurations.yaml
```

## Key Differences from Groovy

### 1. YAML Generation

**Groovy** (implicit):
```groovy
def yamlContent = [
    config: variables
]
new Yaml().dump(yamlContent, writer)
```

**Java** (explicit):
```java
Map<String, Object> yamlContent = new LinkedHashMap<>();
yamlContent.put("config", typeConfigVariables);

DumperOptions options = new DumperOptions();
options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
options.setPrettyFlow(true);
Yaml yamlWriter = new Yaml(options);
yamlWriter.dump(yamlContent, writer);
```

### 2. Collection Operations

**Groovy** (closures):
```groovy
def sourceGroupsWithPrograms = applicationDescriptor.sources.findAll { 
    it.artifactsType?.equalsIgnoreCase("Program") 
}
```

**Java** (streams):
```java
List<ApplicationDescriptor.Source> sourceGroupsWithPrograms = 
    applicationDescriptor.getSources().stream()
        .filter(source -> "Program".equalsIgnoreCase(source.getArtifactsType()))
        .collect(Collectors.toList());
```

### 3. Map Manipulation

**Groovy** (dynamic):
```groovy
def task = tasks.find { it.task == taskName }
if (!task) {
    task = [task: taskName, variables: []]
    tasks << task
}
```

**Java** (typed):
```java
Map<String, Object> task = findTask(tasks, taskName);
if (task == null) {
    task = new LinkedHashMap<>();
    task.put("task", taskName);
    task.put("variables", new ArrayList<Map<String, Object>>());
    tasks.add(task);
}
```

### 4. File Tagging (z/OS)

**Groovy**:
```groovy
"chtag -tc UTF-8 ${yamlFile}".execute().waitFor()
```

**Java**:
```java
try {
    Runtime.getRuntime().exec("chtag -tc UTF-8 " + 
        yamlFile.getAbsolutePath()).waitFor();
} catch (Exception e) {
    // Ignore on non-z/OS systems
}
```

## Dependencies

### Required Libraries

1. **Apache Commons CLI** - Command-line parsing
2. **SnakeYAML** - YAML reading/writing
3. **DBB Core** - Application descriptor utilities

### Shared Utilities

- `com.ibm.dbb.migration.utils.Logger` - UTF-8 logging
- `com.ibm.dbb.migration.utils.ApplicationDescriptorUtils` - YAML operations
- `com.ibm.dbb.migration.model.ApplicationDescriptor` - Application model
- `com.ibm.dbb.migration.model.TypesMapping` - Types configuration model

## Generated Artifacts

### 1. Language Configuration Files

**Location**: `${DBB_MODELER_BUILD_CONFIGURATION}/<type>.yaml`

Example `COBOL.yaml`:
```yaml
config:
  - name: SYSLIB
    value: ${WORKSPACE}/${APP_DIR_NAME}/copybooks
  - name: DBRMLIB
    value: ${WORKSPACE}/${APP_DIR_NAME}/dbrm
```

### 2. Application Configuration File

**Location**: `${DBB_MODELER_APPLICATION_DIR}/<application>/dbb-app.yaml`

Example structure:
```yaml
version: 1.0.0
application:
  name: MYAPP
  tasks:
    - task: COBOL
      variables:
        - name: languageConfigurationSource
          value: ${DBB_BUILD}/build-configuration/COBOL.yaml
          forFiles:
            - MYAPP/cobol/*.cbl
        - name: dependencySearchPath
          value: search:${WORKSPACE}/?path=MYAPP/copybooks/*.cpy
    - task: ImpactAnalysis
      variables:
        - name: impactQueryPatterns
          value:
            - languageExt: cbl
              dependencyPatterns:
                - ${APP_DIR_NAME}/copybooks/*.cpy
                - ${APP_DIR_NAME}/cobol/*.cbl
```

## Usage Example

### Basic Usage

```bash
java -cp dbb-git-migration-modeler.jar \
  com.ibm.dbb.migration.GenerateZBuilderProperties \
  -a MYAPP \
  -c /path/to/config.properties
```

### With Log File

```bash
java -cp dbb-git-migration-modeler.jar \
  com.ibm.dbb.migration.GenerateZBuilderProperties \
  -a MYAPP \
  -c /path/to/config.properties \
  -l /path/to/generation.log
```

## Output Messages

### Success Messages

```
** Reading the Types Configurations definitions from '/path/to/typesConfigurations.yaml'.
** Gathering the defined types for files.
** Generating zBuilder language configuration files.
	Type Configuration for type 'COBOL' found in '/path/to/typesConfigurations.yaml'.
** Generating zBuilder Application configuration file.
** Generating Dependencies Search Paths and Impact Analysis Query Patterns.
** Application Configuration file '/path/to/dbb-app.yaml' successfully created.
** [INFO] 3 Language Configuration files created in '/path/to/build-configuration'.
** [INFO] Before running builds with zBuilder, please copy the content of the 
   '/path/to/build-configuration' folder to your zBuilder instance located at 
   '/path/to/zBuilder'.
** [INFO] Make sure the zBuilder Configuration files (Language Task definitions) 
   are accurate before running a build with zBuilder.
** [INFO] For each Language Task definition, the Dependency Search Path variable 
   potentially needs to be updated to match the layout of the Git repositories.
```

### Warning Messages

```
[WARNING] No Type Configuration for type 'UNKNOWN' found in 
'/path/to/typesConfigurations.yaml'.
```

### Error Messages

```
[ERROR] the Types Configurations file '/path/to/typesConfigurations.yaml' 
does not exist. Exiting.
[ERROR] The Application Descriptor file '/path/to/applicationDescriptor.yml' 
does not exist. Exiting.
[ERROR] The Build Configuration folder must be specified. Exiting.
[ERROR] The DBB zBuilder instance must be specified and exist. Exiting.
```

## Build Integration

### Gradle Build

The class is included in the main JAR:

```gradle
jar {
    manifest {
        attributes(
            'Main-Class': 'com.ibm.dbb.migration.ExtractApplications'
        )
    }
    from {
        configurations.runtimeClasspath.collect { 
            it.isDirectory() ? it : zipTree(it) 
        }
    }
}
```

### Execution from JAR

```bash
java -cp build/libs/dbb-git-migration-modeler-1.0.0.jar \
  com.ibm.dbb.migration.GenerateZBuilderProperties \
  -a MYAPP -c config.properties
```

## Testing Considerations

### Unit Testing

Key areas for testing:
1. Configuration validation
2. YAML generation correctness
3. Dependency pattern generation
4. Task and variable creation
5. File path mapping

### Integration Testing

1. Generate configurations for sample applications
2. Validate generated YAML syntax
3. Test with actual zBuilder builds
4. Verify dependency resolution

## Migration Notes

### From Groovy Script

1. **No Behavioral Changes**: Logic preserved exactly
2. **Type Safety**: Added explicit type checking
3. **Error Handling**: Enhanced with proper exception handling
4. **Logging**: Standardized UTF-8 logging
5. **Cross-Platform**: Works on Windows, Linux, z/OS

### Configuration Compatibility

- Uses same configuration file format
- Generates identical YAML structures
- Compatible with existing zBuilder installations

## Performance Considerations

1. **YAML Processing**: Efficient SnakeYAML usage
2. **File I/O**: Minimal file operations
3. **Memory**: Processes one application at a time
4. **Scalability**: Handles large applications with many files

## Troubleshooting

### Common Issues

**Issue**: "Types Configurations file does not exist"
- **Solution**: Verify TYPE_CONFIGURATIONS_FILE path in config

**Issue**: "Application Descriptor file does not exist"
- **Solution**: Ensure application was extracted first

**Issue**: "No Type Configuration found"
- **Solution**: Add missing type to typesConfigurations.yaml

**Issue**: Generated YAML has incorrect syntax
- **Solution**: Check SnakeYAML version compatibility

## Future Enhancements

1. **Validation**: Add YAML schema validation
2. **Templates**: Support custom configuration templates
3. **Optimization**: Batch process multiple applications
4. **Reporting**: Generate configuration summary reports

## Related Documentation

- [ValidateConfiguration Conversion](VALIDATE-CONFIGURATION-CONVERSION.md)
- [AssessUsage Conversion](ASSESS-USAGE-CONVERSION.md)
- [GenerateZAppBuildProperties Conversion](GENERATE-ZAPPBUILD-PROPERTIES-CONVERSION.md)
- [UTF-8 Standardization](UTF8-STANDARDIZATION.md)

## Conclusion

The Java implementation of GenerateZBuilderProperties provides:
- ✅ Complete feature parity with Groovy version
- ✅ Enhanced type safety and error handling
- ✅ Cross-platform compatibility
- ✅ Standardized UTF-8 logging
- ✅ Maintainable, well-documented code
- ✅ Integration with existing build system

The conversion maintains all functionality while improving code quality and maintainability.