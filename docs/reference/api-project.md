# Project API

The **JeKa Project API** provides a high-level interface for building Java/JVM projects. `JkProject` is its entry point.

## Classes

The classes are located in the `dev.jeka.core.api.project` [package](https://github.com/jeka-dev/jeka/tree/master/core/src/main/java/dev/jeka/core/api/project).

### **`JkProject`**

`JkProject` contains all the definitions for building a JVM project: source locations, dependencies, compilation, testing, 
packaging, and more. It offers extension points to integrate specific build behaviors.

The main members are:

```
project
├─ baseDir, outputDir, jvmTargetVersion, sourceEncoding, moduleId, version, duplicateConflictStrategy (getters and setters)
├─ artifactLocator (defines where artifact files are created)
├─ compilerToolChain
├─ dependencyResolver
├─ compilation  (produces binary files from production sources)
│  ├─ layout (location of source and resource files)
│  ├─ dependencies   (compile dependencies)
│  ├─ procDependencies  (annotation-processor dependencies, passed as -processorpath)
│  ├─ resourceProcessor
│  ├─ preCompileActions (including resource processing)
│  ├─ compileActions (including Java compilation. Compilation for other languages can be added here)
│  ├─ postCompileActions
│  └─ methods : addSourceGenerator(), addJavaCompilerOptions(), generateSources(), resolveDependenciesAsFiles(), run()
├─ test
│  ├─ compilation (same as above, for test sources)
│  ├─ processor
│  │  ├─ preActions
│  │  ├─ postActions
│  │  ├─ engineBehavior (progress display, JUnit platform launcher configuration)
│  │  └─ methods : setForkingProcess()
│  ├─ selection (include/exclude patterns and tags)
│  ├─ postActions
│  └─ methods : setSkipped(), setBreakOnFailures(), run()
├─ pack (produces binary, fat, sources and javadoc JARs)
│  ├─ actions
│  ├─ runtimeDependencies
│  ├─ javadocProcessor
│  ├─ manifestCustomizer
│  └─ methods : createBinJar(), createFatJar(), createSourceJar(), createJavadocJar(), customizeFatJarContent(), 
│               resolveRuntimeDependencies(), run()
├─ qualityCheck
│  └─ methods : add(), run()
├─ e2eTest
│  └─ methods : add(), run()
├─ flatFacade
└─ methods : clean(), fullBuild(), toDependency(), asBuildable(), getInfo(), displayDependencyTree(), getDependenciesAsXml()
```
See a detailed example [here](https://github.com/jeka-dev/jeka/blob/master/samples/samples.project-api/jeka-src/JkProjectCustom.java).

!!! note
    The `testing` and `packaging` fields, and the `pack()` method, are deprecated. Use `test`, `pack` and `pack.run()`.

In a KBean, get the project from the `ProjectKBean`:

```java
class Custom extends KBean {

    @JkPostInit(required = true)
    private void postInit(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project.compilation.addJavaCompilerOptions("-Xlint:unchecked");
        project.test.selection.addExcludePatterns(JkTestSelection.IT_PATTERN);
    }
}
```

#### Flat Facade

`JkProject` offers a simplified facade to configure common settings without delving into its structure.

```java
JkProjectFlatFacade projectFacade = JkProject.of().flatFacade;
projectFacade
       .setModuleId("dev.jeka:sample-javaplugin")
       .setVersionFromGitTag()
       .setMixResourcesAndSources()
       .setLayoutStyle(JkCompileLayout.Style.SIMPLE)
       .addTestExcludeSuffixIf(true, "IT");
projectFacade.dependencies.compile
       .add("com.google.guava:guava:33.4.8-jre")
       .add("org.projectlombok:lombok:1.18.36");
projectFacade.dependencies.runtime
       .remove("org.projectlombok:lombok")
       .add("com.github.djeang:vincer-dom:1.2.0");
projectFacade.dependencies.test
       .add("org.junit.jupiter:junit-jupiter:5.12.2");
projectFacade.doPack();  // compile and create jar
```

#### Project Dependencies

JeKa uses three classpaths, each one derived from the previous ones:

- **Compile classpath:** Needed to compile the classes.

- **Runtime classpath:** Needed to run the application, or embedded in the JAR, or published as transitive dependencies. 
  It is constructed from the compile classpath, where other libraries can be added or removed.

- **Test classpath:** Needed to compile and run the tests. 
  It is constructed by merging the compile and runtime classpaths, where other libraries can be added or removed.

There is also a **processor path** for annotation processors, independent of these classpaths.

Dependencies can be declared programmatically, as above, or in a *jeka.project.deps* file.
See [String Notation](api-dependency-management.md#string-notation) for the accepted formats.

<a id="jeka.project.deps"></a>
#### jeka.project.deps

Project dependencies can be declared in the *[PROJECT_DIR]/jeka.project.deps* text file.

Example:
```ini
[version]
org.junit:junit-bom:5.12.1@pom    # use this BOM as a version provider for all JUnit dependencies
org.postgresql:postgresql:42.5.0
org.glassfish.jaxb:jaxb-*:4.0.5   # use a wildcard to specify the version of several modules

[compile]
com.google.guava:guava:33.4.8-jre
org.lwjgl:lwjgl:natives-linux:3.3.6  # specify the 'natives-linux' classifier for lwjgl
org.glassfish.jaxb:jaxb-runtime      # version for org.glassfish.jaxb:jaxb-* is specified above
../module-utils                      # specify a project directory located on the file system

[compile-only]
jakarta.servlet:jakarta.servlet-api:6.1.0
org.openjfx:javafx-base:${jeka.platform.os}:21.0.2  # properties are interpolated
com.github.tjake:jlama-native:${maven.classifier.os}-${maven.classifier.arch}:: # specify a classifier without specifying the version

[runtime]
org.postgresql:postgresql

[test]
org.junit.jupiter:junit-jupiter
org.junit.platform:junit-platform-launcher
org.fluentlenium:fluentlenium-junit:3.2.0
    !org.apache.httpcomponents:httpclient  # exclude httpclient from 'fluentlenium-junit' transitive dependencies
!!commons-logging:commons-logging          # exclude commons-logging from all dependencies of this section

[processor]
org.projectlombok:lombok:1.18.38
org.mapstruct:mapstruct-processor:1.6.3
```

A line starting with `!` excludes a module from the transitive dependencies of the dependency declared just above.
A line starting with `!!` excludes a module from the transitive dependencies of all the dependencies of the section.

`[version]`  
Specifies the versions of dependencies to use when not explicitly defined.  
You can include all versions defined in a *BOM* by referencing its coordinates and appending `@pom`.  
The section inherits definitions from the `[version]` section of the `jeka.project.deps` file located in the parent directory.

`[compile]`  
Specifies dependencies required for both compiling and running the project.

`[compile-only]`  
Specifies dependencies required only for compiling the project, which are excluded at runtime.

`[runtime]`  
Specifies dependencies required only for running the project.

`[test]`  
Specifies dependencies for the *test* classpath, in addition to those of the `[compile]`, `[compile-only]` and `[runtime]` sections.

`[processor]`  
Specifies annotation-processor dependencies (e.g. Lombok, MapStruct).
These are passed to the compiler via `-processorpath` and are **not** added to the compile or runtime classpaths.
Generated sources are written to `jeka-output/generated-sources/annotation-processors`.

!!! tip
    If you're using the JeKa plugin for IntelliJ, press `ctrl+<space>` for autocomplete suggestions.

**Resolve Dependencies Programmatically**

To resolve the dependencies of the runtime classpath:

```java
JkResolveResult result = project.pack.resolveRuntimeDependencies();     // includes the dependency tree
List<Path> classpath = project.pack.resolveRuntimeDependenciesAsFiles();  // JAR files only
```

**Display the Dependency Tree on the Console**

Use `JkProject.displayDependencyTree()`.

**Change the Maven Repository**

By default, dependencies are resolved using the *Maven Central* repository. 
You can change it using [properties](properties.md#repositories), or programmatically:

```java
project.dependencyResolver.setRepos(JkRepoSet.of("https://my.company/repo"));
```

#### Display Generic Info

`JkProject.getInfo()` returns a text with project info such as locations, source file count, version, and module ID.

#### Build Project

The phases of the build can be invoked using the following methods:

- `project.compilation.generateSources()`: Generates sources if any source generators are registered.
- `project.compilation.run()`: Runs the entire compilation process, including source generation and other registered compilation tasks.
- `project.test.run()`: Compiles and runs tests. Production code is compiled first, if not done yet.
- `project.pack.run()`: Creates the main JAR and any additional artifacts configured. Production code is compiled first, if not done yet. Tests are not run.
- `project.qualityCheck.run()`: Runs the registered quality checkers.
- `project.e2eTest.run()`: Runs the registered end-to-end tests.
- `project.fullBuild()`: Runs clean, test, pack and end-to-end tests.

In the next sections, we detail some classes involved in the build process.

### JkProjectCompilation

Handles the compilation tasks for a `JkProject`, for both production and test code.  
It offers configuration methods for defining:

- The locations of source files and compiled classes (`layout`).
- Dependencies required for compilation (`dependencies`).
- Annotation-processor dependencies passed as `-processorpath` (`procDependencies`).
- The compilation options.
- Source code generators attached to the compilation task.
- Additional `pre` and `post` actions tied to the compilation phase.
- Interpolators for resource processing (`resourceProcessor`).

### JkProjectSourceGenerator

Implement this interface to define a source generator.

```java
class MyGenerator implements JkProjectSourceGenerator {

    @Override
    public String getDirName() {
        return "my-generator";  // directory name, under the generated sources dir
    }

    @Override
    public void generate(JkProject project, Path generatedSourceDir) {
        JkPathFile.of(generatedSourceDir.resolve("org/example/Info.java"))
                .createIfNotExist()
                .write("package org.example; public class Info {}");
    }
}
```

Register it using `project.compilation.addSourceGenerator(new MyGenerator())`. 
Sources are then generated during compilation, or when calling `project.compilation.generateSources()`.
