#!/bin/bash
#********************************************************************************
# Licensed Materials - Property of IBM                                          *
# (c) Copyright IBM Corporation 2018, 2024. All Rights Reserved.                *
#                                                                               *
# Note to U.S. Government Users Restricted Rights:                              *
# Use, duplication or disclosure restricted by GSA ADP Schedule                 *
# Contract with IBM Corp.                                                       *
#********************************************************************************

# Wrapper script to run ExtractApplications Java application

# Usage function
usage() {
    echo "Usage: $0 -c <config-file> [-a <applications>] [-l <log-file>]"
    echo ""
    echo "Options:"
    echo "  -c, --configFile <path>      Path to the DBB Git Migration Modeler Configuration file (required)"
    echo "  -a, --applications <list>    Comma-separated list of applications to extract (optional)"
    echo "  -l, --logFile <path>         Relative or absolute path to an output log file (optional)"
    echo "  -h, --help                   Display this help message"
    echo ""
    echo "Environment Variables:"
    echo "  DBB_HOME                     Path to DBB installation (required)"
    echo "  JAVA_HOME                    Path to Java installation (optional, uses system java if not set)"
    echo ""
    echo "Example:"
    echo "  $0 -c /path/to/config.properties -l /path/to/extract.log"
    echo "  $0 -c /path/to/config.properties -a \"APP1,APP2\" -l /path/to/extract.log"
    exit 1
}

# Check if DBB_HOME is set
if [ -z "$DBB_HOME" ]; then
    echo "ERROR: DBB_HOME environment variable is not set"
    echo "Please set DBB_HOME to your DBB installation directory"
    exit 1
fi

# Check if DBB_HOME exists
if [ ! -d "$DBB_HOME" ]; then
    echo "ERROR: DBB_HOME directory does not exist: $DBB_HOME"
    exit 1
fi

# Determine Java command
if [ -n "$JAVA_HOME" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    JAVA_CMD="java"
fi

# Check if Java is available
if ! command -v "$JAVA_CMD" &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 8+ or set JAVA_HOME"
    exit 1
fi

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Check if JAR file exists
JAR_FILE="$PROJECT_ROOT/target/dbb-git-migration-modeler-1.0.0-jar-with-dependencies.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "ERROR: JAR file not found: $JAR_FILE"
    echo "Please build the project first using: ./src/java/build.sh"
    exit 1
fi

# Build classpath
CLASSPATH="$JAR_FILE:$DBB_HOME/lib/*"

# Parse command-line arguments
ARGS=""
while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--configFile)
            ARGS="$ARGS -c \"$2\""
            shift 2
            ;;
        -a|--applications)
            ARGS="$ARGS -a \"$2\""
            shift 2
            ;;
        -l|--logFile)
            ARGS="$ARGS -l \"$2\""
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            echo "ERROR: Unknown option: $1"
            usage
            ;;
    esac
done

# Check if config file was provided
if [[ ! "$ARGS" =~ "-c" ]]; then
    echo "ERROR: Configuration file is required"
    usage
fi

# Run the application
echo "=========================================="
echo "Running ExtractApplications"
echo "=========================================="
echo "Java: $JAVA_CMD"
echo "JAR: $JAR_FILE"
echo "DBB_HOME: $DBB_HOME"
echo "Arguments: $ARGS"
echo "=========================================="
echo ""

eval "$JAVA_CMD -cp \"$CLASSPATH\" com.ibm.dbb.migration.ExtractApplications $ARGS"

EXIT_CODE=$?

echo ""
if [ $EXIT_CODE -eq 0 ]; then
    echo "=========================================="
    echo "Extraction completed successfully"
    echo "=========================================="
else
    echo "=========================================="
    echo "Extraction failed with exit code: $EXIT_CODE"
    echo "=========================================="
fi

exit $EXIT_CODE

# Made with Bob
