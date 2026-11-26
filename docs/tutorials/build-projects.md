# Building Java Projects

In this tutorial, we'll use the `project` KBean to build a Java application or library. 
This KBean provides build methods and a project layout similar to those of *Maven* and *Gradle*.

If you prefer a lighter structure without the standard `src/main/java` layout, see the [Build Base](build-base.md) tutorial.

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
│   ├── main
│   │   └── java         <- Java code
│   └── test
│       └── java         <- Unit tests
├── jeka-src             <- Java (or Kotlin) code for build customization
│   └── Custom.java      
├── jeka.project.deps    <- Project dependencies
├── jeka.properties      <- Build configuration and properties
├── jeka.ps1             <- PowerShell wrapper (optional)
├── jeka                 <- Bash wrapper (optional)
└── README.md            <- Project overview and build instructions
```

JARs, classes and reports are generated in the `jeka-output` directory, created on the first build.

Put resources in `src/main/resources` and `src/test/resources`.

The generated *jeka.properties* sets the JeKa and Java versions, makes `project` the default KBean, and packages the application as a FAT JAR:
```properties title="jeka.properties"
jeka.version=0.11.68
jeka.java.version=25

jeka.kbean.default=project
@custom=on

@project.pack.jarType=FAT
@project.pack.detectMainClass=true
```

The `jeka` and `jeka.ps1` scripts allow building the project on machines where JeKa is not globally installed.  
You can delete `jeka-src/Custom.java` if you don't need programmatic customization. In this case, also delete the `@custom=on` line.

!!! tip
    In a directory already containing sources, use `jeka project: scaffold scaffold.kind=EMPTY`. 
    It generates the same files, without sample classes. 
    Existing files are kept. If *jeka.properties* or *jeka.project.deps* already exist, JeKa displays the lines to add.

### Simple Layout

If you prefer a flatter structure, scaffold the project with the `SIMPLE` style:

```bash
jeka project: scaffold layout.style=SIMPLE layout.mixSourcesAndResources=true
```

This results in a simpler layout:
```text
.
├── src       <- Java code and resources mixed
├── test      <- Test code and resources mixed
```

and adds these properties:
```properties title="jeka.properties"
@project.layout.style=SIMPLE
@project.layout.mixSourcesAndResources=true
```

To convert an existing project, add these properties and move the files: *src/main/java* and *src/main/resources* to *src*, 
*src/test/java* and *src/test/resources* to *test*. Otherwise, the whole *src* directory, tests included, is compiled as production code.

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
jeka intellij: sync
```

If the project is not yet recognized as a JeKa project, use:
```bash
jeka intellij: initProject
```

## Dependency Management

Dependencies are declared in `jeka.project.deps`. JeKa supports Maven coordinates and BOMs.

```ini title="jeka.project.deps"
[version]    # Define versions or import BOMs
org.junit:junit-bom:5.12.2@pom

[compile]    # Main dependencies
com.google.guava:guava:33.4.0-jre
com.google.code.gson:gson:2.13.1

[compile-only] # Only needed for compilation (e.g., Lombok)
org.projectlombok:lombok:1.18.38

[runtime]    # Only needed at runtime
org.postgresql:postgresql:42.7.4

[test]       # Only needed for testing
org.junit.jupiter:junit-jupiter
org.junit.platform:junit-platform-launcher

[processor]  # Annotation processors
org.projectlombok:lombok:1.18.38
```

