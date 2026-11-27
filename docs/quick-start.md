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
├── jeka.properties      <- JeKa configuration (Java and JeKa version, default parameters...)
├── jeka.ps1             <- Optional PowerShell script to run JeKa on Windows
├── jeka                 <- Optional bash script to run JeKa on Linux/macOS
└── .gitignore           <- Ignores jeka-output and .jeka-work
```
JeKa also creates a `.jeka-work` directory for its internal caches: you can ignore it.

This script class looks like:
```java title="jeka-src/Script.java"
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

// Declare script dependencies using @JkDep("groupId:moduleId:version")
// or using JBang '//DEPS groupId:moduleId:version' comment style
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
### Run methods
You can run the method `hello()` and change the parameter, by executing:
```bash
jeka hello name="JeKa"
```
This displays the following text on the console:
```
Hello JeKa !
```

!!! note "First run needs network access"
    `jeka.properties` pins the versions to use: `jeka.version` for JeKa, `jeka.java.version` for the JDK.
    Whether you run the global `jeka` or the project's `./jeka` script, JeKa runs with the pinned version, so the first
    run may download that JeKa distribution, the JDK (about 200 MB) and the Ivy library used to resolve dependencies.
    They are cached in `~/.jeka/cache` for the next runs. See [Properties](reference/properties.md#standard-properties).

### Write Extra Methods
You can add extra methods relying or not on third-party dependencies. Add the `header()` method, the two `@JkDep`
annotations and the new imports to `jeka-src/Script.java`, keeping `hello()`. The whole file becomes:
```java title="jeka-src/Script.java"
import com.github.lalyos.jfiglet.FigletFont;
import com.google.common.base.Strings;
import dev.jeka.core.tool.JkDep;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

@JkDep("com.github.lalyos:jfiglet:0.0.9")
@JkDep("com.google.guava:guava:33.3.1-jre")
@JkDoc("Minimalist script for demo purpose.")
class Script extends KBean {

    @JkDoc("Person to whom the greeting is intended")
    public String name = "World";

    @JkDoc("Print greeting on console")
    public void hello() {
        String greetings = "Hello " + name + " !";
        System.out.println(greetings);
    }

    @JkDoc("Print an ASCII-art banner")
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
On the first run, JeKa shows `Resolving dependencies...` while it downloads the two libraries, then displays:
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

### KBeans

Every script should inherit from the `KBean` class. 

KBeans can either be provided as source code (located in the `jeka-src` directory) or as compiled classes available in the classpath.

JeKa includes several standard KBeans, which you can list by running:
```bash
jeka --doc
```
### Change Java Version
The scaffold pins a recent JDK in `jeka.properties` (e.g. `jeka.java.version=25`). To use another version, change the value:
```properties title="jeka.properties"
jeka.java.version=21
```
On the next run, JeKa uses that JDK, and downloads it first if it is not installed yet.
To check which JDK is in use, run `jeka --inspect` and look at `Java Version`.

### Source-Runnable Applications

Run `hello` from another directory:
```bash
jeka -r /path/to/script/root-dir hello
```

Run a KBean method from a remote Git repository:
```bash
jeka -r https://github.com/jeka-dev/demo-base-application base: info
```
JeKa clones the repository, then `base: info` shows its build info: module id, main class, classpath, manifest...

!!! note "Trusted repositories"
    Repositories under `github.com/jeka-dev/` are trusted by default. For any other URL, JeKa asks you to confirm
    that you trust it before cloning, and fails without a terminal (CI, scripts).
    See [Trust and Security](tutorials/source-runnable-apps.md#trust-and-security).

JeKa acts as an application source manager, allowing you to run or install applications directly from their source code.
For more details, see the [Source-Runnable Applications tutorial](tutorials/source-runnable-apps.md).

### Common Options & Commands

```bash
jeka --help          # Displays help message
jeka --doc           # Displays documentation on available KBeans
jeka --inspect       # Displays details about JeKa setup and properties
jeka base: depTree   # Show dependency tree
```

### Resources
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
│   │   │   └── MyTest.java
│   │   └── Custom.java  
│   └── app              <- Suggested base package for production code/resources
│       └── App.java     
├── jeka.properties      <- Build configuration (Java and JeKa version, KBean configurations, ...)
├── jeka.ps1             <- Optional PowerShell script to run JeKa on Windows
├── jeka                 <- Optional bash script to run JeKa on Linux/macOS
├── .gitignore           <- Ignores jeka-output and .jeka-work
└── README.md            <- Describes available build commands
```

Build and run it:
```bash
jeka base: test      # Compiles and runs the tests of jeka-src/_dev/test
jeka base: pack      # Creates a fat JAR in jeka-output/, named after the directory
jeka base: runJar    # Runs the JAR
```
`test` ends with `ALL TESTS PASSED`, and `runJar` prints a "Hello JeKa" ASCII-art banner, made by the JFiglet
library that `App.java` declares with `@JkDep`.

The generated `jeka.properties` sets `jeka.kbean.default=base`, so you can leave out `base:` in this project:
`jeka test pack runJar` does the same as the three commands above.
The generated `README.md` lists more commands, such as `jeka -p` to run the built JAR without checking whether the
sources changed, and `jeka docker: build` to create a Docker image.

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
│   ├── main
│   │   └── java         <- Java code
│   └── test
│       └── java         <- Java code for tests
├── jeka-src             <- Optional Java (or Kotlin) code for building the project
│   └── Custom.java      
├── jeka.project.deps    <- Dependency lists for compile, runtime and testing
├── jeka.properties      <- Build configuration (Java and JeKa version, KBean configurations, ...)
├── jeka.ps1             <- Optional PowerShell script to boot JeKa on Windows
├── jeka                 <- Optional bash script to boot JeKa on Linux/MacOS
└── README.md            <- Describes available build commands for building the project
```

Generated artifacts (JARs, reports) are stored in `jeka-output`, created on the first build.

Follow the [tutorial](tutorials/build-projects.md) for more details.

## Create a Spring Boot Project

To create a new Spring Boot project, execute:
```bash
jeka -cp=dev.jeka:springboot-plugin project: scaffold springboot:
```

Without version, JeKa uses the plugin version matching its own version.

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
│   │       └── application.properties
│   └── test             
│       └── java
│           └── app
│               └── ControllerIT.java     <- Integration Test for REST controller 
├── jeka-src             
│   └── Custom.java      <- Optional build class (if needed)
├── jeka.project.deps    <- Spring Boot and extra dependencies
├── jeka.properties      <- Build configuration 
├── jeka.ps1             
├── jeka                 
└── README.md            <- Describes available build commands for building the project
```
This contains a minimal workable project with production and test code. 

### Modify Layout
You can choose a simpler code layout structure when scaffolding:
```bash
jeka -cp=dev.jeka:springboot-plugin project: scaffold layout.style=SIMPLE layout.mixSourcesAndResources=true springboot:
```
You'll end up with the following code layout:
```
.
├── src       <- Contains both Java code and resources    
├── test      <- Contains both Java code and resources for testing
```
To convert an existing project, see [Simple Layout](tutorials/build-projects.md#simple-layout).

### Modify Dependencies
The dependencies are generated with the latest Spring Boot version:
```ini title="jeka.project.deps"
[version]
org.springframework.boot:spring-boot-dependencies:4.2.0@pom

[compile]
org.springframework.boot:spring-boot-starter-webmvc

[test]
org.springframework.boot:spring-boot-starter-webmvc-test
```
You can start from here for modifying, adding code, tests and dependencies.

### Execute Commands

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

### Customize Docker File

To reduce a Docker native image size, use a distroless base image. The native executable must be statically linked as libc is unavailable in such distributions. Configure it as follows:
```properties title="jeka.properties"
@native.staticLink=MUSL
@docker.nativeBaseImage=gcr.io/distroless/static-debian12:nonroot
```

### What Next?

Explore more advanced topics and real-world examples:

- **[Source-Runnable Applications](tutorials/source-runnable-apps.md)**: Run or install apps directly from Git.
- **[Spring Boot + Angular + SonarQube Example](https://github.com/jeka-dev/demo-project-springboot-angular)**: A complete full-stack example.
- **[Kotlin + Spring Boot + ReactJS Example](https://github.com/jeka-dev/working-examples/tree/master/springboot-kotlin-reactjs)**: Using Kotlin with modern web frameworks.
- **[JeKa + Spring Boot + GraalVM Video](https://youtu.be/yfmaAwAjJ2w)**: Watch JeKa in action.
- **[Basics Tutorial](tutorials/basics.md)**: Deep dive into JeKa core concepts.

