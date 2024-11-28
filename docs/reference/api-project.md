# Project API

The Jeka Project API provides a high-level interface for building Java/JVM projects. Its core classes are located in the `dev.jeka.core.api.project` [package](https://github.com/jerkar/jeka/tree/master/dev.jeka.core/src/main/java/dev/jeka/core/api/project).

At the heart of this API is the `JkProject` class, which acts as the central entry point for performing key build tasks, including compilation, testing, resource processing, packaging, publication, and more.  
`JkProject` serves as the root of a comprehensive object-oriented structure, encapsulating all the information and behavior needed to build JVM-based projects.

The API is designed with numerous extension points, making it highly flexible for incorporating specific or custom build behaviors.


## Project Structure 
```
project
+- baseDir
+- outputDir
+- artifactLocator (define where artifact files are supposed to be created)
+- duplicateDependencyConflictStrategy
+- jvmTargetVersion
+- sourceEncoding
+- javaCompileToolChain
+- dependencyResolver
+- compilation  (produce individual binary files from production sources. This includes resource processing, code generation, processing on .class files, ...)
|  +- layout (where are located source and resource files)
|  +- source generators (plugin mechanism for generating source files)
|  +- dependencies   (stands for compile dependencies)
|  +- preCompileActions (including resources processing)
|  +- compileActions (including java sources compilation. Compilation for other languages can be added here)
|  +- postCompileActions
|  +- methods : resolveDependencies(), run()
+- testing
|  +- testCompilation (same as above 'prodcCompilation' but for test sources)
|  |  +- layout
|  |  +- dependencies (stands for test dependencies)
|  |  + ...
|  +- breakOnFailure (true/false)
|  +- skipped (true/false)
|  +- testProcessor
|  |  +- forkedProcess (configured the forked process who will run tests)
|  |  +- preActions
|  |  +- postActions
|  |  +- engineBehavior
|  |  |  +- testReportDir
|  |  |  +- progressDisplayer
|  |  |  +- launcherConfiguration (based on junit5 platform API)
|  |  +- testSelection
|  |  |  +- includePatterns
|  |  |  +- includeTags
|  +- method : run()
+- packaging (produces javadoc and source jar and bin jars)
|  +- javadocConfiguration
|  +- runtimeDependencies
|  +- manifest
|  +- fatJar (customize produced fat/uber jar if any)
|  +- methods : createJavadocJar(), createSourceJar(), createBinJar(), createFatJar(), resolveRuntimeDependencies()
+ methods :  toDependency(transitivity), getIdeSupport(), pack(), getDependenciesAsXml(), includeLocalAndTextDependencies()           
```

## Flat Facade

For convenience, `JkProject` offers a simplified facade to easily configure common settings without delving into its deeper structure.

```Java
JkProjectFlatFacade projectFacade = JkProject.of().flatFacade;
projectFacade
       .setPublishedModuleId("dev.jeka:sample-javaplugin")
       .setVersionFromGitTag()
       .mixResourcesAndSources()
       .setLayoutStyle(SIMPLE)
       .addTestExcludeFilterSuffixedBy("IT", false);
projectFacade.compileDependnecies
       .add("com.google.guava:guava:21.0")
       .add("com.sun.jersey:jersey-server:1.19.4")
       .add("org.junit.jupiter:junit-jupiter-engine:5.6.0");
projectFacade.runtimeDependencies
       .remove("org.junit.jupiter:junit-jupiter-engine")
       .add("com.github.djeang:vincer-dom:1.2.0");
projectFacade.testDependencies
       .add("org.junit.vintage:junit-vintage-engine:5.6.0");
```

See a detailed example [here](https://github.com/jeka-dev/jeka/blob/master/samples/dev.jeka.samples.project-api/jeka-src/JkProjectBuild.java).

## Project Dependencies

Project dependencies in Jeka are managed differently from Maven/Gradle. 
Instead of defining a single collection of dependencies for a specific scope/configuration, 
Jeka uses three distinct classpaths: **compile**, **runtime**, and **test**. 
Each is defined independently but related to the others.

- **Compile classpath:** Set via `JkProject.compilation.dependencies`.
- **Runtime classpath:** Built from the compile classpath, modifiable with `JkProject.packaging.runtimeDependencies`.
- **Test classpath:** Merges the compile and runtime classpaths, further customizable via `JkProject.testing.compilation.dependencies`.

To programmatically add a *compile-only* dependency, you can:

1. Add it to the *compile* classpath and exclude it from the *runtime* classpath.
2. Use the `JkFlatFacade.addCompileOnlyDeps` method.


### Full Text Description

An entire project dependency set can be declared using a full text description.

By default, if a file named `project-dependencies.txt` exists in *[PROJECT_DIR]/jeka*, its content is used to define project dependencies.

Dependencies must follow the format:  
`group:module:[classifier]:[type]:[version]`, where *classifier*, *type*, and *version* are optional.  
See `JkCoordinate.of(String description)` for details.

To import a *bill-of-materials* (BOM), declare a dependency as:  
`group:module::pom:version`.

You can use `@` and `@@` symbols to specify dependency exclusions.


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

`== COMPILE ==`  
Defines dependencies that will constitute the *compile* classpath.

`== RUNTIME ==`  
Defines dependencies that will constitute the *runtime* classpath.  
The dependencies will be the ones declared in the *== COMPILE ==* section plus the ones declared in the *== RUNTIME ==* section.  
If dependencies declared in the *== compile ==* section should not be included for the *runtime* classpath, they should 
be explicitly removed using the '-' symbol.


`== TEST ==`  
Defines dependencies that will constitute the *test* classpath.
The dependencies will be the ones declared in the *== COMPILE ==* or *== RUNTIME ==* sections (merge) plus the ones declared in the *== TEST ==* section.  

!!! tip
    If you are using Jeka plugin for Intellij, hit `ctrl+<space>` for displaying suggestions.

## Resolve Deoendendecies Programatically

## Resolve Dependencies Programmatically

To resolve dependencies that make up the runtime classpath, you can use either of the following methods:

- `JkProject.packaging.resolveRuntimeDependencies()` to fetch the resolution tree, allowing you to reason about the dependency resolution tree.
- `JkProject.packaging.resolveRuntimeDependenciesAsFiles()` to directly fetch the resolved classpath (a list of JAR files).

The second option may be faster, as it caches the result from a previous invocation.