| Section | Description |
| :--- | :--- |
| `[version]` | Versions and BOM imports (using `@pom`). |
| `[compile]` | Included in compile, runtime, and test classpaths. |
| `[compile-only]` | Included in compile and test, but NOT runtime. |
| `[runtime]` | Included in runtime and test, but NOT compile. |
| `[test]` | Included in the test classpath only. |
| `[processor]` | Annotation processors. Not included in any classpath. See [Annotation Processors](../reference/kbeans-project.md#annotation-processors). |

You can also reference local files: `libs/my-local-lib.jar`.

## Java Version

By default, the project targets the Java version running JeKa, set by `jeka.java.version` in *jeka.properties*. 
JeKa itself needs Java 21 or higher. To target another version, set:

```properties title="jeka.properties"
@project.javaVersion=21
```

!!! note
    By default, tests run in a forked process, on a JDK of the target version. This does not work yet for targets below 21: `project: test` fails 
    with `UnsupportedClassVersionError`. For such targets, run the tests in the JeKa process:
    ```properties title="jeka.properties"
    @project.javaVersion=17
    @project.test.fork=false
    ```
    Classes are then compiled for Java 17, and tests run on the JDK running JeKa.

## Run Tests

```bash
jeka project: test                                          # all tests
jeka project: test test.includePatterns='.*MainTest'        # a single test class
jeka project: test test.includePatterns='com.acme.Order.*'  # several test classes
jeka project: build -Djeka.test.skip=true                   # full build, without tests
```

`test.includePatterns` is a space-separated list of regular expressions, matching fully qualified class names. 
Quote it, as some shells expand `*`.

## JAR Packaging

The scaffolded *jeka.properties* sets a FAT JAR. Without this property, JeKa creates a regular JAR.

```properties title="jeka.properties"
@project.pack.jarType=FAT
```

| Type | Description |
| :--- | :--- |
| `REGULAR` (default) | Contains the project classes and resources only. |
| `FAT` | Includes all runtime dependencies. |
| `SHADE` | Same as `FAT`, with the dependency packages relocated. |

!!! note
    `SHADE` relocates classes and `META-INF/services` files, but not the class names written in other text files.
    For example, `logback.xml` must mention the relocated name: `my.app._shaded.ch.qos.logback.core.ConsoleAppender`,
    where `my.app` is the root package of your application. Prefer `FAT` for applications that don't need relocation.

    Frameworks that list classes in their own resource files, such as Micronaut (`META-INF/micronaut/...`),
    don't start from a shaded jar: use `FAT` for them.

### Multiple Artifacts

For libraries, you might want to produce both a regular JAR and an extra shaded JAR:

```properties title="jeka.properties"
# Main JAR without dependencies
@project.pack.jarType=REGULAR

# Produce an extra shaded jar suffixed with '-all'
@project.pack.shadeJarClassifier=all

# Ensure the extra artifact is published to Maven
@maven.pub.extraArtifacts=all
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

The project must be in a Git repository having at least one commit. Otherwise, commands fail with a bare Git error (code 128 or 129).

- If you are on a **Tag**: The version is the tag name (e.g., `1.2.3`).
- If you are on a **Branch**: The version is `[branch]-SNAPSHOT` (e.g., `master-SNAPSHOT`), with `/` replaced by `-` (`feat/x` gives `feat-x-SNAPSHOT`).
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
| `jeka project: build` | Full cycle: clean, compile, test, pack, quality checks, end-to-end tests. |
| `jeka project: runJar` | Executes the generated JAR. |
| `jeka project: info` | Displays project setup and properties. |
| `jeka project: depTree` | Prints the full dependency tree. |

### Extensions (Optional)
- `jeka native: compile`: Compiles to a GraalVM native executable.
- `jeka docker: build`: Creates a Docker image.
- `jeka project: pack maven: publishLocal`: Publishes to the JeKa local repository. See [Maven KBean](../reference/kbeans-maven.md#publish-locally).

## Quality & Coverage (Plugins)

JeKa plugins like **JaCoCo** and **SonarQube** can be added via `jeka.properties`.

```properties title="jeka.properties"
# Inject plugins into the build classpath
jeka.classpath=dev.jeka:sonarqube-plugin dev.jeka:jacoco-plugin

# Activate JaCoCo for the project
@jacoco=on

# Configure SonarQube
sonar.host.url=http://localhost:9000

# Create a shortcut command for full build + quality check
jeka.cmd.quality=project: test pack sonarqube: run
```

Run with: `jeka ::quality`.

## Programmatic Customization

For complex logic, use `jeka-src/Custom.java`. This allows you to hook into the build lifecycle using `@JkPostInit`.

```java title="jeka-src/Custom.java"
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.JkPostInit;

public class Custom extends KBean {

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        // Customize compiler options
        projectKBean.project.compilation.addJavaCompilerOptions("-Xlint:unchecked");
        
        // Add a runtime dependency
        projectKBean.project.pack.runtimeDependencies.add("org.postgresql:postgresql:42.7.4");
    }

    @JkDoc("Custom action that can be called from CLI")
    public void myAction() {
        System.out.println("Hello from Custom KBean!");
    }
}
```

Run your custom action with: `jeka custom: myAction`.

!!! note
    The `JkProject` object is the heart of the `project` KBean. It provides a rich API for every aspect of the build. [See API Reference](../reference/api-project.md).
 