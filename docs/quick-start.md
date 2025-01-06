# Quick Start

This section explains how to get started quickly using the command-line mode.

**Prerequisite:** Jeka must be [installed](installation.md).

!!! note
    Using the [IntelliJ Plugin](https://plugins.jetbrains.com/plugin/24505-jeka) is the fastest way to get started. 
    It provides a wizard for creating various types of projects, ranging from simple scripts to full-fledged Spring Boot applications.

This quick-start guide covers muliple use cases:
 
- [Create scripts in Java](#create-scripts) and execute from command line.
- [Create a Base Application or Library](#create-a-base-app-or-library).
- [Create a Java Project](#create-a-java-project)

  
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
        ystem.out.println(FigletFont.convertOneLine("Hello Ascii Art !"));
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

  - You can have multiple script methods in `Script.java`: they must only be public, non-static, return void, have zero arguments.
  - `Script.java` can be renamed in with any name and be located in any package you like.
  - There can have multiple script classes. When invoking, we may specify the class name as `jeka script2: hi'.

#### KBeans

Every script should inherit from the `KBean` class. 

KBeans can be present as source code (in *jeka-src* dir), or as compiled class present in classpath.
Jeka is bundled with several standard KBeans, that you can list by executing:
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
jeka projec: scaffold
```
This generates a structure as:
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

