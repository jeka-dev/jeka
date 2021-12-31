Jeka features high-level and low-level classes to deal with Java builds and JVM concepts.

## Java Tool Base API

Base classes are used as foundation for implementing Jeka high-level build API but they can be used directly in a low level build description.
These classes belong to `dev.jeka.core.api.java` [package](https://github.com/jerkar/jeka/tree/master/dev.jeka.core/src/main/java/dev/jeka/core/api/java).

* `JkClassLoader` and `JkUrlClassloader` Wrap a `java.lang.ClassLoader` adding convenient methods and classpath scanning capability.

* `JkJarPacker` A simple utility tyo create Jar or fat Jar file from compiled classes.

* `JkJavaCompiler` Wraps either a Java Compiler tool, nor a *javac* process.

* `JkJavadocProcessor` A Java source processor producing standard Javadoc

* `JkJavaProcess` A utility to launch Java process (from class dirs or jars)

* `JkManifest` Stands for the manifest file to include in jar files.

## Testing API

Jeka features a simple yet powerful API to launch tests. It relies entirely on JUnit5. This means that any test framework supported by Junit5 platform.

Jeka testing API mostly hides *Junit Platform*. For most of the cases, you won't need to code
against *Junit-Platform* API to launch tests with Jeka. Nevertheless, Jeka allows users to
code against *Junit-Platform* for fine-tuning.

The API classes all belongs to `dev.jeka.core.api.java.testing` [package](https://github.com/jerkar/jeka/tree/master/dev.jeka.core/src/main/java/dev/jeka/core/api/java/testing).

* `JkTestProcessor` This is the entry point to launch tests. Tests are executed using the
  current classloader classpath + extra class path mentioned in `#launch` method arguments.
* `JkTestResult` The result of a test launch : count for found, failure, skip, success ...
* `JkTestSelection` A mean to determine which test to launch. It can be set using file or tag filter. It is
  also possible to code against *JUnit Platform*


## Project API

This is the Jeka high-level API to build Java/JVM projects. API classes belong to  `dev.jeka.core.api.project` [package](https://github.com/jerkar/jeka/tree/master/dev.jeka.core/src/main/java/dev/jeka/core/api/project).

It introduces the concept of `JkProject` from where is performed compilation, testing, resources processing, packaging, publication and more.
`JkProject` is the root of a deep structure embracing the *parent-chaining* pattern for readability.

The API contains a lot of extension points to add specific behaviors.

<details>
 <summary>Project API structure</summary>

```
project
+- baseDir
+- outputDir
+- artifactProducer (define artifacts to be produce by the build as map of artifactName -> Consumer<Path> producing the artifact)
+- duplicateDependencyConflictStrategy
+- construction  (Produce packaged binaries from sources. This includes test checking)
|  +- jvmTargetVersion
|  +- sourceEncoding
|  +- javaCompiler
|  +- dependencyResolver
|  +- runtimeDependencies
|  +- manifest
|  +- fatJar (customize produced fat/uber jar if any)
|  +- compilation  (produce individual binary files from production sources. This includes resource processing, code generation, transpiling, post binary processing, ...)
|  |  +- layout (where are located source and resource files)
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
|  |  |  |  +- progressDisplayer
|  |  |  |  +- launcherConfiguration (based on junit5 platform API)
|  |  |  +- testSelection
|  |  |  |  +- includePatterns
|  |  |  |  +- includeTags
|  |  +- method : run()
|  +- methods : createBinJar(), createFatJar(), resolveRuntimeDependencies(), getDependenciesAsXml()
|  +            includeLocalDependencies(), includeTextDependencies()            
+- documentation (mainly procude javadoc and source jar)
|  +- javadocConfiguration
|  +- methods : createJavadocJar(), createSourceJar(), run()
+- publication (define information about module and artifacts to be published)
|  +- moduleId (group:name)
|  +- version
|  +- maven (maven specific information to be published in a Maven Repositoty)
|  |  +- dependencyCustomizer (customize the dependencies to be published)
|  |  +- mavenSpecificInfo
|  |  +- methods : publish
|  +- ivy (Ivy specific information to be published in a Ivy Repositoty)
|  |  +- dependencyCustomizer (customize the dependencies to be published)
|  |  +- ivySpecifictInfo
|  |  +- method : publish()
|  +- methods : publish(), getVersion(), getModuleId()
+ methods : getArtifacctPath(artifactName), toDependency(transitivity), getIdeSupport(), pack()
```
 </details>

For simplicity’s sake, `JkProject` provides a facade in order to setup common settings friendly,
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

If facade is not sufficient for setting up project build, it's still possible to complete through the main API.
`JkProject` instances are highly configurable.

Here is a pretty complete example inspired from the [Jeka Build Class](https://github.com/jerkar/jeka/blob/master/dev.jeka.core/jeka/def/dev/jeka/core/CoreBuild.java) .

