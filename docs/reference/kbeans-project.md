# Project KBean - The Java Build Engine

<!-- header-autogen-doc -->

[`ProjectKBean`](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/project/ProjectKBean.java) 
is the core KBean for building JVM projects in JeKa. It acts as the equivalent of the **Java Plugin** in Maven or Gradle, 
wrapping a [`JkProject`](api-project.md) instance to provide high-level build automation.

## Key Features

- **Standardized Build Lifecycle**: Methods for `compile`, `test`, `pack`, and more out-of-the-box.
- **Dependency Management**: Automated resolution for compile, runtime, and test classpaths.
- **Artifact Creation**: Generates regular, fat (uber), shaded, source, and Javadoc JARs.
- **Project Introspection**: Displays detailed dependency trees and project configurations.
- **Git Integration**: Automatically infers project versions from Git tags/metadata.
- **Extensibility**: Foundation for other KBeans to extend the build process (e.g., Docker, Jacoco, Sonarqube).

## Core Methods

`ProjectKBean` exposes several methods to automate your build process from the command line:

| Method | Description |
| :--- | :--- |
| `scaffold` | Creates a new project structure and build script skeleton. |
| `clean` | Deletes the `jeka-output` directory. |
| `compile` | Compiles source code and processes resources. |
| `test` | Compiles and executes unit tests (e.g., JUnit 5). |
| `pack` | Creates the project's JAR artifacts. |
| `build` | Complete cycle: `clean` -> `compile` -> `test` -> `pack` -> `checkQuality` -> `e2eTest`. |
| `info` | Displays project metadata (version, module ID, layout, etc.). |
| `depTree` | Prints the resolved dependency tree to the console. |
| `runJar` | Executes the generated JAR file. |

## Common Options

These properties can be set via command line (e.g., `project: javaVersion=17`) or in `jeka.properties` (e.g., `@project.javaVersion=17`).

| Property | Description                                | Default |
| :--- |:-------------------------------------------| :--- |
| `javaVersion` | Target JVM version (e.g., `21`, `25`).     | Same as JeKa runtime |
| `test.skip` | If `true`, tests are not executed.         | `false` |
| `pack.jarType` | Type of JAR to produce (`REGULAR`, `FAT`). | `REGULAR` |
| `version` | Hardcode the project version.              | Inferred from Git |
| `moduleId` | Maven coordinates (`group:name`).          | `null` |

## Programmatic Customization

Access the underlying `JkProject` for deep customization, typically within `@JkPostInit`.

```java
class MyBuild extends KBean {

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        
        // Example: Add a custom compiler option
        project.compilation.addJavaCompilerOptions("-Xlint:unchecked");
        
        // Example: Customize the JAR manifest
        project.pack.manifest.addMainAttribute("Author", "JeKa Team");
    }
}
```

## Annotation Processors

Enable processors like **Lombok** by adding them as `compile-only` dependencies in `dependencies.txt`:

```ini
[compile-only]
org.mapstruct:mapstruct-processor:1.6.3
org.projectlombok:lombok:1.18.38
```

Output directory: `jeka-output/generated-sources/annotation-processors`.

## Extending with Plugins

`ProjectKBean` is designed to be extensible. Many JeKa plugins interact with it to add specific capabilities:

- **[Jacoco](https://github.com/jeka-dev/jeka/blob/master/plugins/dev.jeka.plugins.jacoco/src/dev/jeka/plugins/jacoco/JacocoKBean.java)**: Collects code coverage during the `test` phase.
- **[Sonarqube](https://github.com/jeka-dev/jeka/blob/master/plugins/dev.jeka.plugins.sonarqube/src/dev/jeka/plugins/sonarqube/SonarqubeKBean.java)**: Exports project data for static analysis.
- **[Protobuf](https://github.com/jeka-dev/jeka/blob/master/plugins/dev.jeka.plugins.protobuf/src/dev/jeka/plugins/protobuf/ProtobufKBean.java)**: Integrates Protocol Buffers compilation.
- **[Docker](kbeans-docker.md)**: Packages your project as a Docker image.

## Summary

<!-- body-autogen-doc -->