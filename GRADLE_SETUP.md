# Gradle Setup Guide

## Issue: Missing Gradle Wrapper JAR

If you see this error:
```
Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain
Caused by: java.lang.ClassNotFoundException: org.gradle.wrapper.GradleWrapperMain
```

This means the Gradle wrapper JAR file (`gradle/wrapper/gradle-wrapper.jar`) is missing. This is a binary file that cannot be created from text.

## Solutions

### Solution 1: Initialize Gradle Wrapper (Recommended)

If you have Gradle installed on your system:

```bash
# Run the initialization script
chmod +x init-gradle.sh
./init-gradle.sh
```

This will generate the missing `gradle-wrapper.jar` file.

### Solution 2: Install Gradle and Use Directly

#### On z/OS (USS)

```bash
# Download Gradle
cd /tmp
wget https://services.gradle.org/distributions/gradle-8.5-bin.zip

# Extract
unzip gradle-8.5-bin.zip
mv gradle-8.5 /usr/local/gradle

# Add to PATH
export PATH=$PATH:/usr/local/gradle/bin

# Verify installation
gradle --version

# Initialize wrapper
cd /path/to/project
gradle wrapper --gradle-version=8.5
```

#### Using SDKMAN (Unix/Linux/macOS)

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Gradle
sdk install gradle 8.5

# Initialize wrapper
cd /path/to/project
gradle wrapper --gradle-version=8.5 --no-daemon
```

#### On Ubuntu/Debian

```bash
sudo apt-get update
sudo apt-get install gradle

# Initialize wrapper
cd /path/to/project
gradle wrapper --gradle-version=8.5 --no-daemon
```

#### On macOS

```bash
brew install gradle

# Initialize wrapper
cd /path/to/project
gradle wrapper --gradle-version=8.5 --no-daemon
```

### Solution 3: Use System Gradle Without Wrapper

If you have Gradle installed, you can build directly without the wrapper:

```bash
# Instead of ./gradlew
gradle clean build --no-daemon

# The build.sh script will automatically detect and use system Gradle with --no-daemon
./src/java/build.sh
```

### Solution 4: Manual Wrapper JAR Download

If you cannot install Gradle, download the wrapper JAR manually:

```bash
# Create wrapper directory
mkdir -p gradle/wrapper

# Download wrapper JAR (Gradle 8.5)
cd gradle/wrapper
wget https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar

# Or using curl
curl -L -o gradle-wrapper.jar https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar

# Return to project root
cd ../..

# Make gradlew executable
chmod +x gradlew

# Now you can use the wrapper
./gradlew clean build --no-daemon
```

## Verification

After setting up Gradle, verify it works:

```bash
# Check Gradle version
gradle --version
# or
./gradlew --version

# Build the project
./gradlew clean build --no-daemon
# or
./src/java/build.sh
```

## Build Script Behavior

The [`src/java/build.sh`](src/java/build.sh) script now automatically:

1. **Checks for Gradle wrapper JAR**
2. **Falls back to system Gradle** if wrapper is missing
3. **Provides clear instructions** if neither is available

## Project Structure After Setup

```
dbb-git-migration-modeler/
├── build.gradle
├── gradlew                              # Wrapper script (Unix)
├── gradlew.bat                          # Wrapper script (Windows)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar           # ← This file is needed!
│       └── gradle-wrapper.properties
└── src/java/
```

## Why This Happens

The Gradle wrapper JAR is a binary file that:
- Cannot be stored as text in version control
- Must be generated using Gradle
- Or downloaded from the Gradle repository

Git repositories often exclude binary files, which is why the JAR might be missing.

## Alternative: Use Pre-built JAR

If you cannot set up Gradle, you can still use the pre-built JAR files if they're available:

```bash
# If build/libs/ directory exists with JAR files
java -cp "build/libs/dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar:$DBB_HOME/lib/*" \
  com.ibm.dbb.migration.ExtractApplications \
  -c config.properties \
  -l extract.log
```

## Getting Help

If you continue to have issues:

1. **Check Java version**: `java -version` (need Java 8+)
2. **Check Gradle installation**: `gradle --version`
3. **Verify file permissions**: `ls -la gradlew gradle/wrapper/`
4. **Check environment variables**: `echo $JAVA_HOME`

## Quick Reference

| Task | Command |
|------|---------|
| Initialize wrapper | `./init-gradle.sh` |
| Build with wrapper | `./gradlew clean build --no-daemon` |
| Build with system Gradle | `gradle clean build --no-daemon` |
| Build with script | `./src/java/build.sh` |
| Check Gradle version | `gradle --version` |
| List Gradle tasks | `./gradlew tasks --no-daemon` |

## Support

For more information:
- [Gradle Installation Guide](https://gradle.org/install/)
- [Gradle Wrapper Documentation](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- Project README: [`src/java/README.md`](src/java/README.md)
- Quick Start: [`GRADLE_QUICK_START.md`](GRADLE_QUICK_START.md)