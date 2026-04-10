@echo off
REM ========================================
REM Build script for Windows
REM Builds the Java application using stub libraries
REM ========================================

echo.
echo ========================================
echo DBB Git Migration Modeler - Windows Build
echo ========================================
echo.

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java JDK 8 or higher
    exit /b 1
)

REM Check if Java is installed (required for Gradle Wrapper)
echo Checking Java installation...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Gradle Wrapper requires Java to run
    exit /b 1
)

echo [1/3] Checking for stub JAR files...
if not exist "jzos-stubs.jar" (
    echo Stub JAR files not found. Creating them now...
    call create-stubs.bat
    if errorlevel 1 (
        echo ERROR: Failed to create stub JAR files
        exit /b 1
    )
) else (
    echo Stub JAR files found: jzos-stubs.jar, dmh-stubs.jar
)

echo.
echo [2/3] Building project with Gradle Wrapper...
gradlew.bat clean build --no-daemon
if errorlevel 1 (
    echo ERROR: Build failed
    exit /b 1
)

echo.
echo [3/3] Verifying build outputs...
if not exist "build\libs\dbb-git-migration-modeler-1.0.0.jar" (
    echo ERROR: Main JAR not found
    exit /b 1
)
if not exist "build\libs\dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar" (
    echo ERROR: Fat JAR not found
    exit /b 1
)

echo.
echo ========================================
echo BUILD SUCCESSFUL
echo ========================================
echo.
echo Build outputs:
echo   - Main JAR: build\libs\dbb-git-migration-modeler-1.0.0.jar
echo   - Fat JAR:  build\libs\dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar
echo   - Dependencies: build\libs\lib\
echo.
echo Next steps:
echo   1. Transfer the fat JAR to your z/OS system
echo   2. Run on z/OS with: java -cp "dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar:$DBB_HOME/lib/*" com.ibm.dbb.migration.ExtractApplications -c config.properties
echo.
echo For more information, see BUILD-ON-WINDOWS.md
echo ========================================

@REM Made with Bob
