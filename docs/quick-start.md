# Quick Start

Welcome to JeKa! This guide will help you get up and running in minutes.

## Installation

You can get JeKa in two ways:

- **IntelliJ Plugin (Recommended)**: Install the [JeKa Plugin](https://plugins.jetbrains.com/plugin/24505-jeka). It's the fastest way to start, providing wizards for scripts, apps, and Spring Boot projects.
- **CLI**: [Install JeKa CLI](installation.md) manually or via SDKMAN!: `sdk install jeka`.

## Choose Your Path

This guide covers several use cases:

- [Create scripts in Java](#create-scripts) to automate your tasks.
- [Create a Base Application or Library](#create-a-base-application-or-library) for simple, pure Java projects.
- [Create a Java Project](#create-a-java-project) for more complex, standard layouts.
- [Create a Spring Boot Project](#create-a-spring-boot-project) with a pre-configured setup.

  
!!! tip "IntelliJ IDEA Users"
    After scaffolding or modifying dependencies, synchronize your project by executing:
    ```bash
    jeka intellij: sync
    ```

## Create Scripts

Create a directory to host the codebase. Navigate into it and execute:
```bash
jeka base: scaffold
```
This generates a structure as:
```
.                        <- Project root directory
├── jeka-src             <- Source dir for JeKa scripts and configuration code
│   └── Script.java     
└── jeka.properties      <- JeKa configuration (Java and JeKa version, default parameters...)
```
This script class looks like:
```java
@JkDoc("Minimalist script for demo purpose.")
class Script extends KBean {

    @JkDoc("Person to whom the greeting is intended")
    public String name = "World";

    @JkDoc("Print greeting on console")
    public void hello() {
        String greetings = "Hello " + name + " !";
        System.out.println(greetings);
    }
}
```
#### Run methods
You can run the method `hello()` and change the parameter, by executing:
```bash
jeka hello name="JeKa"
```
This displays the following text on the console:
```
Hello JeKa !
```
#### Write Extra Methods
You can add extra methods relying or not on third-party dependencies as:
```java
import com.github.lalyos.jfiglet.FigletFont;
import com.google.common.base.Strings;
import dev.jeka.core.tool.JkDep;
import dev.jeka.core.tool.KBean;

@JkDep("com.github.lalyos:jfiglet:0.0.9")
@JkDep("com.google.guava:guava:33.3.1-jre")
class Script extends KBean {

    public void header() throws Exception {
        System.out.println(Strings.repeat("-", 80));
        System.out.println(FigletFont.convertOneLine("Hello Ascii Art !"));
        System.out.println(Strings.repeat("-", 80));
    }
}
```
Execute:
```bash
jeka header
```
This will display the following text on the console:
```
--------------------------------------------------------------------------------
  _   _      _ _            _             _ _      _         _     _ 
 | | | | ___| | | ___      / \   ___  ___(_|_)    / \   _ __| |_  | |
 | |_| |/ _ \ | |/ _ \    / _ \ / __|/ __| | |   / _ \ | '__| __| | |
 |  _  |  __/ | | (_) |  / ___ \\__ \ (__| | |  / ___ \| |  | |_  |_|
 |_| |_|\___|_|_|\___/  /_/   \_\___/\___|_|_| /_/   \_\_|   \__| (_)
                                                                     

--------------------------------------------------------------------------------
```
!!! note
    - You can define multiple script methods in `Script.java`. These methods must be public, non-static, take no arguments, and return `void`.
    - You can rename `Script.java` to any name and place it in any package.
    - You can create multiple script classes. To run a specific script, use the class name, e.g., `jeka script2: hi`.
    - You can also [use classes provided by JeKa](reference/api-intro.md) without explicitly declaring them.

#### KBeans

Every script should inherit from the `KBean` class. 

KBeans can either be provided as source code (located in the `jeka-src` directory) or as compiled classes available in the classpath.

JeKa includes several standard KBeans, which you can list by running:
```bash
jeka --doc
```
#### Change Java Version
To change version of Java, edit `jeka.properties`:
```properties
jeka.java.version=23
```
This will automatically download Java 23 (if not already installed) on the next method run.

#### Source-Runnable Applications

Run `hello` from another directory:
```bash
jeka -r /path/to/script/root-dir hello
```

Run `hello` from a remote Git repository:
```bash
jeka -r https://github.com/jeka-dev/demo-base hello
```

JeKa acts as an application source manager, allowing you to run or install applications directly from their source code.
For more details, see the [Source-Runnable Applications tutorial](tutorials/source-runnable-apps.md).

#### Common Options & Commands

```bash
jeka --help          # Displays help message
jeka --doc           # Displays documentation on available KBeans
jeka --inspect       # Displays details about JeKa setup and properties
jeka base: depTree   # Show dependency tree
```

#### Resources
- [Basics Tutorial](tutorials/basics.md)
- [Write Scripts in Java Video](https://youtu.be/r-PUX6amBrw)

## Create a Base Application or Library

JeKa's *base* mode is the simplest way to build pure Java applications or libraries. It avoids the complexity of traditional Maven/Gradle structures while supporting full-featured builds, testing, native compilation, and Docker packaging.

To create a new code structure, run the following command:
```bash
jeka base: scaffold scaffold.kind=APP
```
This creates a structure like this:
```
. 
├── jeka-src             <- Source root directory
│   ├── _dev             <- Optional package containing all non-prod (build and test)
│   │   ├── test
│   │   └── Build.java  
│   └── app              <- Suggested base package for production code/resources
│       └── App.java     
├── jeka-output          <- Directory where generated artifacts (JARs, reports) are stored
├── jeka.properties      <- Build configuration (Java and JeKa version, KBean configurations, ...)
└── README.md            <- Describes available build commands
```

Follow the [tutorial](tutorials/build-base.md) for more details.

## Create a Java Project

For more complex needs, *Project* mode provides a standard layout similar to Maven or Gradle, supporting multi-module builds and advanced dependency management.

To create a new project structure, execute:
```bash
jeka project: scaffold
```
This generates a project structure as:
```
.
├── src                  
│   ├── main             <- Java code and resources
│   │   ├── java
│   │   └── resources    
│   └── test             <- Java code and resources for tests
│       ├── java
│       └── resources 
├── jeka-src             <- Optional Java (or Kotlin) code for building the project
│   └── Build.java      
├── jeka-output          <- Directory where generated artifacts (JARs, reports) are stored
├── jeka.project.deps   <- Dependency lists for compile, runtime and testing
├── jeka.properties      <- Build configuration (Java and JeKa version, KBean configurations, ...)
├── jeka.ps              <- Optional PowerShell script to boot JeKa on Windows
├── jeka                 <- Optional bash script to boot JeKa on Linux/MacOS
└── README.md            <- Describes available build commands for building the project
```

Follow the [tutorial](tutorials/build-projects.md) for more details.

## Create a Spring Boot Project

To create a new Spring Boot project, execute:
```bash
jeka -cp=dev.jeka:springboot-plugin project: scaffold springboot:
```

This generates the following project structure:
```
.
├── src                  
│   ├── main             
│   │   ├── java
│   │   │   └── app
│   │   │       ├── Application.java      <- Spring Boot app class
│   │   │       └── Controller.java       <- REST controller
│   │   └── resources    
│   └── test             
│       ├── java
│       │   └── app
│       │       └── ControllerIT.java     <- Integration Test for REST controller 
│       └── resources 
├── jeka-src             
│   └── Build.java       <- Optional build class (if needed)
├── jeka-output          <- Directory where generated artifacts (JARs, reports) are stored
├── jeka.project.deps   <- Spring Boot and extra dependencies
├── jeka.properties      <- Build configuration 
├── jeka.ps              
├── jeka                 
└── README.md            <- Describes available build commands for building the project
```
This contains a minimal workable project with production and test code. 

#### Modify Layout
You can choose a simpler code layout structure by setting the following properties:
```properties title="jeka.properties"
@project.layout.style=SIMPLE
@project.layout.mixSourcesAndResources=true
```
You'll end up with the following code layout:
```
.
├── src       <- Contains both Java code and resources    
├── test      <- Contains both Java code and resources for testing
```

#### Modify Dependencies
The dependencies are generated with the latest Spring Boot version:
```ini title="jeka.project.deps"
[version]
org.springframework.boot:spring-boot-dependencies:3.4.1@pom

[compile]
org.springframework.boot:spring-boot-starter-web

[test]
org.springframework.boot:spring-boot-starter-test
```
You can start from here for modifying, adding code, tests and dependencies.

#### Execute Commands

These are the most useful commands for developing Spring Boot applications.

```text title="Common Commands"
jeka project: test       # Compiles and run tests
jeka project: pack       # Compiles and creates Bootable Jar
jeka project: runJar     # Runs the bootable JAR
jeka project: build      # All-in-one: compile, test, pack, and verification
jeka project: depTree    # Displays the dependency tree

jeka docker: build       # Creates Docker image containing the Spring Boot application
jeka docker: buildNative # Creates Docker image for the native-compiled application
```

#### Customize Docker File

To reduce a Docker native image size, use a distroless base image. The native executable must be statically linked as libc is unavailable in such distributions. Configure it as follows:
```properties title="jeka.properties"
@native.staticLink=MUSL
@docker.nativeBaseImage=gcr.io/distroless/static-debian12:nonroot
```

#### What Next?

Explore more advanced topics and real-world examples:

- **[Source-Runnable Applications](tutorials/source-runnable-apps.md)**: Run or install apps directly from Git.
- **[Spring Boot + Angular + SonarQube Example](https://github.com/jeka-dev/demo-project-springboot-angular)**: A complete full-stack example.
- **[Kotlin + Spring Boot + ReactJS Example](https://github.com/jeka-dev/working-examples/tree/master/springboot-kotlin-reactjs)**: Using Kotlin with modern web frameworks.
- **[JeKa + Spring Boot + GraalVM Video](https://youtu.be/yfmaAwAjJ2w)**: Watch JeKa in action.
- **[Basics Tutorial](tutorials/basics.md)**: Deep dive into JeKa core concepts.

