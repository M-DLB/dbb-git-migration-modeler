# Gradle Wrapper Guide

## What is Gradle Wrapper?

The Gradle Wrapper is a script that allows you to run Gradle builds **without having Gradle installed** on your system. It automatically downloads and uses the correct Gradle version specified in the project.

## Benefits

✅ **No Gradle installation required** - Just need Java
✅ **Consistent builds** - Everyone uses the same Gradle version
✅ **Easy setup** - Works out of the box
✅ **Cross-platform** - Works on Windows, Linux, and macOS

## Prerequisites

**Only Java is required:**
- Java JDK 8 or higher
- No Gradle installation needed!

Check if Java is installed:
```powershell
java -version
```

## Using Gradle Wrapper

### On Windows

Use `gradlew.bat`:

```powershell
# Build the project
.\gradlew.bat clean build --no-daemon

# Run specific tasks
.\gradlew.bat tasks --no-daemon
.\gradlew.bat clean --no-daemon
.\gradlew.bat build --no-daemon
```

### On Linux/macOS/z/OS

Use `gradlew`:

```bash
# Build the project
./gradlew clean build --no-daemon

# Run specific tasks
./gradlew tasks --no-daemon
./gradlew clean --no-daemon
./gradlew build --no-daemon
```

## First Run

The first time you run the Gradle Wrapper, it will:

1. Download Gradle 8.5 (specified in [`gradle/wrapper/gradle-wrapper.properties`](gradle/wrapper/gradle-wrapper.properties:3))
2. Cache it in `~/.gradle/wrapper/dists/`
3. Use it to build your project

**This is normal and only happens once!**

Example output:
```
Downloading https://services.gradle.org/distributions/gradle-8.5-bin.zip
...........10%...........20%...........30%...........40%...........50%...........60%...........70%...........80%...........90%...........100%

BUILD SUCCESSFUL in 45s
```

## Quick Start

### Windows

```powershell
# 1. Ensure Java is installed
java -version

# 2. Build the project (first run downloads Gradle)
.\gradlew.bat clean build --no-daemon

# 3. Find your JAR files
dir build\libs
```

### Linux/macOS/z/OS

```bash
# 1. Ensure Java is installed
java -version

# 2. Make gradlew executable (first time only)
chmod +x gradlew

# 3. Build the project (first run downloads Gradle)
./gradlew clean build --no-daemon

# 4. Find your JAR files
ls -l build/libs/
```

## Gradle Wrapper Files

The project includes these wrapper files:

```
├── gradlew              # Unix/Linux/macOS wrapper script
├── gradlew.bat          # Windows wrapper script
└── gradle/
    └── wrapper/
        ├── gradle-wrapper.jar        # Wrapper executable
        └── gradle-wrapper.properties # Wrapper configuration
```

**These files are committed to Git** so everyone can build without installing Gradle.

## Common Commands

### Build Commands

```powershell
# Clean build directory
.\gradlew.bat clean --no-daemon

# Compile code
.\gradlew.bat compileJava --no-daemon

# Build JARs
.\gradlew.bat build --no-daemon

# Build without tests
.\gradlew.bat build -x test --no-daemon
```

### Information Commands

```powershell
# List all available tasks
.\gradlew.bat tasks --no-daemon

# Show project information
.\gradlew.bat info --no-daemon

# Show dependencies
.\gradlew.bat dependencies --no-daemon
```

### Clean Commands

```powershell
# Clean build directory
.\gradlew.bat clean --no-daemon

# Clean and rebuild
.\gradlew.bat clean build --no-daemon
```

## Why `--no-daemon`?

The `--no-daemon` flag prevents Gradle from running as a background process. This is important for:

- **z/OS compatibility** - Daemon processes can cause issues on z/OS
- **CI/CD pipelines** - Ensures clean builds in automated environments
- **Resource management** - Prevents background processes from consuming resources

## Troubleshooting

### Problem: "java: command not found"

**Cause:** Java is not installed or not in PATH

**Solution:**
```powershell
# Install Java (Windows with Chocolatey)
choco install openjdk

# Or download from: https://adoptium.net/
```

### Problem: "Permission denied" (Linux/macOS)

**Cause:** gradlew script is not executable

**Solution:**
```bash
chmod +x gradlew
./gradlew clean build --no-daemon
```

### Problem: Download fails or times out

**Cause:** Network issues or firewall blocking download

**Solution:**
1. Check internet connection
2. Check firewall/proxy settings
3. Try again - downloads are resumable
4. If behind corporate proxy, configure in `gradle.properties`:
   ```properties
   systemProp.http.proxyHost=proxy.company.com
   systemProp.http.proxyPort=8080
   systemProp.https.proxyHost=proxy.company.com
   systemProp.https.proxyPort=8080
   ```

### Problem: "Could not find or load main class org.gradle.wrapper.GradleWrapperMain"

**Cause:** gradle-wrapper.jar is missing or corrupted

**Solution:**
The gradle-wrapper.jar should be in `gradle/wrapper/`. If missing, it needs to be regenerated (requires Gradle installed temporarily).

## Updating Gradle Version

To update the Gradle version used by the wrapper:

```powershell
# If you have Gradle installed
gradle wrapper --gradle-version 8.6 --no-daemon

# Or manually edit gradle/wrapper/gradle-wrapper.properties
# Change: distributionUrl=https\://services.gradle.org/distributions/gradle-8.6-bin.zip
```

## Comparison: Gradle vs Gradle Wrapper

| Feature | System Gradle | Gradle Wrapper |
|---------|--------------|----------------|
| Installation | Required | Not required |
| Version | System-dependent | Project-specific |
| Consistency | Varies by system | Same for everyone |
| Setup | Manual | Automatic |
| Best for | Local development | Team projects |

## Integration with Build Scripts

The automated build script ([`build-windows.bat`](build-windows.bat:1)) uses Gradle Wrapper:

```batch
.\gradlew.bat clean build --no-daemon
```

This ensures consistent builds across all environments.

## Summary

The Gradle Wrapper makes building the project simple:

1. **No Gradle installation needed** - Just Java
2. **Consistent builds** - Same Gradle version for everyone
3. **Easy to use** - Just run `gradlew` or `gradlew.bat`
4. **Automatic setup** - Downloads Gradle on first run

### Quick Commands

**Windows:**
```powershell
.\gradlew.bat clean build --no-daemon
```

**Linux/macOS/z/OS:**
```bash
./gradlew clean build --no-daemon
```

That's it! The Gradle Wrapper handles everything else automatically.

## Additional Resources

- [Official Gradle Wrapper Documentation](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- [Gradle User Manual](https://docs.gradle.org/current/userguide/userguide.html)
- Project build configuration: [`build.gradle`](build.gradle:1)
- Wrapper properties: [`gradle/wrapper/gradle-wrapper.properties`](gradle/wrapper/gradle-wrapper.properties:1)