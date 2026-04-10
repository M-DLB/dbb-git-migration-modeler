#!/bin/bash
#********************************************************************************
# Licensed Materials - Property of IBM                                          *
# (c) Copyright IBM Corporation 2018, 2024. All Rights Reserved.                *
#                                                                               *
# Note to U.S. Government Users Restricted Rights:                              *
# Use, duplication or disclosure restricted by GSA ADP Schedule                 *
# Contract with IBM Corp.                                                       *
#********************************************************************************

# Script to initialize Gradle wrapper

set -e

echo "=========================================="
echo "Initializing Gradle Wrapper"
echo "=========================================="

# Check if gradle is installed
if ! command -v gradle &> /dev/null; then
    echo "ERROR: Gradle is not installed"
    echo ""
    echo "Please install Gradle first:"
    echo ""
    echo "Option 1: Using SDKMAN (recommended)"
    echo "  curl -s \"https://get.sdkman.io\" | bash"
    echo "  source \"$HOME/.sdkman/bin/sdkman-init.sh\""
    echo "  sdk install gradle 8.5"
    echo ""
    echo "Option 2: Manual download"
    echo "  Download from: https://gradle.org/releases/"
    echo "  Extract and add to PATH"
    echo ""
    echo "Option 3: Using package manager"
    echo "  - Ubuntu/Debian: sudo apt-get install gradle"
    echo "  - macOS: brew install gradle"
    echo "  - RHEL/CentOS: sudo yum install gradle"
    echo ""
    exit 1
fi

GRADLE_VERSION=$(gradle --version | grep "Gradle" | awk '{print $2}')
echo "Found Gradle version: $GRADLE_VERSION"
echo ""

# Navigate to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Initializing Gradle wrapper in: $SCRIPT_DIR"
echo ""

# Generate wrapper
gradle wrapper --gradle-version=8.5

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "Gradle Wrapper Initialized Successfully!"
    echo "=========================================="
    echo ""
    echo "You can now build the project using:"
    echo "  ./gradlew clean build"
    echo ""
    echo "Or use the build script:"
    echo "  ./src/java/build.sh"
    echo ""
else
    echo ""
    echo "=========================================="
    echo "Gradle Wrapper Initialization Failed!"
    echo "=========================================="
    exit 1
fi

# Made with Bob
