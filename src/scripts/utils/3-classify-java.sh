#!/bin/env bash
#*******************************************************************************
# Licensed Materials - Property of IBM
# (c) Copyright IBM Corporation 2018, 2025. All Rights Reserved.
#
# Note to U.S. Government Users Restricted Rights:
# Use, duplication or disclosure restricted by GSA ADP Schedule
# Contract with IBM Corp. 
#*******************************************************************************
 
# Internal variables
DBB_GIT_MIGRATION_MODELER_CONFIG_FILE=
APPLICATION_FILTER=
rc=0

# Get Options
if [ $rc -eq 0 ]; then
	while getopts "c:a:" opt; do
		case $opt in
		c)
			argument="$OPTARG"
			nextchar="$(expr substr $argument 1 1)"
			if [ -z "$argument" ] || [ "$nextchar" = "-" ]; then
				rc=4
				ERRMSG="[ERROR] DBB Git Migration Modeler Configuration file required. rc="$rc
				echo $ERRMSG
				break
			fi
			DBB_GIT_MIGRATION_MODELER_CONFIG_FILE="$argument"
			;;
		a)
			argument="$OPTARG"
			nextchar="$(expr substr $argument 1 1)"
			if [ -z "$argument" ] || [ "$nextchar" = "-" ]; then
				rc=4
				ERRMSG="[ERROR] Comma-separated Applications list required. rc="$rc
				echo $ERRMSG
				break
			fi
			APPLICATION_FILTER="$argument"
			;;
		esac
	done
fi
#

# Validate Options
validateOptions() {
	if [ -z "${DBB_GIT_MIGRATION_MODELER_CONFIG_FILE}" ]; then
		rc=8
		ERRMSG="[ERROR] Argument to specify DBB Git Migration Modeler configuration file (-c) is required. rc="$rc
		echo $ERRMSG
	fi
	
	if [ ! -f "${DBB_GIT_MIGRATION_MODELER_CONFIG_FILE}" ]; then
		rc=8
		ERRMSG="[ERROR] DBB Git Migration Modeler configuration file not found. rc="$rc
		echo $ERRMSG
	fi
}

# Call validate Options
if [ $rc -eq 0 ]; then
 	validateOptions
fi

# Determine DBB_MODELER_HOME from config file
if [ $rc -eq 0 ]; then
	DBB_MODELER_HOME=$(grep "^DBB_MODELER_HOME=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	if [ -z "$DBB_MODELER_HOME" ]; then
		rc=8
		ERRMSG="[ERROR] DBB_MODELER_HOME not found in configuration file. rc="$rc
		echo $ERRMSG
	fi
fi

# Set up Java classpath
if [ $rc -eq 0 ]; then
	JAR_FILE="$DBB_MODELER_HOME/build/libs/dbb-git-migration-modeler-1.0.0.jar"
	LIB_DIR="$DBB_MODELER_HOME/build/libs/lib"
	
	if [ ! -f "$JAR_FILE" ]; then
		rc=8
		ERRMSG="[ERROR] JAR file not found: $JAR_FILE. Please run 'gradlew build' first. rc="$rc
		echo $ERRMSG
	fi
	
	# Build classpath with all dependencies
	CLASSPATH="$JAR_FILE"
	if [ -d "$LIB_DIR" ]; then
		for jar in "$LIB_DIR"/*.jar; do
			CLASSPATH="$CLASSPATH:$jar"
		done
	fi
	
	# Add DBB libraries if DBB_HOME is set
	if [ -n "$DBB_HOME" ]; then
		CLASSPATH="$CLASSPATH:$DBB_HOME/lib/*"
	fi
fi

if [ $rc -eq 0 ]; then
	# Load required configuration properties
	DBB_MODELER_METADATASTORE_TYPE=$(grep "^DBB_MODELER_METADATASTORE_TYPE=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	DBB_MODELER_FILE_METADATA_STORE_DIR=$(grep "^DBB_MODELER_FILE_METADATA_STORE_DIR=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	DBB_MODELER_APPLICATION_DIR=$(grep "^DBB_MODELER_APPLICATION_DIR=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	DBB_MODELER_LOGS=$(grep "^DBB_MODELER_LOGS=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	
	# Drop and recreate the Build MetadataStore folder
	if [ "$DBB_MODELER_METADATASTORE_TYPE" = "file" ]; then
		if [ -d $DBB_MODELER_FILE_METADATA_STORE_DIR ]; then
			rm -rf $DBB_MODELER_FILE_METADATA_STORE_DIR
		fi
		if [ ! -d $DBB_MODELER_FILE_METADATA_STORE_DIR ]; then
			mkdir -p $DBB_MODELER_FILE_METADATA_STORE_DIR
		fi
	fi

	# Adding commas before and after the passed parm, to search for pattern including commas
	APPLICATION_FILTER=",${APPLICATION_FILTER},"

	# Scan files
	cd $DBB_MODELER_APPLICATION_DIR
	for applicationDir in $(ls | grep -v dbb-zappbuild)
	do
		# If no parm specified or if the specified list of applications contains the current application
		if [ "$APPLICATION_FILTER" == ",," ] || [[ ${APPLICATION_FILTER} == *",${applicationDir},"* ]]; then
			echo "*******************************************************************"
			echo "Scan application directory '$DBB_MODELER_APPLICATION_DIR/$applicationDir'"
			echo "*******************************************************************"
			CMD="java -cp \"$CLASSPATH\" com.ibm.dbb.migration.ScanApplication -c \"$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE\" -a \"$applicationDir\""
			echo "[INFO] ${CMD}" >> $DBB_MODELER_LOGS/3-$applicationDir-scan.log
			eval $CMD >> $DBB_MODELER_LOGS/3-$applicationDir-scan.log 2>&1
			rc=$?
			
			if [ $rc -ne 0 ]; then
				echo "[ERROR] Scan failed for application '$applicationDir'. rc=$rc"
				break
			fi
		fi
	done

	# Assess file usage across applications
	if [ $rc -eq 0 ]; then
		cd $DBB_MODELER_APPLICATION_DIR
		for applicationDir in $(ls | grep -v dbb-zappbuild)
		do
			# If no parm specified or if the specified list of applications contains the current application
			if [ "$APPLICATION_FILTER" == ",," ] || [[ ${APPLICATION_FILTER} == *",${applicationDir},"* ]]; then
				echo "*******************************************************************"
				echo "Assess Include files & Programs usage for '$applicationDir'"
				echo "*******************************************************************"
				CMD="java -cp \"$CLASSPATH\" com.ibm.dbb.migration.AssessUsage -c \"$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE\" -a \"$applicationDir\""
				echo "[INFO] ${CMD}" >> $DBB_MODELER_LOGS/3-$applicationDir-assessUsage.log
				eval $CMD >> $DBB_MODELER_LOGS/3-$applicationDir-assessUsage.log 2>&1
				rc=$?
				
				if [ $rc -ne 0 ]; then
					echo "[ERROR] Assess usage failed for application '$applicationDir'. rc=$rc"
					break
				fi
			fi
		done
	fi
fi

exit $rc

# Made with Bob
