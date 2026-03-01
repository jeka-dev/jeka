# Build Base-Mode

The `base` mode is a "best of both worlds" approach, sitting between single-file scripts like *JBang* and full-featured *Maven* or *Gradle* [projects](build-projects.md). 

In this tutorial, we'll use the [`base` KBean](../reference/kbeans-base.md) to build a Java application or library with minimal configuration.

Visit the [demo-base-application](https://github.com/jeka-dev/demo-base-application) repository to see a concrete example.

**Prerequisite:** JeKa must be [installed](../installation.md).

!!! tip
    Run `jeka base: --doc` to see all available options and commands for the [base KBean](../reference/kbeans-base.md).

## Scaffold a New Code Base

To create a base structure ready for coding, run:

```bash
jeka base: scaffold.kind=APP base: scaffold
```

This generates the following project structure:
```
. 
├── jeka-src             <- Source root directory
│   ├── _dev             <- Optional package containing all non-prod (build and test)
│   │   ├── test
│   │   └── Custom.java  
│   └── app              <- Suggested base package for production code/resources
│       └── App.java     
├── jeka-output          <- Generated dir where artifacts such as jars, classes, reports or doc are generated
├── jeka.properties      <- Build configuration (Java and JeKa version, KBean configurations, ...)
└── README.md            <- Describes available build commands
```

All your Java code is supposed to be in the `jeka-src` folder.

`_dev` is a special package for source code and dependencies used only for development (e.g., tests, builds).
If you're new to Java, you can ignore or delete it.

The scaffolded example includes an `App` class in the `app` package.  
You can add or modify classes in any package you like.

## Sync with IntelliJ

The easiest way to work with JeKa in IntelliJ is by using the [JeKa IntelliJ Plugin](https://plugins.jetbrains.com/plugin/13364-jeka). It provides syntax highlighting, auto-completion, and direct execution of JeKa commands.

To synchronize your project with IntelliJ and generate the necessary project files, run:

```bash
jeka intellij: sync
```

If changes don't appear or you need to re-initialize the project structure, use:

```bash
jeka intellij: initProject
```

## Add Dependencies

In `base` mode, dependencies are declared directly in your Java source files using the `@JkDep` annotation. This keeps your build configuration close to the code that uses it.

The scaffolded `App.java` includes examples:

```java title="jeka-src/app/App.java"
@JkDep("com.github.lalyos:jfiglet:0.0.9")
@JkDep("com.fasterxml.jackson.core:jackson-core:2.18.2")
public class App {
    // ...
}
```

For more details on how to specify dependencies, see the [Dependency Management](../reference/api-dependency-management.md#string-notation) guide.

### File-based Dependencies

You can also include JAR files as dependencies by placing them in the `jeka-boot` directory. They will be automatically added to the production classpath:

```text
├── jeka-boot      <- JARs included in the production classpath.
```

### Non-production Dependencies

Dependencies used only for development (like testing or custom build logic) should be declared in classes under the `_dev` package. This ensures they are not included in the final application bundle.

```java title="jeka-src/_dev/Custom.java"
@JkDep("org.junit.jupiter:junit-jupiter:5.11.4")
@JkDep("org.mockito:mockito-junit-jupiter:5.15.2")
class Custom extends KBean {
    // ...
}
```

!!! note
    Remember to run `jeka intellij: sync` after modifying dependencies to update your IDE's classpath.

## Run your Application

You can execute your application directly from the command line:

```bash
jeka --program arg0 args1 ... # or `jeka -p` for short
```

Use the `--clean` (`-c`) option to force a clean compilation before running:

```bash
jeka -c -p
```

### Run from Remote Git

JeKa can run applications directly from a remote Git repository without having to clone it manually:

```bash
jeka --remote https://github.com/jeka-dev/demo-base-application.git -p
```

## Native Compilation

JeKa makes it easy to compile your application into a standalone native executable using GraalVM.

To compile to native, execute:

```bash
jeka native: compile
```

Once compiled, running `jeka --program ...` will automatically use the native executable instead of the JVM.

!!! note
    If your application requires resources (like icons or config files) at runtime, you might need to set:
    ```properties title="jeka.properties"
    @native.includeAllResources=true
    ```

To always trigger a native build when running the application, add this to your `jeka.properties`:

```properties title="jeka.properties"
jeka.program.build=native: compile
```

## Dockerization

You can package your application into a Docker image with a single command:

```bash
jeka docker: build
```

This builds the image and registers it with your local Docker daemon. The console output will provide the exact command to run your new image.

To create an even smaller, faster-starting image using your native executable, run:

```bash
jeka docker: buildNative
```

## Pre-defined Build Commands

The [`base` KBean](../reference/kbeans-base.md) provides several built-in commands for common tasks:

```text title="Available Base Commands"
jeka base: test       # Run all tests in the _dev package
jeka base: pack       # Run tests and create application JARs
jeka base: runJar     # Execute the generated JAR
jeka base: info       # Display project configuration and dependencies
jeka base: depTree    # Display the full dependency tree 
```

For more advanced commands, see the [Project Build tutorial](build-projects.md#pre-defined-build-commands).

## Create a Library

While `base` mode is often used for applications, it can also be used to build and publish libraries. To do so, you must define a `moduleId` and a `version`.

Add these to your `jeka.properties`:

```properties title="jeka.properties"
@base.moduleId=org.example:my-lib
@base.version=1.0.0-SNAPSHOT
```

You can then publish your library to a Maven repository:

```bash
jeka maven: publish
```