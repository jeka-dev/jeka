# Spring-Boot Plugin for JeKa

Adapt `project` or `base` KBean for Spring-Boot:

- Produce bootable jars
- Customize .war file for projectKBean
- Adapt scaffolding
- Include Spring Maven repositories for resolution
- Adapt Docker image generator to include port exposure

**This KBean pre-initializes the following KBeans:**

| Pre-initialized KBean |Description  |
|-----------------------|-------------|
| ProjectKBean          |Sets test progress style to PLAIN to display JVM messages gracefully. |


**This KBean post-initializes the following KBeans:**

| Post-initialised KBean | Description                                                                                                                                               |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| BaseKBean              | Adapts base KBean: creates Bootable JAR on #pack, adds Springboot Maven repositories to dependency resolutions, forces tests to run in separated process. |
| DockerKBean            | Adds exposed ports to the built images.                                                                                                                   |
| NativeKBean            | Adds Springboot AOT step when building native executable.                                                                                                 |
| ProjectKBean           | Adapts project: creates Bootable JAR on #pack, adds Springboot Maven repositories to dependency resolutions, forces tests to run in separated process.    |


**This KBean exposes the following fields:**

| Field                          | Description                                                                                      |
|--------------------------------|--------------------------------------------------------------------------------------------------|
| createOriginalJar [boolean]    | If true, create original jar artifact for publication (jar without embedded dependencies.        |
| createWarFile [boolean]        | If true, create a .war filed.                                                                    |
| springRepo [enum:JkSpringRepo] | Specific Spring repo where to download spring artifacts. Not needed if you use official release. |
| aotProfiles [String]           | The springboot profiles that should be activated while processing AOT.                           |
| exposedPorts [String]          | Space separated string of ports to expose. This is likely to be used by external tool as Docker. |


**This KBean exposes the following methods:**

| Method    | Description                                    |
|-----------|------------------------------------------------|
| info      | Provides info about this plugin configuration. |


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



## Configuration example

```properties
jeka.classpath=dev.jeka:springboot-plugin
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