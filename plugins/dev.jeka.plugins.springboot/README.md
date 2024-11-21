# Spring-Boot Plugin for JeKa

Plugin to build Spring-Boot applications with minimal effort. <br/>

This plugin contains a [KBean](src/dev/jeka/plugins/springboot/SpringbootKBean.java) and a library to handle the build of Spring-Boot applications, and specially 
the creation of a bootable jar.

## Create a new Spring-Boot Project from Scratch

To create a new project from scratch, execute :

```shell
jeka scaffold#run scaffold#wrapper @dev.jeka:springboot-plugin springboot#
``` 
This will download this plugin, and scaffold a Spring-Boot project for you in the working dir.

It is possible to scaffold a project relying on properties only instead of build code.
```shell
jeka scaffold#run scaffold#wrapper @dev.jeka:springboot-plugin springboot#scaffoldKind=PROPS
``` 

> [!NOTE]
> You need JeKa installed on your machine to execute these command lines.
  Alternatively, Jeka Ide for IntelliJ does not required to have Jeka installed.


## Configure using KBeans 

[SpringbootKBean](src/dev/jeka/plugins/springboot/SpringbootKBean.java) is designed to auto-configure
*ProjectKBean*, *SelfAppKBean* and *ScaffoldKBean* when any of them is present in the runtime.

It configures following KBeans when it is loaded in the runbase :
- `ProjectKBean` : 
  - Adds Spring-Boot bom to dependencies of the project.
  - Instructs project to create bootable jar. War file and original artifacts can be generated as well
  - Sets sensitive default as not producing javadoc or source artifacts.
- `SelfAppKBean` : Instructs app to create bootable jar.
- `ScaffoldKBean` : Creates specific build class and simple Spring-Boot app template code.

As all KBean, this can be instantiated both using *property file* or *programmatically*.

```properties
jeka.classpath.inject=dev.jeka:springboot-plugin

@springboot=
@springboot.springbootVersion=3.2.1
```
Visit [source code](src/dev/jeka/plugins/springboot/SpringbootKBean.java) to see available options.

## Configure Programmatically

For greater control, it is possible to configure any `JkProject` instance to be built as a Spring-Boot project.

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

## Native Support (Experimental)

### Generate Native Image Executable

To create a native executable of your application for the current platform, run the following command:

```shell
jeka native: compile
```

It is possible to specify the Spring profiles to active at build time :

```shell
jeka native: compile springboot: nativeOps.aotProfiles=profile-a,profile-b
```

### Create Docker Image running the native executable

To create a Docker Image of the native application, execute:

```shell
jeka docker: buildNative
```
This will create a docker image, running the springboot executable application with non-root user.

Note:
If your running on Windows or Macos, Jeka will run the native compilation into a Docker container, in order it is generated 
for Linux.

To see the content of the generated image, execute:

```shell
jeka docker: infoNative
```

Visit [source code](src/dev/jeka/plugins/springboot/JkSpringbootProject) to see available options.



