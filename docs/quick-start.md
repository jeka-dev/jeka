# Quick Start

## Install JeKa
  
- Install [Jeka CLI](installation.md) 
- or Install [IntelliJ Plugin for JeKa](https://plugins.jetbrains.com/plugin/24505-jeka)

!!! note
    Using the [IntelliJ Plugin](https://plugins.jetbrains.com/plugin/24505-jeka) is the fastest way to get started. 
    It provides a wizard for creating various types of projects, ranging from simple scripts to full-fledged Spring Boot applications.

## Follow the Guide

This quick-start guide covers muliple use cases:
 
- [Create scripts in Java](#create-scripts) and execute from command line.
- [Create a Base Application or Library](#create-a-base-app-or-library).
- [Create a Java Project](#create-a-java-project)
- [Create a workable Spring-Boot Project](#create-a-spring-boot-project) in seconds

  
!!! notes

    If you are coding in IntelliJ IDEA, after scaffolding or modifying dependencies, execute the following command to synchronize:
    ```
    jeka intellij: iml
    ```

## Create Scripts

Create a directory to host the codebase. Navigate into it and execute:
```
jeka base: scaffold
```
This generates a structure as:
```
.
├── jeka-src             <- Source root directory
│   └── Script.java     
└── jeka.properties      <- Configuration (Java and jeka version, default parameters...)
```
Ths script class looks like:
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
You can run the method `hello()` and changing the parameter, by executing:
```
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

    ...

    public void header() throws Exception {
        System.out.println(Strings.repeat("-", 80));
        System.out.println(FigletFont.convertOneLine("Hello Ascii Art !"));
        System.out.println(Strings.repeat("-", 80));
    }
}
```
Execute:
```
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
Note that:

- You can define multiple script methods in `Script.java`. These methods must be public, non-static, have no arguments, and return void.
- `Script.java` can be renamed to any name and placed in any package you prefer.
- There can be multiple script classes. When invoking a specific script, you can specify the class name, for example: `jeka script2: hi`.
- You can also [use the classes bundled with JeKa](reference/api-intro.md) without requiring explicit declarations.

#### KBeans

Every script should inherit from the `KBean` class. 

KBeans can either be provided as source code (located in the *jeka-src* directory) or as compiled classes available in the classpath.

Jeka includes several standard KBeans, which you can list by running:
```
jeka --doc
```
#### Change Java Version
To change version of Java, edit *jeka.properties*:
```properties
jeka.java.version=23
```
This will automatically download Java 23 (if not already installed) on the next method run.

#### Run remotely

Run `hello`from another directory:
```
jeka -r /path/to/scrcipt/root-dir hello
```

Run `hello`from remote git repo:
```
jeka -r https://my.githost/my-repo.git hello
```

#### Common Options/Commands:
```
jeka --help          <- Displays help message
--doc                <- Displays documentation on availbale scripts
--inspect            <- Displays details about Jeka setup
jeka base: depTree   <- Show dependency tree

```

#### Resources
- [Basics Tutorial](tutorials/basics.md)
- [Write Scripts in Java Video](https://youtu.be/r-PUX6amBrw)

## Create a Base App or Library

Jeka provides a *base* mode, which simplifies the creation of pure Java applications or libraries by avoiding the complexity of a traditional project structure.  

Despite its simplicity, this structure supports full build configuration, automated testing, native compilation, Maven publication, and Docker image creation.

To create a new code structure, run the following command:
```
jeka base: scaffold scaffold.kind=APP
```
This creates a structure like this:
```
. 
├── jeka-src             <- Source root directory
│   ├── _dev             <- Optional package containing all non-prod (build and test)
│   │   ├── test
│   │   └── Build.java  
│   └── app              <- Sugested base package for production code/resources
│       └── App.java     
├── jeka-output          <- Generated dir where artifacts as jars, classes, reports or doc are generated
├── jeka.properties      <- Build configuration  (Java and jeka version, kben configurations, ...)
└── README.md            <- Describes available build commands
```

Follow the [tutorial](tutorials/build-base.md) for more details.

## Create a Java Project

In this mode, you can create a fully-fledged project similar to *Maven* or *Gradle*.

To create a new project structure, execute:
```
jeka project: scaffold
```
This generates a project structure as:
```
.
├── src                  
│   ├── main             <- Java code and reources
│   │   ├── java
│   │   └── resources    
│   └── test             <- Java code and reources for tests
│       ├── java
│       └── resources 
├── jeka-src             <- Optional Java (or Kotlin) code for building the project
│   └── Build.java      
├── jeka-output          <- Generated dir where artifacts as jars, classes, reports or doc are generated
├── dependencies.txt     <- Dependency lists for compile, runtime and testing
├── jeka.properties      <- Build configuration  (Java and jeka version, kben configurations, ...)
├── jeka.ps              <- Optional Powershell script to boot Jeka on Windows
├── jeka                 <- Optional bash script to boot Jeka on Linuw/MacOS
└── README.md            <- Describes available build commands for building the project
```

Follow the [tutorial](tutorials/build-projects.md) for more details.

## Create a Spring-Boot Project

To create a new project Spring-Boot, execute:
```
jeka -cp=dev.jeka:springboot-plugin project: scaffold springboot:
```

This generates the following project structure:
```
.
├── src                  
│   ├── main             
│   │   ├── java
│   │   │   └── app
│   │   │       ├── Application.java.     <- Spring-Boot app class
│   │   │       └── Controller.java.      <- REST controller
│   │   └── resources    
│   └── test             
│       ├── java
│       │   └── app
│       │       └── ControllerIt.java     <- Integration Test for REST controller 
│       └── resources 
├── jeka-src             
│   └── Build.java       <- Empty build class -in case of.
├── jeka-output         
├── dependencies.txt     <- Springboot and extra dependencies
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
├── src       <- Contains both Java code and resooources    
├── test      <- Contains both Java code and resooources for testing
```

#### Modify Dependencies
The dependencies are generated with the latest Spring-Boot version:
```ada title="dependencies.txt"
== COMPILE ==
org.springframework.boot:spring-boot-dependencies::pom:3.4.1
org.springframework.boot:spring-boot-starter-web

== RUNTIME ==

== TEST ==
org.springframework.boot:spring-boot-starter-test
```
You can start from here for modifying, adding code, tests and dependencies.

#### Execute Commands

These are the most useful commands for developping Spring-Boot applications.

``` title="Common Commands"
jeka project: pack       <- Compiles, tests and creates Bootable Jar
jeka project: runJar     <- Run bootable jar
jeka project: depTree    <- Displays dependency tree

jeka docker: build       <- Creates Docker image containing the Spring-Boot application
jeka docker: buildNative <- Creates Docker image containing the Spring-Boot application compiled to native.
```

#### Customize Docker File

To reduce a Docker native image size, use a distroless base image. The native executable must be statically linked as libc is unavailable in such distributions. Configure it as follows:
```properties title="jeka.properties"
@native.staticLink=MUSL
@docker.nativeBaseImage=gcr.io/distroless/static-debian12:nonroot
```

#### What Next?

Now that you're here, you can explore the following resources to enhance your project.
Learn how to include SonarQube analysis, add a ReactJS web client, perform end-to-end testing, or implement a delivery pipeline in Java:

- [Example using SpringBoot + Angular + Sonarqube + Jacoco + Docker + end-to-end testing](https://github.com/jeka-dev/demo-project-springboot-angular)
- [Exemple using Kotlin + StringBoot + ReactJS + Sonarqube/Jacoco](https://github.com/jeka-dev/working-examples/tree/master/springboot-kotlin-reactjs)
- [Video on Jeka + Springboot + Docker + GraalVM](https://youtu.be/yfmaAwAjJ2w)
- [Documentation](https://jeka-dev.github.io/jeka/)

