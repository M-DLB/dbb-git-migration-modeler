#!/bin/bash
#********************************************************************************
# Licensed Materials - Property of IBM                                          *
# (c) Copyright IBM Corporation 2018, 2024. All Rights Reserved.                *
#                                                                               *
# Note to U.S. Government Users Restricted Rights:                              *
# Use, duplication or disclosure restricted by GSA ADP Schedule                 *
# Contract with IBM Corp.                                                       *
#********************************************************************************

# Build script for ExtractApplications Java application

set -e

echo "=========================================="
echo "Building ExtractApplications Java Application"
echo "=========================================="

# Check Java version
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 8+ to build this project"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 8 ]; then
    echo "ERROR: Java 8 or higher is required"
    echo "Current Java version: $JAVA_VERSION"
    exit 1
fi

echo ""
echo "Java version: $(java -version 2>&1 | head -n 1)"
echo ""

# Navigate to project root (where build.gradle is located)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$PROJECT_ROOT"

echo "Building project in: $PROJECT_ROOT"
echo ""

# Check if Gradle wrapper JAR exists
if [ ! -f "$PROJECT_ROOT/gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "WARNING: Gradle wrapper JAR not found"
    echo ""
    echo "The Gradle wrapper needs to be initialized first."
    echo ""
    echo "Option 1: Initialize wrapper (requires Gradle installed)"
    echo "  ./init-gradle.sh"
    echo ""
    echo "Option 2: Use system Gradle directly"
    echo "  gradle clean build"
    echo ""
    echo "Option 3: Install Gradle"
    echo "  See: https://gradle.org/install/"
    echo ""
    
    # Try to use system gradle if available
    if command -v gradle &> /dev/null; then
        echo "Found system Gradle, using it instead..."
        echo "Running: gradle clean build"
        gradle clean build
        BUILD_RESULT=$?
    else
        echo "ERROR: Neither Gradle wrapper nor system Gradle found"
        echo "Please run: ./init-gradle.sh"
        exit 1
    fi
else
    # Make gradlew executable
    chmod +x gradlew 2>/dev/null || true
    
    # Clean and build
    echo "Running: ./gradlew clean build"
    ./gradlew clean build
    BUILD_RESULT=$?
fi

if [ $BUILD_RESULT -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "Build Successful!"
    echo "=========================================="
    echo ""
    echo "Output files:"
    echo "  - Main JAR: build/libs/dbb-git-migration-modeler-1.0.0.jar"
    echo "  - Fat JAR:  build/libs/dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar"
    echo "  - Dependencies: build/libs/lib/"
    echo ""
    echo "To run the application:"
    echo "  java -cp \"build/libs/dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar:\$DBB_HOME/lib/*\" \\"
    echo "    com.ibm.dbb.migration.ExtractApplications \\"
    echo "    -c /path/to/config.properties \\"
    echo "    -l /path/to/extract.log"
    echo ""
else
    echo ""
    echo "=========================================="
    echo "Build Failed!"
    echo "=========================================="
    exit 1
fi

# Made with Bob
