# Spring-Boot Plugin for JeKa

A plugin for building Spring Boot applications with minimal effort.  

This plugin provides a [KBean](src/dev/jeka/plugins/springboot/SpringbootKBean.java) 
and a library to simplify building Spring Boot applications, particularly bootable JARs.

**Command line documentation**: `jeka springboot: --doc`.

## Create a New Spring Boot Project from Scratch

To create a new Spring Boot project from scratch, run the following command:
```shell
jeka -cp=dev.jeka:springboot-plugin project: scaffold springboot:
```
This command downloads the plugin and creates a Spring Boot project in the current working directory.

## Configure Using KBeans

The [SpringbootKBean](src/dev/jeka/plugins/springboot/SpringbootKBean.java) is designed to auto-configure
*ProjectKBean*, *BaseKBean*, *DockerKBean*, and *NativeKBean* when any of these are present at init time.

- **`ProjectKBean` or `BaseKBean`:**
  - Adds the Spring Boot BOM (Bill of Materials) to the project dependencies (optional).
  - Configures the project to produce a bootable JAR. WAR files and original artifacts can also be generated.
  - Enhances scaffold operations.

- **`NativeKBean`:**
  - Pass profiles to activate in the compiled executable (via `aotProfiles` property)

- **`DockerKBean`:**
  - Pass the port to expose in the generated Dockerfile (via `exposedPorts` property)

Like all KBeans, this can be instantiated using **property files** or programmatically.  
Example of property file configuration:
```properties
jeka.classpath.inject=dev.jeka:springboot-plugin
@springboot=

@springboot.aotProfiles=stage,mock
...
```
For more details on available options, visit the [source code](src/dev/jeka/plugins/springboot/SpringbootKBean.java).

## Configure Programmatically
For more control, you can configure a `JkProject` instance programmatically to build a Spring Boot project.

Example:
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
    // ... configure your JkProject instance as usual.
  }

}
```

