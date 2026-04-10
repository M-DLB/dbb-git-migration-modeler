# Simple Gradle Setup (System Gradle)

This project uses **system Gradle** (no wrapper required). You need to have Gradle installed on your system.

## Prerequisites

- **Java 8 or higher**
- **Gradle installed** on your system

## Installing Gradle

### On z/OS (USS)

```bash
# Download Gradle
cd /tmp
wget https://services.gradle.org/distributions/gradle-8.5-bin.zip

# Extract
unzip gradle-8.5-bin.zip
mv gradle-8.5 /usr/local/gradle

# Add to PATH (add to ~/.profile for persistence)
export PATH=$PATH:/usr/local/gradle/bin

# Verify installation
gradle --version
```

### Using SDKMAN (Recommended for Unix/Linux/macOS)

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Gradle
sdk install gradle

# Verify installation
gradle --version
```

### On Ubuntu/Debian

```bash
sudo apt-get update
sudo apt-get install gradle
gradle --version
```

### On macOS

```bash
brew install gradle
gradle --version
```

### On RHEL/CentOS

```bash
sudo yum install gradle
gradle --version
```

## Building the Project

Once Gradle is installed:

```bash
# Using the build script (recommended)
./src/java/build.sh

# Or directly with Gradle
gradle clean build --no-daemon
```

## Build Outputs

```
build/
├── libs/
│   ├── dbb-git-migration-modeler-1.0.0.jar                          # Main JAR
│   ├── dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar   # Fat JAR ⭐
│   └── lib/                                                          # Dependencies
└── classes/java/main/                                                # Compiled classes
```

## Running the Application

```bash
# Using the wrapper script
./src/java/run-extract-applications.sh \
  -c /path/to/config.properties \
  -l /path/to/extract.log

# Or directly with Java
java -cp "build/libs/dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar:$DBB_HOME/lib/*" \
  com.ibm.dbb.migration.ExtractApplications \
  -c /path/to/config.properties \
  -l /path/to/extract.log
```

## Common Gradle Commands

```bash
# Clean build artifacts
gradle clean --no-daemon

# Compile Java sources
gradle compileJava --no-daemon

# Build JAR files
gradle build --no-daemon

# Display project information
gradle info --no-daemon

# List all available tasks
gradle tasks --no-daemon

# List dependencies
gradle dependencies --no-daemon
```

## Why --no-daemon?

The `--no-daemon` flag prevents Gradle from starting background daemon processes:
- ✅ Better for z/OS environments
- ✅ Cleaner for CI/CD pipelines
- ✅ Predictable resource usage
- ✅ No lingering processes

## Troubleshooting

### Gradle not found

```bash
# Check if Gradle is in PATH
which gradle

# Check Gradle version
gradle --version

# If not found, install Gradle (see above)
```

### Java version issues

```bash
# Check Java version (need Java 8+)
java -version

# Set JAVA_HOME if needed
export JAVA_HOME=/usr/lpp/java/J8.0_64
```

### Build fails

```bash
# Clean and rebuild
gradle clean build --no-daemon

# Check for errors in build output
# Verify DBB_HOME is set
echo $DBB_HOME
```

## Project Structure

```
dbb-git-migration-modeler/
├── build.gradle                    # Gradle build configuration
├── src/
│   └── java/
│       └── com/ibm/dbb/migration/  # Java source files
└── build/                          # Build outputs (created after build)
```

## Quick Reference

| Task | Command |
|------|---------|
| Build project | `gradle clean build --no-daemon` |
| Build with script | `./src/java/build.sh` |
| Run application | `./src/java/run-extract-applications.sh -c config.properties` |
| Clean build | `gradle clean --no-daemon` |
| List tasks | `gradle tasks --no-daemon` |
| Check version | `gradle --version` |

## No Wrapper Files Needed

This project does NOT use Gradle wrapper, so you won't need:
- ❌ `gradlew` / `gradlew.bat`
- ❌ `gradle/wrapper/` directory
- ❌ `gradle-wrapper.jar`

Just install Gradle on your system and use `gradle` commands directly!

## Support

For more information:
- [Gradle Installation Guide](https://gradle.org/install/)
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- Project README: [`src/java/README.md`](src/java/README.md)