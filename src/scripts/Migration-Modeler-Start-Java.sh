#!/bin/env bash
#*******************************************************************************
# Licensed Materials - Property of IBM
# (c) Copyright IBM Corporation 2018, 2025. All Rights Reserved.
#
# Note to U.S. Government Users Restricted Rights:
# Use, duplication or disclosure restricted by GSA ADP Schedule
# Contract with IBM Corp.
#*******************************************************************************

Prolog() {
	echo
	echo " DBB Git Migration Modeler (Java Edition)                                                                   "
	echo " Release:     $MigrationModelerRelease                                                                      "
	echo
	echo " Script:      Migration-Modeler-Start-Java.sh                                                               "
	echo
	echo " Description: The purpose of this script is to facilitate the execution of the 4-step process supported     "
	echo "              by the DBB Git Migration Modeler using Java implementations.                                  "
	echo "              For more information please refer to:    https://github.com/IBM/dbb-git-migration-modeler     "
	echo
}

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
			APPLICATION_FILTER="-a $argument"
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
	fi
	
	if [ ! -f "${DBB_GIT_MIGRATION_MODELER_CONFIG_FILE}" ]; then
		rc=8
		ERRMSG="[ERROR] DBB Git Migration Modeler configuration file not found. rc="$rc
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
	# Validate environment using Java
	java -cp "$CLASSPATH" com.ibm.dbb.migration.ValidateConfiguration -c "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE"
	rc=$?

	if [ $rc -ne 0 ]; then
		exit $rc
	fi

	# Load configuration properties
	export DBB_MODELER_HOME=$(grep "^DBB_MODELER_HOME=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	export DBB_MODELER_WORK=$(grep "^DBB_MODELER_WORK=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	export DBB_MODELER_APPCONFIG_DIR=$(grep "^DBB_MODELER_APPCONFIG_DIR=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	export DBB_MODELER_APPLICATION_DIR=$(grep "^DBB_MODELER_APPLICATION_DIR=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	export DBB_MODELER_LOGS=$(grep "^DBB_MODELER_LOGS=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	export DBB_MODELER_FILE_METADATA_STORE_DIR=$(grep "^DBB_MODELER_FILE_METADATA_STORE_DIR=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	export DBB_MODELER_BUILD_CONFIGURATION=$(grep "^DBB_MODELER_BUILD_CONFIGURATION=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	export DBB_MODELER_APPMAPPINGS_DIR=$(grep "^DBB_MODELER_APPMAPPINGS_DIR=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	export INTERACTIVE_RUN=$(grep "^INTERACTIVE_RUN=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	export BUILD_FRAMEWORK=$(grep "^BUILD_FRAMEWORK=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)
	export PIPELINE_CI=$(grep "^PIPELINE_CI=" "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" | cut -d'=' -f2)

	PGM="Migration-Modeler-Start-Java.sh"
	# Print Prolog
	export MigrationModelerRelease=$(cat $DBB_MODELER_HOME/release.properties | awk -F 'Migration-Modeler-release=' '{printf $2}')
	Prolog
fi
	
if [ $rc -eq 0 ]; then
	echo ""
	echo "[PHASE] Cleanup working directories"
	if [[ $INTERACTIVE_RUN == "true" ]]; then
		read -p "Do you want to clean the working directory '$DBB_MODELER_WORK' (Y/n): " variable
	else
		variable="Y"
	fi

	if [[ -z "$variable" || $variable =~ ^[Yy]$ ]]; then

		#### Cleanup output directories
		if [ -d $DBB_MODELER_APPCONFIG_DIR ]; then
			rm -rf $DBB_MODELER_APPCONFIG_DIR
			echo "[INFO] Removed '${DBB_MODELER_APPCONFIG_DIR}' folder"
		fi
		if [ -d $DBB_MODELER_APPLICATION_DIR ]; then
			rm -rf $DBB_MODELER_APPLICATION_DIR
			echo "[INFO] Removed '${DBB_MODELER_APPLICATION_DIR}' folder"
		fi
        if [ -d $DBB_MODELER_LOGS ]; then
            rm -rf $DBB_MODELER_LOGS
            echo "[INFO] Removed '${DBB_MODELER_LOGS}' folder"
        fi
        if [ -d $DBB_MODELER_BUILD_CONFIGURATION ]; then
            rm -rf $DBB_MODELER_BUILD_CONFIGURATION
            echo "[INFO] Removed '${DBB_MODELER_BUILD_CONFIGURATION}' folder"
        fi
        if [ -d $DBB_MODELER_FILE_METADATA_STORE_DIR ]; then
            rm -rf $DBB_MODELER_FILE_METADATA_STORE_DIR
            echo "[INFO] Removed '${DBB_MODELER_FILE_METADATA_STORE_DIR}' folder"
        fi
	fi
fi

if [ $rc -eq 0 ]; then
	#### Create work directories
	if [ ! -d $DBB_MODELER_LOGS ]; then
		mkdir -p $DBB_MODELER_LOGS
		echo "[INFO] Created '${DBB_MODELER_LOGS}' folder"
		rc=$?
	fi
fi

if [ $rc -eq 0 ]; then
	echo
	echo "[PHASE] Extract applications from using Applications Mapping files located at '$DBB_MODELER_APPMAPPINGS_DIR'"
	if [[ $INTERACTIVE_RUN == "true" ]]; then
		read -p "Do you want run the application extraction (Y/n): " variable
	else
		variable="Y"
	fi

	if [[ -z "$variable" || $variable =~ ^[Yy]$ ]]; then
	 	
		#### Application Extraction step (Java)
		$DBB_MODELER_HOME/src/scripts/utils/1-extractApplications-java.sh -c "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" $APPLICATION_FILTER
	   	rc=$?
	fi
fi

if [ $rc -eq 0 ]; then
	#### Migration execution step (Java)
	echo
	echo "[PHASE] Execute migrations using DBB Migration mapping files stored in '$DBB_MODELER_APPCONFIG_DIR'"
	if [[ $INTERACTIVE_RUN == "true" ]]; then
		read -p "Do you want to execute the migration using DBB Migration utility (Y/n): " variable
	else
		variable="Y"
	fi
	
	if [[ -z "$variable" || $variable =~ ^[Yy]$ ]]; then
		$DBB_MODELER_HOME/src/scripts/utils/2-runMigrations-java.sh -c "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" $APPLICATION_FILTER
	   	rc=$?
	fi
fi

if [ $rc -eq 0 ]; then
	#### Classification step (Java)
	echo
	echo "[PHASE] Assess usage and perform classification"
	if [[ $INTERACTIVE_RUN == "true" ]]; then
		read -p "Do you want to perform the usage assessment and classification process (Y/n): " variable
	else
		variable="Y"
	fi
	if [[ -z "$variable" || $variable =~ ^[Yy]$ ]]; then
		$DBB_MODELER_HOME/src/scripts/utils/3-classify-java.sh -c "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" $APPLICATION_FILTER
	   	rc=$?
	fi
fi

if [ $rc -eq 0 ]; then
	#### Property Generation step (Java)
	echo
	echo "[PHASE] Generate build configuration"
	if [[ $INTERACTIVE_RUN == "true" ]]; then
		read -p "Do you want to generate the $BUILD_FRAMEWORK Configuration files (Y/n): " variable
	else
		variable="Y"
	fi
	if [[ -z "$variable" || $variable =~ ^[Yy]$ ]]; then
		$DBB_MODELER_HOME/src/scripts/utils/4-generateProperties-java.sh -c "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" $APPLICATION_FILTER
	   	rc=$?
	fi
fi

repositoriesInitialized=false

if [ $rc -eq 0 ]; then
	#### Application repository initialization (Java)
	echo
	echo "[PHASE] Initialize application's repositories"
	if [[ $INTERACTIVE_RUN == "true" ]]; then
		read -p "Do you want to initialize application's repositories (Y/n): " variable
	else
		variable="Y"
	fi
	if [[ -z "$variable" || $variable =~ ^[Yy]$ ]]; then
		$DBB_MODELER_HOME/src/scripts/utils/5-initApplicationRepositories-java.sh -c "$DBB_GIT_MIGRATION_MODELER_CONFIG_FILE" $APPLICATION_FILTER
		rc=$?
		if [ $rc -eq 0 ]; then
			repositoriesInitialized=true
		fi
	fi
fi

if [ $rc -eq 0 ]; then
	#### Summary
	echo
	echo "[PHASE] Summary"
	java -cp "$CLASSPATH" com.ibm.dbb.migration.CalculateDependenciesOrder -a "$DBB_MODELER_APPLICATION_DIR"
	if [ "$repositoriesInitialized" = true ]; then
		echo
		echo "***********************************************************************************************************"
		echo "*************************************    What needs to be done now    *************************************"
		echo "***********************************************************************************************************"
		echo
		
		case $PIPELINE_CI in
			AzureDevOps)
				GitDistribution="Azure DevOps platform"
				PipelineOrchestrator="Azure DevOps"
			;;
			GitlabCI)
				GitDistribution="GitLab platform"
				PipelineOrchestrator="GitLab CI"
			;;
			GitHubActions)
				GitDistribution="GitHub platform"
				PipelineOrchestrator="GitHub Actions"
			;;
			*)
				GitDistribution="Git Central server"
				PipelineOrchestrator="Pipeline Orchestrator's"
			;;
		esac
		
		
		echo "For each application:                                                                                      "
		echo "- Create a Git project in your $GitDistribution                                                            "
		echo "- Add a remote configuration for the application's Git repository on USS using the 'git remote add' command"
		echo "- Initialize the $PipelineOrchestrator variables in the pipeline configuration"
		echo "- Push the application's Git repository in the order of the above ranked list                            "
		echo
		echo "***********************************************************************************************************"
	fi	
fi

if [ $rc -ne 0 ]; then
	echo ${ERRMSG}
	exit $rc
fi

# Made with Bob
