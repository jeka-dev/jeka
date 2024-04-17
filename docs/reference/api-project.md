# Project API

This is the Jeka high-level API to build Java/JVM projects. API classes belong to  `dev.jeka.core.api.project` [package](https://github.com/jerkar/jeka/tree/master/dev.jeka.core/src/main/java/dev/jeka/core/api/project).

It introduces the concept of `JkProject` from where it performs compilation, testing, resources processing, packaging, publication, and more.
`JkProject` is the root of a deep structure embracing the *parent-chaining* pattern for readability.

The API contains a lot of extension points to add specific behaviors.

### Project Structure 
``` title="JkProject structure"
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

For convenience, `JkProject` provides a facade in order to make common settings friendly,
without navigating deep into the structure. From facade, you can
setup dependencies, java version, project layout, test behavior, test selection and publication.

```Java
JkProject.of().flatFacade()
   .configureCompileDependencies(deps -> deps
           .and("com.google.guava:guava:21.0")
           .and("com.sun.jersey:jersey-server:1.19.4")
           .and("org.junit.jupiter:junit-jupiter-engine:5.6.0"))
   .configureRuntimeDependencies(deps -> deps
           .minus("org.junit.jupiter:junit-jupiter-engine")
           .and("com.github.djeang:vincer-dom:1.2.0"))
   .configureTestDependencies(deps -> deps
           .and("org.junit.vintage:junit-vintage-engine:5.6.0"))
   .addTestExcludeFilterSuffixedBy("IT", false)
   .setJavaVersion(JkJavaVersion.V8)
   .setPublishedModuleId("dev.jeka:sample-javaplugin")
   .setPublishedVersion("1.0-SNAPSHOT");

```

If the façade is not sufficient to set up the project build, you can use the main API.
`JkProject` instances are highly configurable.

Here is a pretty complete example inspired from the [Jeka Build Class](https://github.com/jerkar/jeka/blob/master/dev.jeka.core/jeka/def/dev/jeka/core/CoreBuild.java) .

### Dependencies

Project dependencies are managed differently than in Maven/Gradle. Instead of defining 
a single collection of dependencies, each bounded for a specific scope/configuration, 
Jeka projects define 3 distinct classpaths : compile, runtime and test.

Each classpath defines its own set of dependencies independently, though they are defined relatively to each other.

*Compile classpath :* is defined using `project.getCompilation().configureDependencies()`.

*Runtime classpath :* is defined from *Compile Classpath*. This base can be modified using `project.packaging.configureDependencies()`.

*Test classpath :* is defined from a merge of *Compile Classpath* and *Runtime Classpath*. This base can be modified 
using `project.getTesing().getCompilation().configureDependencies()`

#### Full Text Description

An entire project dependency set can be declared with full text description.

By default, if a file named `project-dependencies.txt` is present in *[PROJECT_DIR]/jeka*, this content is taken 
in account to specify project dependencies.

Dependencies have to be declared with the format `group:module:[classifier]:[type]:[version]` where *classifier*, *type* and *version' are optional.  
See `JkCoordinate.of(String description)* for details.

To import *bill-of-materials* (aka *BOM*) just declare a dependency as 'group:module::pom:version'

Symbols `@` and `@@` can be used to mention dependency exclusions.

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







