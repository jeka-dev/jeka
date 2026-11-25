# Migrate a Maven Project

This tutorial converts an existing single-module Maven project to JeKa. Sources stay in place, as JeKa uses the same 
`src/main/java` and `src/test/java` layout by default. The *pom.xml* file is kept until you are satisfied with the result.

You need the [JeKa CLI](../installation.md), and Maven (`mvn` in the PATH, or a `mvnw` wrapper in the project).

## 1. Generate the JeKa Files

Commit your work first, then execute from the directory containing the *pom.xml* file:

```bash
jeka project: scaffold scaffold.kind=EMPTY
```

This adds *jeka.properties*, *jeka.project.deps*, and the `jeka` and `jeka.ps1` wrapper scripts. 
`kind=EMPTY` avoids generating the sample classes.

!!! warning
    This replaces the existing *README.md* file. Restore it with `git checkout README.md`.

## 2. Convert the Dependencies

```bash
jeka maven: migrateDeps
```

This displays the dependencies of the *pom.xml* as Java code, then as *jeka.project.deps* content. 
Copy the latter into *jeka.project.deps*, replacing its content:

```ini title="jeka.project.deps"
[compile]
com.google.guava:guava:33.4.8-jre

[compile-only]
- org.projectlombok:lombok

[runtime]
org.postgresql:postgresql:42.7.4

[test]
org.junit.jupiter:junit-jupiter:5.12.2
```

!!! warning "Fix `[compile-only]` by hand"
    For now, dependencies of `provided` scope are displayed with a leading `-` and without version, as *lombok* above. 
    Remove the dash and add the version. Otherwise, builds fail with `Coordinate specification '- org...' is not correct`.
    ```ini
    [compile-only]
    org.projectlombok:lombok:1.18.38
    ```

If *lombok* or another dependency is an annotation processor, also declare it in a `[processor]` section:

```ini
[processor]
org.projectlombok:lombok:1.18.38
```

Maven scopes translate as follows:

| Maven                            | jeka.project.deps                                                   |
|:---------------------------------|:--------------------------------------------------------------------|
| `compile`                        | `[compile]`                                                         |
| `provided`                       | `[compile-only]`                                                    |
| `runtime`                        | `[runtime]`                                                         |
| `test`                           | `[test]`                                                            |
| `<annotationProcessorPaths>`     | `[processor]`                                                       |
| `<dependencyManagement>`, `import` scope | `[version]`, with `@pom` suffix for BOMs                    |
| `<exclusions>`                   | `!group:name` lines, below the dependency                           |

See [jeka.project.deps](../reference/api-project.md#jeka.project.deps) for the complete syntax.

## 3. Set the Coordinates and the Java Version

Replace the content of *jeka.properties*, keeping the generated `jeka.version`:

```properties title="jeka.properties"
jeka.version=0.11.68
jeka.java.version=21
jeka.kbean.default=project

@project=on
@project.moduleId=com.acme:shop
@project.version=1.0.0
```

| Maven                                       | JeKa                                                      |
|:--------------------------------------------|:----------------------------------------------------------|
| `groupId`, `artifactId`                     | `@project.moduleId=com.acme:shop`                         |
| `version`                                   | `@project.version=1.0.0`, or `@project.gitVersioning.enable=true` |
| `maven.compiler.release`                    | `jeka.java.version` (21 or higher) or `@project.javaVersion` |
| `<compilerArgs>`                            | `@project.compilation.compilerOptions=-Xlint:unchecked`   |
| `<mainClass>` of the JAR or Shade plugin    | `@project.pack.mainClass`, or `@project.pack.detectMainClass=true` |
| Shade or Assembly plugin                    | `@project.pack.jarType=FAT` (or `SHADE` to relocate packages) |
| Repositories of *settings.xml*              | [`jeka.repos.download`](../reference/properties.md#repositories) |
| Other plugins                               | [KBeans](../reference/kbeans.md), in `jeka-src` or from a plugin JAR |

For a target lower than Java 21, see [Java Version](build-projects.md#java-version).

## 4. Build and Compare

```bash
jeka project: test pack --clean
```

Then check that the result matches the Maven build:

```bash
jeka project: depTree     # compare with 'mvn dependency:tree'
jeka project: info        # versions, source locations, main class...
jeka maven: info          # what will be published
```

In IntelliJ, execute `jeka intellij: sync` to use the JeKa dependencies. 
When done, delete the *pom.xml* file and the Maven wrapper.

## Command Equivalents

See [Common Commands](../comparison.md#common-commands).

## Multi-Module Projects

Repeat the steps above in each module, then:

- Declare the modules in the *jeka.properties* of the parent directory: `_jeka.child-bases=core, plugins/*`.
- Replace dependencies between modules by relative paths in *jeka.project.deps*, as `../core`.
- Move the content of `<dependencyManagement>` to the `[version]` section of the *jeka.project.deps* of the parent directory.

See [Multi-Module](../reference/multi-modules.md).

!!! note
    For now, dependencies on sibling modules are missing from the published POM.

## Keep Building with Maven

JeKa can also run an existing Maven build, for instance to make the application runnable from Git. 
See [Run Maven](../reference/kbeans-maven.md#run-maven).
