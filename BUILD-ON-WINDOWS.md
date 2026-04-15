# Building on Windows Without z/OS Libraries

This guide explains how to build the Java application on your Windows laptop when you don't have access to the z/OS-specific libraries (JZOS, DBB, DMH Scanner).

## Overview

The application uses z/OS-specific libraries that are only available on mainframe systems. To compile on Windows, we use **stub JAR files** that contain empty implementations of these classes. These stubs are only used for compilation - the real libraries will be used when the application runs on z/OS.

## Prerequisites

1. **Java JDK 8 or higher** installed
2. **Gradle** installed (see [GRADLE_SIMPLE_SETUP.md](GRADLE_SIMPLE_SETUP.md) for installation instructions)

## Step-by-Step Build Process

### Step 1: Create Stub JAR Files

Run the stub creation script to generate the necessary stub libraries:

```powershell
.\create-stubs.bat
```

This will create two JAR files in the project root:
- `jzos-stubs.jar` - Stub implementations for JZOS classes (ZFile, PdsDirectory, etc.)
- `dmh-stubs.jar` - Stub implementations for DMH Scanner classes (Dmh5210, ScanProperties, etc.)

**Note:** These stub files are tracked in Git so you only need to create them once.

### Step 2: Build the Project

Once the stub JARs are created, build the project using Gradle:

```powershell
gradle clean build --no-daemon
```

This will:
1. Compile all Java source files using the stub libraries
2. Run tests (if any)
3. Create the JAR files in `build/libs/`:
   - `dbb-git-migration-modeler-1.0.0.jar` - Main JAR
   - `dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar` - Fat JAR with all dependencies

### Step 3: Deploy to z/OS

Transfer the generated JAR file to your z/OS system where the real libraries are available:

```bash
# On z/OS, run the application with real libraries
java -cp "dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar:$DBB_HOME/lib/*:$JZOS_HOME/*" \
  com.ibm.dbb.migration.ExtractApplications \
  -c config.properties \
  -l extract.log
```

## How It Works

### Build Configuration

The [`build.gradle`](build.gradle:34) file is configured to:

1. **Check for real libraries first**: If `JZOS_HOME` or `DBB_HOME` environment variables are set and point to valid directories, use those libraries
2. **Fall back to stubs**: If real libraries aren't found, use the stub JAR files for compilation
3. **Mark as compile-only**: These dependencies are marked as `compileOnly`, meaning they won't be packaged in the final JAR

```gradle
// Use real libraries if available, otherwise use stubs
if (System.getenv('JZOS_HOME') && new File(System.getenv('JZOS_HOME')).exists()) {
    compileOnly fileTree(dir: System.getenv('JZOS_HOME'), include: ['**/*.jar'])
} else if (new File('jzos-stubs.jar').exists()) {
    compileOnly files('jzos-stubs.jar')
}
```

### Stub Implementation

The stub classes provide minimal implementations that satisfy the compiler:

```java
// Example: ZFile stub
package com.ibm.jzos;
import java.io.*;

public class ZFile {
    public ZFile(String dsn, String mode) throws IOException {}
    public static boolean dsExists(String dsn) { return false; }
    public InputStream getInputStream() { return null; }
    public void close() throws IOException {}
}
```

These stubs:
- ✅ Allow compilation to succeed
- ✅ Provide correct method signatures
- ✅ Are never used at runtime (real libraries are used on z/OS)
- ❌ Cannot be used to run the application (will fail with NullPointerException or similar)

## Troubleshooting

### Problem: "gradle: command not found"

**Solution:** Install Gradle following the instructions in [GRADLE_SIMPLE_SETUP.md](GRADLE_SIMPLE_SETUP.md)

### Problem: Stub JARs not found

**Solution:** Run `.\create-stubs.bat` to create the stub JAR files

### Problem: Compilation errors about missing classes

**Solution:** 
1. Verify stub JARs exist: `dir *.jar`
2. Recreate stubs: `.\create-stubs.bat`
3. Clean and rebuild: `gradle clean build --no-daemon`

### Problem: Application fails when running on Windows

**Expected behavior:** The application is designed to run on z/OS only. The stubs are for compilation purposes only. You must deploy the JAR to z/OS to run it.

## Build Outputs

After a successful build, you'll find:

```
build/
├── libs/
│   ├── dbb-git-migration-modeler-1.0.0.jar                          # Main JAR
│   ├── dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar   # Fat JAR
│   └── lib/                                                          # Dependencies
│       ├── commons-cli-1.5.0.jar
│       └── snakeyaml-2.0.jar
└── classes/
    └── java/
        └── main/
            └── com/
                └── ibm/
                    └── dbb/
                        └── migration/
                            └── *.class                               # Compiled classes
```

## Key Points

1. **Stubs are for compilation only** - They allow the code to compile but cannot be used to run the application
2. **Real libraries required at runtime** - The application must run on z/OS where JZOS, DBB, and DMH Scanner are installed
3. **No code changes needed** - The same source code works on both Windows (for compilation) and z/OS (for execution)
4. **Stubs are version-controlled** - Once created, the stub JARs are committed to Git so other developers don't need to recreate them

## Alternative: Build on z/OS

If you have access to a z/OS system with Gradle installed, you can build directly there:

```bash
# On z/OS
export DBB_HOME=/var/dbb
export JZOS_HOME=/usr/lpp/IBM/izoda/v1r1/IBM/jzos
gradle clean build --no-daemon
```

This approach uses the real libraries and doesn't require stubs.

## Summary

Building on Windows is now possible thanks to stub libraries:

1. ✅ Run `.\create-stubs.bat` once to create stub JARs
2. ✅ Run `gradle clean build --no-daemon` to compile
3. ✅ Deploy the JAR to z/OS for execution
4. ✅ No code changes required

The stub approach provides the best of both worlds: develop and compile on Windows, run on z/OS with full functionality.