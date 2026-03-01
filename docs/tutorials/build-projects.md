# Building Java Projects

In this tutorial, we'll use the `project` KBean to build a Java application or library. 
This KBean provides build methods and a project layout similar to those of *Maven* and *Gradle*.

If you prefer a lighter structure without the standard `src/main/java` layout, see the [Build Base](build-base.md) tutorial.

**Prerequisite:** JeKa must be [installed](../installation.md).

!!! tip
    Run `jeka project: --doc` to see all available options and methods.

## Scaffold a New Project

Run the following command to create a standard project structure, ready for coding:

```bash
jeka project: scaffold
```

You'll get the following project structure:
```text
.
├── src                  
│   ├── main             <- Java code and resources
│   │   ├── java
│   │   └── resources    
│   └── test             <- Unit tests and test resources
│       ├── java
│       └── resources 
├── jeka-src             <- Java (or Kotlin) code for build customization
│   └── Custom.java      
├── jeka-output          <- Generated artifacts (JARs, classes, reports)
├── dependencies.txt     <- Project dependencies
├── jeka.properties      <- Build configuration and properties
├── jeka.ps              <- PowerShell wrapper (optional)
├── jeka                 <- Bash wrapper (optional)
└── README.md            <- Project overview and build instructions
```

The `jeka` and `jeka.ps` scripts allow building the project on machines where JeKa is not globally installed.  
You can delete `jeka-src/Custom.java` if you don't need programmatic customization.

### Simple Layout

If you prefer a flatter structure, you can enable the `SIMPLE` style:

```properties title="jeka.properties"
@project.layout.style=SIMPLE
@project.layout.mixSourcesAndResources=true
```

This results in a simpler layout:
```text
.
├── src       <- Java code and resources mixed
├── test      <- Test code and resources mixed
```

### Local JAR Dependencies

You can also include local JARs by placing them in the `libs` directory:

```text
.
├── libs                  
│   ├── compile          <- JARs for compile, runtime, and test classpaths
│   ├── compile-only     <- JARs for compile and test classpaths only
│   ├── runtime          <- JARs for runtime and test classpaths
│   └── test             <- JARs for test classpath only
```

## IDE Integration

To synchronize your project with **IntelliJ IDEA**, run:

```bash
jeka intellij: iml --force
```

If the project is not yet recognized as a JeKa project, use:
```bash
jeka intellij: initProject
```

## Dependency Management

Dependencies are declared in `dependencies.txt`. JeKa supports Maven coordinates and BOMs.

```ini title="dependencies.txt"
[version]    # Define versions or import BOMs
org.junit:junit-bom:5.12.2@pom

[compile]    # Main dependencies
com.google.guava:guava:33.4.0-jre
com.google.code.gson:gson:2.13.1

[compile-only] # Only needed for compilation (e.g., Lombok)
org.projectlombok:lombok:1.18.32

[runtime]    # Only needed at runtime
org.postgresql:postgresql:42.7.4

[test]       # Only needed for testing
org.junit.jupiter:junit-jupiter
org.junit.platform:junit-platform-launcher
```

| Section | Description |
| :--- | :--- |
| `[version]` | Versions and BOM imports (using `@pom`). |
| `[compile]` | Included in compile, runtime, and test classpaths. |
| `[compile-only]` | Included in compile and test, but NOT runtime. |
| `[runtime]` | Included in runtime and test, but NOT compile. |
| `[test]` | Included in the test classpath only. |

You can also reference local files: `libs/my-local-lib.jar`.

## Java Version

By default, the project uses the same Java version as the JeKa runtime (see `jeka.java.version` in `~/.jeka/jeka.properties`). 
To target a specific version, set:

```properties title="jeka.properties"
@project.javaVersion=17
```

## JAR Packaging

JeKa creates a **Regular JAR** by default. You can change this to a **Fat JAR** (includes all dependencies) or a **Shaded JAR** (fat JAR with relocated packages).

```properties title="jeka.properties"
@project.pack.jarType=FAT
```

### Multiple Artifacts

For libraries, you might want to produce both a regular JAR and an extra shaded JAR:

```properties title="jeka.properties"
# Produce an extra shaded jar suffixed with '-all'
@project.pack.shadeJarClassifier=all

# Ensure the extra artifact is published to Maven
@maven.publication.extraArtifacts=all
```

## Versioning and Git

You can hardcode the version in `jeka.properties`:

```properties title="jeka.properties"
@project.moduleId=com.example:my-app
@project.version=1.0.0-SNAPSHOT
```

### Automatic Git Versioning

The recommended approach is to let JeKa infer the version from Git:

```properties title="jeka.properties"
@project.gitVersioning.enable=true
```

- If you are on a **Tag**: The version is the tag name (e.g., `1.2.3`).
- If you are on a **Branch**: The version is `[branch]-SNAPSHOT` (e.g., `master-SNAPSHOT`).
- **Metadata**: The JAR manifest will automatically include Git commit ID, branch, and "dirty" state.

## Essential Build Commands

### Common Flags
- `-c` or `--clean`: Deletes `jeka-output` before running.
- `-v` or `--verbose`: Shows detailed logs.
- `-p` or `--program`: Runs the generated application.

### Project Lifecycle
| Command | Description |
| :--- | :--- |
| `jeka project: compile` | Compiles source code. |
| `jeka project: test` | Runs unit tests (e.g., JUnit). |
| `jeka project: pack` | Packages the project into a JAR. |
| `jeka project: build` | Full cycle: clean, compile, test, pack, quality checks. |
| `jeka project: runJar` | Executes the generated JAR. |
| `jeka project: info` | Displays project setup and properties. |
| `jeka project: depTree` | Prints the full dependency tree. |

### Extensions (Optional)
- `jeka native: compile`: Compiles to a GraalVM native executable.
- `jeka docker: build`: Creates a Docker image.
- `jeka maven: publishLocal`: Publishes to the local Maven repository.

## Quality & Coverage (Plugins)

JeKa plugins like **JaCoCo** and **SonarQube** can be added via `jeka.properties`.

```properties title="jeka.properties"
# Inject plugins into the build classpath
jeka.inject.classpath=dev.jeka:sonarqube-plugin dev.jeka:jacoco-plugin

# Activate JaCoCo for the project
@jacoco=

# Configure SonarQube
sonar.host.url=http://localhost:9000

# Create a shortcut command for full build + quality check
jeka.cmd.quality=project: pack sonarqube: run
```

Run with: `jeka quality`.

## Programmatic Customization

For complex logic, use `jeka-src/Custom.java`. This allows you to hook into the build lifecycle using `@JkPostInit`.

```java title="jeka-src/Custom.java"
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.JkPostInit;

public class Custom extends KBean {

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        // Customize compiler options
        projectKBean.project.compilation.addJavaCompilerOptions("-Xlint:unchecked");
        
        // Add a custom resource to the JAR
        projectKBean.project.pack.runtimeDependencies.add("org.postgresql:postgresql:42.7.4");
    }

    @JkDoc("Custom action that can be called from CLI")
    public void myAction() {
        System.out.println("Hello from Custom KBean!");
    }
}
```

Run your custom action with: `jeka myAction`.

!!! note
    The `JkProject` object is the heart of the `project` KBean. It provides a rich API for every aspect of the build. [See API Reference](../reference/api-project.md).
 