# Spring-Boot Plugin for JeKa

Adapt `project` or `base` KBean for Spring-Boot:

- Produce bootable jars
- Customize .war file for projectKBean
- Adapt scaffolding
- Include Spring Maven repositories for resolution
- Adapt Docker image generator to include port exposure

|Field  |Description  |Type  |
|-------|-------------|------|
|createOriginalJar |If true, create original jar artifact for publication (jar without embedded dependencies |boolean |
|createWarFile |If true, create a .war filed. |boolean |
|springRepo |Specific Spring repo where to download spring artifacts. Not needed if you use official release. |JkSpringRepo |
|aotProfiles |The springboot profiles that should be activated while processing AOT |String |
|exposedPorts |Space separated string of ports to expose. This is likely to be used by external tool as Docker. |String |


|Pre-initializer Method  |Description  |Pre-initialised KBean  |
|-------|-------------|------|
|initProjectKbean |Set test progress style to PLAIN to display JVM messages gracefully. |ProjectKBean |


|KBean Initialisation  |
|--------|
|Initialise `ProjectKBean` (or `BaseKBean) in order to:<br/><br/>- Produce bootable JAR file.<br/>- Adapt scaffolding to generate basic springboot application.<br/>- Add Spring Maven repositories.<br/>- Customize Docker image generator to export 8080 port. |


|Method  |Description  |
|--------|-------------|
|info |Provides info about this plugin configuration |


Resources:

- Command-line documentation: `jeka springboot: --doc`.
- Source Code: [Visit here](src/dev/jeka/plugins/springboot/SpringbootKBean.java).
- Example: [Simple app](https://github.com/jeka-dev/demo-springboot-simple).

## Quick Start

Create a Spring-Boot project from scratch:
```shell
jeka -cp=dev.jeka:springboot-plugin project: scaffold springboot:
```

This command downloads the plugin and initializes a Spring Boot project in the current working directory.
The scaffolded project contains workable code and configuration based on the latest Spring-Boot version.

## Initialization

The `SpringbootKBean` automatically configures *ProjectKBean*, *BaseKBean*, *DockerKBean*, and *NativeKBean* 
when any of these are present during initialization.
- **`ProjectKBean` or `BaseKBean`:**
  - Adds the Spring Boot BOM (Bill of Materials) to the project dependencies (optional).
  - Configures the project to produce a bootable JAR. WAR files and original artifacts can also be generated.
  - Enhances scaffolding operations.
- **`NativeKBean`:**
  - Allows profiles to be passed for activation in the compiled executable (via the `aotProfiles` property).
- **`DockerKBean`:**
  - Configures the ports to expose in the generated Dockerfile (via the `exposedPorts` property).
    Like all KBeans, these can be configured using **property files** or programmatically.

## Configuration

There's no required configuration. `jeka.properties` file allow to specify some settings as:

```properties
jeka.inject.classpath=dev.jeka:springboot-plugin
@springboot=

# Optional properties
@springboot.springRepos=MILESTONE
@springboot.aotProfiles=stage,mock
@springboot.exposedPort=80
@springboot.createWarFile=true
```

## Programmatic Usage

We can also configure a `JkProject` instance programmatically for working with Spring-Boot.

```java
JkProject project = myProject();
JkSpringbootProject.of(project)
        .configure()
        .includeParentBom("3.2.1");
```