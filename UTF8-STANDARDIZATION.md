# UTF-8 Encoding Standardization

## Overview
All three Java migration tools now use UTF-8 as the default encoding for log files, ensuring consistency across the toolkit.

## Changes Made

### 1. MigrateDatasets.java
**Removed deprecated IBM-1047 encoding support:**

#### Command-line Option Removed
- **Removed**: `-le, --logEncoding` option
- **Reason**: Standardizing on UTF-8 eliminates the need for encoding configuration

**Before:**
```java
options.addOption(Option.builder("le")
    .longOpt("logEncoding")
    .hasArg()
    .argName("logEncoding")
    .desc("Encoding for the logfile (optional), default is IBM-1047")
    .build());
```

**After:**
```java
// Option removed - UTF-8 is now the standard
```

#### Logger Initialization Updated
**Before:**
```java
String logEncoding = cmd.getOptionValue("le", "IBM-1047");
logger.create(logFilePath, logEncoding);
logger.logMessage("Messages will be saved in '" + logFilePath + "' with encoding '" + logEncoding + "'");
```

**After:**
```java
logger.create(logFilePath); // Uses UTF-8 by default
logger.logMessage("Messages will be saved in '" + logFilePath + "' with UTF-8 encoding");
```

#### Help Text Updated
**Before:**
```java
.desc("Path to a logfile (optional)")
```

**After:**
```java
.desc("Path to a logfile (optional, UTF-8 encoding)")
```

### 2. Logger.java (Shared Utility)
The shared Logger utility already supports UTF-8 as default:

```java
public void create(String loggerFilePath) throws IOException {
    create(loggerFilePath, "UTF-8"); // Default encoding
}

public void create(String loggerFilePath, String encoding) throws IOException {
    this.loggerFilePath = loggerFilePath;
    this.encoding = encoding;
    // Creates file with specified encoding
    logWriter = new BufferedWriter(
        new OutputStreamWriter(
            new FileOutputStream(loggerFilePath, true), 
            encoding
        )
    );
}
```

### 3. All Three Tools Now Consistent

| Tool | Logger Usage | Encoding |
|------|-------------|----------|
| **ExtractApplications** | `logger.create(logFilePath)` | UTF-8 (default) |
| **MigrateDatasets** | `logger.create(logFilePath)` | UTF-8 (default) |
| **ScanApplication** | `logger.create(logFilePath)` | UTF-8 (default) |

## Benefits

1. **Consistency**: All tools use the same encoding standard
2. **Simplicity**: No need to specify encoding in command-line arguments
3. **Modern Standard**: UTF-8 is the universal standard for text encoding
4. **Cross-platform**: UTF-8 works consistently across all platforms
5. **Maintainability**: Single encoding standard simplifies code maintenance

## Migration Notes

### For Users Upgrading from Groovy Scripts
If you were using the `-le` or `--logEncoding` option with the Groovy version:

**Old command:**
```bash
java -cp ... MigrateDatasets -l logfile.txt -le IBM-1047 datasets.txt
```

**New command:**
```bash
java -cp ... MigrateDatasets -l logfile.txt datasets.txt
```

The `-le` option is no longer supported. All log files are now created with UTF-8 encoding.

### For z/OS Users
If you need to view log files on z/OS with EBCDIC tools, you may need to convert the encoding:

```bash
iconv -f UTF-8 -t IBM-1047 logfile.txt > logfile-ebcdic.txt
```

## Build Verification

The changes have been tested and verified:

```bash
./gradlew clean build
```

**Result:** BUILD SUCCESSFUL
- All three tools compile without errors
- Single JAR created: `dbb-git-migration-modeler-1.0.0.jar`
- All dependencies properly included

## Technical Details

### Character Encoding Flow

1. **Logger Creation**: When `-l` option is used, logger is created with UTF-8
2. **File Writing**: All log messages written using UTF-8 encoding
3. **File Reading**: Log files can be read with any UTF-8 compatible editor

### Encoding Support Matrix

| Encoding | ExtractApplications | MigrateDatasets | ScanApplication |
|----------|-------------------|-----------------|-----------------|
| UTF-8 | ✅ Default | ✅ Default | ✅ Default |
| IBM-1047 | ❌ Not supported | ❌ Removed | ❌ Not supported |
| Custom | ❌ Not supported | ❌ Removed | ❌ Not supported |

## Related Files

- [`src/java/com/ibm/dbb/migration/MigrateDatasets.java`](src/java/com/ibm/dbb/migration/MigrateDatasets.java) - Updated to use UTF-8
- [`src/java/com/ibm/dbb/migration/ExtractApplications.java`](src/java/com/ibm/dbb/migration/ExtractApplications.java) - Already using UTF-8
- [`src/java/com/ibm/dbb/migration/ScanApplication.java`](src/java/com/ibm/dbb/migration/ScanApplication.java) - Already using UTF-8
- [`src/java/com/ibm/dbb/migration/utils/Logger.java`](src/java/com/ibm/dbb/migration/utils/Logger.java) - Shared logger utility

## Conclusion

The UTF-8 standardization simplifies the toolkit while maintaining full functionality. All log files are now consistently encoded, making them easier to share, process, and integrate with modern development tools.