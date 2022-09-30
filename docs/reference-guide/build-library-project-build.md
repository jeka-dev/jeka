Jeka features high-level and low-level classes to deal with Java builds and JVM concepts.

## Java

Base classes are used as foundation for implementing Jeka high-level build API but they can be used directly in a low level build description.
These classes belong to `dev.jeka.core.api.java` [package](https://github.com/jerkar/jeka/tree/master/dev.jeka.core/src/main/java/dev/jeka/core/api/java).

* `JkClassLoader` and `JkUrlClassloader` Wrap a `java.lang.ClassLoader` adding convenient methods and classpath scanning capability.
* `JkJarPacker` A simple utility tyo create Jar or fat Jar file from compiled classes.
* `JkJavaCompiler` Wraps either a Java Compiler tool, or a *javac* process.
* `JkJavadocProcessor` A Java source processor producing standard Javadoc
* `JkJavaProcess` A utility to launch Java process (from class dirs or jars)
* `JkManifest` Stands for the manifest file to include in jar files.

## JEE

Located in package `dev.jeka.core.api.j2e`

* `JkJ2eWarArchiver` : Provides methods to generates war files, including dependency jars.
* `JkJ2eWarProjectAdapter` : Helps to adapt an existing `JkProject` to make it generate _war_ artefacts_.

## Kotlin

Located in `dev.jeka.core.api.kotlin` package.

* `JkKotlinCompiler` : Provides mean to get a suitable compiler according a given Kotlin version. This class also provides methods to 
  compile Kotlin sources in a fluent way.

* `JkKotlinModules` : Holds constants of common Kotlin library coordinates.

## Testing API

Jeka features a simple yet powerful API to launch tests. It relies entirely on JUnit5. This means that any test framework supported by Junit5 platform.

Jeka testing API mostly hides *Junit Platform*. For most of the cases, you won't need to code
against *Junit-Platform* API to launch tests with Jeka. Nevertheless, Jeka allows users to
code against *Junit-Platform* for fine-tuning.

The API classes all belongs to `dev.jeka.core.api.testing` [package](https://github.com/jerkar/jeka/tree/master/dev.jeka.core/src/main/java/dev/jeka/core/api/java/testing).

### `JkTestProcessor` 

This is the entry point to launch tests. Tests are executed using the current classloader classpath 
+ extra class path mentioned in `#launch` method arguments.
  
We can access to `JkEngineBehavior` by `JkTestProcessor#getEngineBehavior()`. From there we can 
  
  * Select output dir of the test report
  * Change how test progress is displayed
  * Modify how _JUnitPlatform_ will behave by accessing directly to the _JunitPlatform_ API 

### `JkTestSelection` 

This is the object passed as argument of `JkTestProcessor#launch` to determine which test to launch. 

It can be set using file or tag filter. It is also possible to code against  _JunitPlatform_ API (example [here](https://github.com/jerkar/jeka/blob/master/samples/dev.jeka.samples.junit5/jeka/def/Junit5Build.java)).

### `JkTestResult` 

The result of a test launch. Ir provides count for tests found, failure, skip, success ...


## Project API

This is the Jeka high-level API to build Java/JVM projects. API classes belong to  `dev.jeka.core.api.project` [package](https://github.com/jerkar/jeka/tree/master/dev.jeka.core/src/main/java/dev/jeka/core/api/project).

It introduces the concept of `JkProject` from where it performs compilation, testing, resources processing, packaging, publication and more.
`JkProject` is the root of a deep structure embracing the *parent-chaining* pattern for readability.

The API contains a lot of extension points to add specific behaviors.

Project Structure 
``` title="JkProject structure"
project
+- baseDir
+- outputDir
+- artifactProducer (define artifacts procuded by the build)
+- duplicateDependencyConflictStrategy
+- construction  (Produce packaged binaries from sources. This includes test checking)
|  +- jvmTargetVersion
|  +- sourceEncoding
|  +- javaCompiler
|  +- dependencyResolver
|  +- runtimeDependencies
|  +- manifest
|  +- fatJar (customize produced fat/uber jar if any)
|  +- compilation  (produce individual binary files from production sources. This includes resource processing, code generation, processing on .class files, ...)
|  |  +- layout (where are located source and resource files)
|  |  +- source generators (plugin mechanism for generating source files)
|  |  +- dependencies   (stands for compile dependencies)
|  |  +- preCompileActions (including resources processing)
|  |  +- compileActions (including java sources compilation. Compilation for other languages can be added here)
|  |  +- postCompileActions
|  |  +- methods : resolveDependencies(), run()
|  +- testing
|  |  +- compilation (same as above 'compilation' but for test sources/resources)
|  |  |  +- layout
|  |  |  +- dependencies (stands for test dependencies)
|  |  |  + ...
|  |  +- breakOnFailure (true/false)
|  |  +- skipped (true/false)
|  |  +- testProcessor
|  |  |  +- forkedProcess (configured the forked process who will run tests)
|  |  |  +- preActions
|  |  |  +- postActions
|  |  |  +- engineBehavior
|  |  |  |  +- testReportDir
|  |  |  |  +- progressDisplayer
|  |  |  |  +- launcherConfiguration (based on junit5 platform API)
|  |  |  +- testSelection
|  |  |  |  +- includePatterns
|  |  |  |  +- includeTags
|  |  +- method : run()
|  +- methods : createBinJar(), createFatJar(), resolveRuntimeDependencies(), getDependenciesAsXml()
|  +            includeLocalDependencies(), includeTextDependencies()            
+- documentation (produces javadoc and source jar)
|  +- javadocConfiguration
|  +- methods : createJavadocJar(), createSourceJar(), run()
+- publication (define information about module and artifacts to be published)
|  +- moduleId (group:name)
|  +- version
|  +- maven (maven specific information to be published in a Maven Repositoty)
|  |  +- dependencyCustomizer (customize the dependencies to be published)
|  |  +- mavenSpecificInfo
|  |  +- method : publish()
|  +- ivy (Ivy specific information to be published in a Ivy Repositoty)
|  |  +- dependencyCustomizer (customize the dependencies to be published)
|  |  +- ivySpecifictInfo
|  |  +- method : publish()
|  +- methods : publish(), getVersion(), getModuleId()
+ methods : getArtifacctPath(artifactName), toDependency(transitivity), getIdeSupport(), pack()
```

For simplicity sake, `JkProject` provides a facade in order to make common settings friendly,
without navigating deep into the structure. From facade, you can
setup dependencies, java version, project layout, test behavior, test selection and publication.

```Java
JkProject.of().simpleFacade()
   .configureCompileDeps(deps -> deps
           .and("com.google.guava:guava:21.0")
           .and("com.sun.jersey:jersey-server:1.19.4")
           .and("org.junit.jupiter:junit-jupiter-engine:5.6.0"))
   .configureRuntimeDeps(deps -> deps
           .minus("org.junit.jupiter:junit-jupiter-engine")
           .and("com.github.djeang:vincer-dom:1.2.0"))
   .configureTestDeps(deps -> deps
           .and("org.junit.vintage:junit-vintage-engine:5.6.0"))
   .addTestExcludeFilterSuffixedBy("IT", false)
   .setJavaVersion(JkJavaVersion.V8)
   .setPublishedModuleId("dev.jeka:sample-javaplugin")
   .setPublishedVersion("1.0-SNAPSHOT");

```

If facade is not sufficient for setting up the project build, you can use the main API.
`JkProject` instances are highly configurable.

Here is a pretty complete example inspired from the [Jeka Build Class](https://github.com/jerkar/jeka/blob/master/dev.jeka.core/jeka/def/dev/jeka/core/CoreBuild.java) .

