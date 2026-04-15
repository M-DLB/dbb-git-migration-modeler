# MappingRule Stub Cleanup

## Overview
The `MappingRuleStub` class has been removed as the actual `MappingRule` class is available in the DBB core library.

## Investigation Results

### 1. DBB Library Contains MappingRule
The actual implementation exists in the DBB core JAR:

```bash
$ jar -tf lib/dbb/dbb.core_3.0.4.1.jar | findstr /i "MappingRule"
com/ibm/dbb/migration/AbstractMappingRule$CopyInfo.class
com/ibm/dbb/migration/AbstractMappingRule.class
com/ibm/dbb/migration/IMappingRule.class
com/ibm/dbb/migration/MappingRule.class
```

**Package:** `com.ibm.dbb.migration.MappingRule`

### 2. Stub Was Not Being Used
Search results showed the stub was never imported or used in any Java code:

```bash
$ search_files "MappingRuleStub|import.*stubs\.MappingRuleStub"
Found 0 results.
```

### 3. MappingRule References in Code
The only references to MappingRule in the codebase are in `MigrateDatasets.java` as string literals for reflection:

**File:** `src/java/com/ibm/dbb/migration/MigrateDatasets.java`

```java
// Line 221: Help text
.desc("The ID of mapping rule (optional), for example: com.ibm.dbb.migration.MappingRule")

// Line 347: Default mapping rule ID
String mappingRuleId = "com.ibm.dbb.migration.MappingRule";

// Line 452-453: Mapping rule ID resolution
mappingIds.put("MappingRule", "com.ibm.dbb.migration.MappingRule");
mappingIds.put("com.ibm.dbb.migration.MappingRule", "com.ibm.dbb.migration.MappingRule");

// Line 475: Default return value
return new Object[]{"com.ibm.dbb.migration.MappingRule", new HashMap<String, String>()};
```

These are all string references used for dynamic class loading via reflection, not direct class imports.

## Actions Taken

### 1. Removed Stub File
```bash
del src\java\com\ibm\dbb\migration\stubs\MappingRuleStub.java
```

### 2. Removed Empty Stubs Directory
```bash
rmdir src\java\com\ibm\dbb\migration\stubs
```

### 3. Verified Build Success
```bash
./gradlew clean build
```

**Result:** BUILD SUCCESSFUL in 10s

## Current Project Structure

```
src/java/com/ibm/dbb/migration/
├── ExtractApplications.java
├── MigrateDatasets.java
├── ScanApplication.java
├── model/
│   ├── ApplicationDescriptor.java
│   ├── ApplicationMappingConfiguration.java
│   ├── RepositoryPathsMapping.java
│   └── TypesMapping.java
└── utils/
    ├── ApplicationDescriptorUtils.java
    └── Logger.java
```

**Note:** The `stubs/` directory has been completely removed.

## Why the Stub Was Created

The stub was likely created during initial development to:
1. Enable compilation on non-z/OS systems without DBB libraries
2. Provide a minimal interface for development and testing

However, since:
- The actual DBB libraries are now available in `lib/dbb/`
- The stub was never actually used in the code
- MappingRule is only referenced via reflection (string-based class loading)

The stub was unnecessary and has been safely removed.

## Runtime Behavior

At runtime, when `MigrateDatasets` needs to use the MappingRule:

1. The class name `"com.ibm.dbb.migration.MappingRule"` is passed to reflection APIs
2. Java's ClassLoader loads the actual class from `dbb.core_3.0.4.1.jar`
3. The real DBB implementation is instantiated and used

This approach allows the code to compile without importing the class directly, while still using the actual implementation at runtime.

## Benefits of Removal

1. **Cleaner Codebase**: No unnecessary stub implementations
2. **Single Source of Truth**: Only the actual DBB implementation exists
3. **Reduced Maintenance**: No need to keep stub in sync with DBB API changes
4. **Clarity**: Developers know to use the real DBB classes from the JAR files

## Related Documentation

- [DBB Core Library](lib/dbb/dbb.core_3.0.4.1.jar) - Contains the actual MappingRule implementation
- [MigrateDatasets.java](src/java/com/ibm/dbb/migration/MigrateDatasets.java) - Uses MappingRule via reflection
- [Build Configuration](build.gradle) - Includes DBB JARs in classpath

## Conclusion

The MappingRuleStub has been successfully removed. The project now relies entirely on the actual DBB library implementations, which are available in the `lib/dbb/` directory. The build continues to succeed, confirming that the stub was indeed unnecessary.