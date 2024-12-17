# Spring Boot Plugin for JeKa
A plugin designed to simplify building Spring Boot applications with minimal effort.  
This plugin provides a [KBean](src/dev/jeka/plugins/springboot/SpringbootKBean.java)
and a library that streamlines building Spring Boot applications, especially bootable JARs.

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
The scaffolded project contains workable code and configuration.

## Initialization

The [SpringbootKBean](src/dev/jeka/plugins/springboot/SpringbootKBean.java) automatically configures
*ProjectKBean*, *BaseKBean*, *DockerKBean*, and *NativeKBean* when any of these are present during initialization.
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
@springboot.springRepos=MILESTONE
@springboot.aotProfiles=stage,mock
@springboot.exposedPort=80
@springboot.createWarFile=true
```
## Programmatic Configuration
We can also configure a `JkProject` instance programmatically for working with Spring-Boot.
```java
import dev.jeka.core.api.project.JkProject;
import dev.jeka.plugins.springboot.JkSpringbootProject;
import dev.jeka.plugins.springboot.SpringbootKBean;
@JkDep("dev.jeka:springboot-plugin")
class MyBuild extends KBean {
    JkProject project = JkProject.of();
    @Override
    protected void init() {
        JkSpringbootProject.of(project)
                .configure()
                .includeParentBom("3.2.1");
        // ... configure your JkProject instance as needed.
    }
}
```