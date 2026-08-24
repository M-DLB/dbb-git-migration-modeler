# The DBB Git Migration Modeler utility

## Description

This asset provides a guided approach to plan and migrate source codebase from MVS datasets to z/OS UNIX System Services (USS) folders, and helps to identify and document the boundaries of mainframe applications.

The main capabilities of the DBB Git Migration Modeler utility are:
* the identification of applications based on naming conventions applied to datasets members names
* the copy of source code files contained in datasets members to files on USS following a standard repository layout,
* the usage assessment of include files and submodules across all applications to understand application-level dependencies, documented in the **Application Descriptor** file,
* the generation of build properties based on existing types and configuration in the legacy SCM solution
* the initialization of Git repositories, pipeline templates and other configuration files related to the applications

The different phases of the migration workflow are described in [the Migration Storyboard section](docs/01-Storyboard.md#migration-storyboard).

## How does it work

The DBB Git Migration Modeler is a Java application packaged as a JAR file. It must be built first using Gradle, then invoked through the provided shell scripts.

The utility comes with 2 primary scripts:

* The [Setup script](./Setup.sh) is used to define parameters that will be used throughout the migration process with the DBB Git Migration Modeler. It must be executed first and once, to set the defined parameters and create a configuration file that will be used in subsequent steps.
* The [Migration-Modeler-Start script](./Migration-Modeler-Start.sh) runs the different phases of the migration process by invoking the `MigrationOrchestrator` Java class.

An additional script is available for refreshing Application Descriptor files for applications already migrated to Git:
* The [Refresh-Application-Descriptor-Files script](./src/scripts/Refresh-Application-Descriptor-Files.sh) can be used to refresh Application Descriptor files for applications that were already migrated to Git.

## Building the application

### Option 1: Using the Gradle Wrapper (Recommended)

```bash
./gradlew clean build
```

### Option 2: Using an Installed Gradle on z/OS (Without Gradle Wrapper)

If using a pre-installed Gradle on z/OS instead of the wrapper:

1. Ensure `JAVA_HOME` and `GRADLE_HOME` are set, and `gradle` is added to `PATH`:
   ```bash
   export JAVA_HOME=/usr/lpp/java/J8.0_64   # adjust to your Java installation path
   export GRADLE_HOME=/usr/lpp/gradle/gradle-8.5  # adjust to your Gradle installation path
   export PATH=$GRADLE_HOME/bin:$JAVA_HOME/bin:$PATH
   ```

2. Set environment variables for encoding/conversion and daemon network binding:
   ```bash
   export _BPXK_AUTOCVT=ON
   export _TAG_REDIR_IN=TXT
   export _TAG_REDIR_OUT=TXT
   export _TAG_REDIR_ERR=TXT

   # Bind Gradle daemon communication explicitly to the local loopback interface (required on z/OS when using Gradle daemon)
   export GRADLE_DAEMON_BIND_ADDRESS=127.0.0.1
   ```

3. Run Gradle directly:
   ```bash
   gradle clean build
   ```

Both options produce the JAR file at `build/libs/dbb-git-migration-modeler-2.0.0.jar` along with its runtime dependencies in `build/libs/lib/`.

## Required setup and configuration

The DBB Git Migration Modeler is using two types of configuration information:
* A configuration file created during the Setup phase, which is used as input for all migration phases.
* Configuration files, shipped in the [samples directory](./samples/) that must be tailored to meet requirements.

The configuration parameters collected during the Setup phase are described in [Setting up the DBB Git Migration Modeler configuration](docs/02-Setup.md#setting-up-the-dbb-git-migration-modeler-configuration) section.
The different Configuration files are described in the [Configuring input files](docs/03-Configuration.md#configuring-the-migration-modeler-input-files) section.


## Cross-application dependencies and the Application Descriptor files

The DBB Git Migration Modeler utility is used to better scope applications' repositories and helps to understand dependencies between applications.
After the execution of the [Assessment Phase](docs/01-Storyboard.md#the-assessment-phase), the dependencies between applications are knwon and documented in each Application Descriptor file. For a given application, its Application Descriptor file lists:
* the other applications that are considered as dependencies (i.e., the build and the deployment of the given application cannot be correctly performed without these dependencies),
* the other applications that are consuming a service (either an Include File or a Submodule) that the given application provides

The Application Descriptor files are meant to also describe the list of artifacts belonging to their application. Artifacts are classed into groups, each group represent a type of artifact (COBOL programs, COBOL copybooks, BMS maps, etc.).

## Migrations scenarios for the DBB Git Migration Modeler

The DBB Git Migration Modeler can support multiple scenarios, from simple configuration to complex architecture. Complexity is generally due to the number of input datasets that contain sources.

In such situations, it is recommended to run the [Framing phase](docs/01-Storyboard.md#the-framing-phase) with multiple *Applications Mapping* files, that may contain different input datasets and naming conventions. Different configuration scenarios are outlined in the [Define Applications Mapping files](docs/03-Configuration.md#define-applications-mapping-files) section.