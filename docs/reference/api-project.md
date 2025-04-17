# Project API

The **Jeka Project API** provides a high-level interface for building Java/JVM projects. At the core of this API is the `JkProject` class, which serves as the central entry point for performing build tasks.

## Classes

The classes are located in the `dev.jeka.core.api.project` [package](https://github.com/jerkar/jeka/tree/master/dev.jeka.core/src/main/java/dev/jeka/core/api/project).

### **`JkProject`**

The `JkProject` class contains all the essential definitions for building a JVM project, including source locations, build output, testing, packaging (e.g., creating JAR files), compiler settings, dependencies, and more.

Designed with multiple extension points, it provides flexibility for integrating custom or specific build behaviors.

To manage all of these concerns efficiently, the class is structured as follows:

```
project
├─ baseDir
├─ outputDir
├─ artifactLocator (define where artifact files are supposed to be created)
├─ duplicateDependencyConflictStrategy
├─ jvmTargetVersion
├─ sourceEncoding
├─ javaCompileToolChain
├─ dependencyResolver
├─ compilation  (produce individual binary files from production sources. This includes resource processing, code generation, processing on .class files, ...)
│  ├─ layout (where are located source and resource files)
│  ├─ source generators (plugin mechanism for generating source files)
│  ├─ dependencies   (stands for compile dependencies)
│  ├─ preCompileActions (including resources processing)
│  ├─ compileActions (including java sources compilation. Compilation for other languages can be added here)
│  ├─ postCompileActions
│  └─ methods : resolveDependencies(), run()
├─ testing
│  ├─ testCompilation (same as above 'prodcCompilation' but for test sources)
│  │  ├─ layout
│  │  ├─ dependencies (stands for test dependencies)
│  │  └─ ...
│  ├─ breakOnFailure (true/false)
│  ├─ skipped (true/false)
│  ├─ testProcessor
│  │  ├─ forkedProcess (configured the forked process who will run tests)
│  │  ├─ preActions
│  │  ├─ postActions
│  │  ├─ engineBehavior
│  │  │  ├─ testReportDir
│  │  │  ├─ progressDisplayer
│  │  │  └─ launcherConfiguration (based on junit5 platform API)
│  │  └─ testSelection
│  │     ├─ includePatterns
│  │     └─ includeTags
│  └─ method : run()
├─ packaging (produces javadoc and source jar and bin jars)
│  ├─ javadocConfiguration
│  ├─ runtimeDependencies
│  ├─ manifest
│  ├─ fatJar (customize produced fat/uber jar if any)
│  └─ methods : createJavadocJar(), createSourceJar(), createBinJar(), createFatJar(), resolveRuntimeDependencies()
└─ methods :  toDependency(transitivity), getIdeSupport(), pack(), getDependenciesAsXml(), includeLocalAndTextDependencies()           
```
See a detailed example [here](https://github.com/jeka-dev/jeka/blob/master/samples/dev.jeka.samples.project-api/jeka-src/JkProjectBuild.java).

#### Flat Facade

For convenience, `JkProject` offers a simplified facade to easily configure common settings without delving into its deeper structure.

```Java
JkProjectFlatFacade projectFacade = JkProject.of().flatFacade;
projectFacade
       .setPublishedModuleId("dev.jeka:sample-javaplugin")
       .setVersionFromGitTag()
       .mixResourcesAndSources()
       .setLayoutStyle(SIMPLE)
       .addTestExcludeFilterSuffixedBy("IT", false);
projectFacade.dependencies.compile
       .add("com.google.guava:guava:21.0")
       .add("com.sun.jersey:jersey-server:1.19.4")
       .add("org.projectlombok:lombok:1.18.36");
projectFacade.dependencies.runtime
       .remove("org.projectlombok:lombok")
       .add("com.github.djeang:vincer-dom:1.2.0");
projectFacade.dependencies.test
       .add("org.junit.vintage:junit-vintage-engine:5.6.0");
projectFacade.doPack();  // compile, test and create jar
```

#### Project Dependencies

Project dependencies in Jeka are managed differently from Maven/Gradle. 
Instead of a single collection of dependencies for a specific scope/configuration, 
Jeka uses three distinct classpaths: **compile**, **runtime**, and **test**. Each is defined independently but can reference the others.

- **Compile classpath:** 

Classpath needed to compile the classes.

- **Runtime classpath:** 

Classpath needed to run the build application or to embedded with the built library (embedded in jar or specified as transitive dependencies).

This classpath is automatically constructed by taking the compile classpath upon which other libraries can be added or removed.

- **Test classpath:** 

Classpath needed to compile and run the tests. 

This classpath is constructed by merging the compile and runtime classpaths upon which other libraries can be added or removed.

Dependencies must follow the format: `group:module:[classifier]:[type]:[version]`
where *classifier*, *type*, and *version* are optional. See [`JkCoordinate` javadoc](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/api/depmanagement/JkCoordinate.java) for details.

To import a *bill-of-materials* (BOM), declare a dependency as: `group:module::pom:version`.

Using the programmatic api, you can also declare filesystem dependencies, meaning jar files located in 
the project code base.

<a href="#dependencies_txt"></a>
#### dependencies.txt

Entire project dependencies can be declared in full text located in the *[PROJECT_DIR]/dependencies.txt* file.


!!! example

    ```
    == COMPILE ==
    org.lwjgl:lwjgl-bom::pom:3.3.1   # Use lwjgl BOM so we don't need to specify lwjgl versions afterward
    org.lwjgl:lwjgl:natives-linux::  # specify the 'natives-linux' classifier for lwjgl
    org.projectlombok:lombok:1.16.16  

    == RUNTIME ==
    org.postgresql:postgresql:42.5.0
    - org.projectlombok:lombok       # remove lombok from runtime dependencies 

    == TEST ==
    org.seleniumhq.selenium:selenium-chrome-driver:3.4.0
    org.fluentlenium:fluentlenium-junit:3.2.0
    @ org.apache.httpcomponents:httpclient  # exclude http-client from fluentlenium-junit transitive dependencies
    org.fluentlenium:fluentlenium-assertj:3.2.0
    @@ net.sourceforge.htmlunit:htmlunit    # exclude htmlunit from all transitive dependencies

    ```

As shown on the above example, we can use `@` and `@@` symbols to specify dependency exclusions.`

`== COMPILE ==`  
Defines dependencies for the *compile* classpath.

`== RUNTIME ==`  
Defines dependencies for the *runtime* classpath.

If any dependencies from the *COMPILE* section should not be included in the *runtime* classpath, 
they must be explicitly removed using the '-' symbol.

`== TEST ==`  
Defines dependencies for the *test* classpath.  
This will include dependencies from both the *COMPILE* and *RUNTIME* sections, along with those specified in the *TEST* section.

!!! tip  
    If you're using the Jeka plugin for IntelliJ, press `ctrl+<space>` for autocomplete suggestions.

**Resolve Dependencies Programmatically**

To resolve dependencies that make up the runtime classpath, you can use one of the following methods:

- `JkProject.packaging.resolveRuntimeDependencies()` to fetch the dependency resolution tree.
- `JkProject.packaging.resolveRuntimeDependenciesAsFiles()` to get the resolved classpath as a list of JAR files.

The second option may be faster as it caches the results of previous invocations.

**Display Dependency tree on the console**

The dependency tree and the resulting classpath can be displayed on the console using:
`JkProject.displayDependencyTree()` methods.

**Change the Maven repository**

By default, the dependencies are resolved using *Maven central* repository.

We can change it programatically, by using `JkProject.dependencyResolver.setRepos()` method.

#### Display Generic Info

We can display project info such as locations, source file count, version, moduleID, and more by displaying the
result of the `JkProject.getInfo()` method.

#### Build Project

Different phases of the build can be invoked using the following methods:

- `JkProject.compilation.generateSources()`: Generates sources if any source generators are registered.
- `JkProject.compilation.run()`: Runs the entire compilation process, including source generation and other registered compilation tasks.
- `JkProject.testing.run()`: Compiles tests and runs them. This also includes production code compilation if it hasn't been done yet.
- `JkProject.pack()`: Creates the main JAR and any additional JARs specifically configured. This includes running tests if they haven't been executed.

In the next section, we'll detail the classes involved in the entire build process.

### JkProjectCompilation

Handles the compilation tasks for a `JkProject`.  
This class is used for both production and test code compilation.  
It offers configuration methods for defining:

- The locations of source files and compiled classes.
- Dependencies required for compilation.
- The compiler and compilation options.
- Source code generators attached to the compilation task.
- Additional `pre` and `post` actions tied to the compilation phase.
- Interpolators for resource processing.

### JkProjectSourceGenerator

Implement this class to define a source generator.
Register the generator in a project using:
`JkProject.compilation.addSourceGenerator(JkProjectSourceGenerator sourceGenerator)`.

Once registered, sources will be automatically generated during compilation or when explicitly calling:
`JkProject.compilation.generateSources()`.






