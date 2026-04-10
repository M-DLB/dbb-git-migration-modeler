# Gradle Quick Start Guide

This project uses **Gradle** as the build system. No Gradle installation is required - the project includes Gradle Wrapper.

## Prerequisites

- **Java 8 or higher** installed
- **JAVA_HOME** environment variable set (optional but recommended)

## Quick Commands

### Build the Project

```bash
# Unix/Linux/macOS
./gradlew clean build --no-daemon

# Windows
gradlew.bat clean build --no-daemon

# Or use the build script (automatically uses --no-daemon)
./src/java/build.sh
```

### Common Gradle Tasks

```bash
# Clean build artifacts
./gradlew clean --no-daemon

# Compile Java sources only
./gradlew compileJava --no-daemon

# Build JAR files (includes fatJar)
./gradlew build --no-daemon

# Run the application
./gradlew run --no-daemon --args="-c config.properties -l extract.log"

# Display project information
./gradlew info --no-daemon

# List all available tasks
./gradlew tasks --no-daemon

# List all dependencies
./gradlew dependencies --no-daemon
```

**Note**: The `--no-daemon` flag is recommended for z/OS and CI/CD environments to avoid daemon process issues.

## Build Outputs

After running `./gradlew build`, you'll find:

```
build/
├── libs/
│   ├── dbb-git-migration-modeler-1.0.0.jar                          # Main JAR
│   ├── dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar   # Fat JAR (recommended)
│   └── lib/                                                          # Runtime dependencies
└── classes/
    └── java/
        └── main/                                                     # Compiled classes
```

## Running the Application

### Using the Fat JAR (Recommended)

```bash
java -cp "build/libs/dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar:$DBB_HOME/lib/*" \
  com.ibm.dbb.migration.ExtractApplications \
  -c /path/to/config.properties \
  -l /path/to/extract.log
```

### Using the Wrapper Script

```bash
./src/java/run-extract-applications.sh \
  -c /path/to/config.properties \
  -l /path/to/extract.log
```

### Using Gradle Run Task

```bash
./gradlew run --no-daemon --args="-c /path/to/config.properties -l /path/to/extract.log"
```

## Gradle Wrapper

The Gradle Wrapper ensures everyone uses the same Gradle version:

- **gradlew** (Unix/Linux/macOS) - Shell script
- **gradlew.bat** (Windows) - Batch file
- **gradle/wrapper/** - Wrapper configuration and JAR

### First Run

On first run, Gradle Wrapper will:
1. Download Gradle 8.5 (if not already cached)
2. Cache it in `~/.gradle/wrapper/dists/`
3. Use it for all subsequent builds

### Updating Gradle Version

To update to a newer Gradle version:

```bash
./gradlew wrapper --gradle-version=8.6 --no-daemon
```

## Project Structure

```
dbb-git-migration-modeler/
├── build.gradle                    # Build configuration
├── gradlew                         # Gradle wrapper (Unix)
├── gradlew.bat                     # Gradle wrapper (Windows)
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar      # Wrapper JAR
│       └── gradle-wrapper.properties
└── src/
    └── java/
        └── com/ibm/dbb/migration/  # Java source files
```

## Dependencies

The project uses these dependencies:

### Runtime (Included in Fat JAR)
- **Apache Commons CLI 1.5.0** - Command-line parsing
- **SnakeYAML 2.0** - YAML configuration parsing

### Provided (Must be in Classpath)
- **IBM JZOS** - z/OS file operations
- **IBM DBB** - Build framework
- **IBM DMH Scanner** - File classification

## Environment Variables

### Optional but Recommended

```bash
# Java installation
export JAVA_HOME=/usr/lpp/java/J8.0_64

# DBB installation (for provided dependencies)
export DBB_HOME=/var/dbb

# JZOS installation (for provided dependencies)
export JZOS_HOME=/usr/lpp/IBM/izoda/v1r1/IBM/jzos
```

## Troubleshooting

### Gradle Daemon Issues

```bash
# Stop all Gradle daemons (if any are running)
./gradlew --stop

# Run with --no-daemon (recommended for z/OS)
./gradlew build --no-daemon
```

**Note**: All build scripts in this project use `--no-daemon` by default.

### Clean Everything

```bash
# Clean build artifacts
./gradlew clean --no-daemon

# Also clean Gradle cache (use with caution)
rm -rf ~/.gradle/caches/
```

### Permission Issues (Unix/Linux)

```bash
# Make gradlew executable
chmod +x gradlew
```

### Build Fails with "Could not find..."

Ensure environment variables are set:
```bash
echo $DBB_HOME
echo $JZOS_HOME
```

If not set, update `build.gradle` with correct paths:
```groovy
compileOnly fileTree(dir: '/var/dbb', include: ['lib/**/*.jar'])
```

## IDE Integration

### IntelliJ IDEA
1. Open project directory
2. IntelliJ will auto-detect Gradle
3. Click "Import Gradle Project"

### Eclipse
1. Install Buildship Gradle plugin
2. File → Import → Gradle → Existing Gradle Project
3. Select project directory

### VS Code
1. Install "Gradle for Java" extension
2. Open project directory
3. Use Gradle tasks view

## Gradle vs Maven

This project was converted from Maven to Gradle:

| Feature | Maven | Gradle |
|---------|-------|--------|
| Build file | pom.xml | build.gradle |
| Build command | mvn clean package | ./gradlew clean build |
| Output directory | target/ | build/ |
| Wrapper | Not standard | Included |
| Performance | Slower | Faster (incremental builds) |

## Additional Resources

- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- [Gradle Build Language Reference](https://docs.gradle.org/current/dsl/)
- [Gradle Wrapper Documentation](https://docs.gradle.org/current/userguide/gradle_wrapper.html)

## Support

For issues specific to this project:
1. Check `src/java/README.md` for detailed documentation
2. Review `JAVA_CONVERSION_SUMMARY.md` for conversion details
3. Verify all environment variables are set correctly
4. Ensure Java 8+ is installed and accessible

---

**Quick Start**: Just run `./gradlew build` and you're ready to go! 🚀