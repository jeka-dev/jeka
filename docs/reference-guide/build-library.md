# The Build Library

Jeka contains a library for all regular things you need to build/test/publish projects..
The library does not depend on the execution engine and has zero dependency. 

## API Style

_Jeka_ tries to stick with a consistent API design style.

* All Jeka public classes/interfaces start with `Jk`. The reason is for easing distinction, in IDE, between classes supposed be used
  in production or test and the ones used for building. It also helps to explore Jeka API.
* As a rule of thumb _Jeka_ favors immutable objects for shallow structures and
[parent-chaining trees](https://github.com/djeang/parent-chaining/blob/master/readme.md) for deeper ones.
Both provide a fluent interface when possible.
* All objects are instantiated using static factory methods. Every factory method names start with `of`.
* All accessor method names (methods returning a result without requiring IO, only computation) starts with `get`.
* To create a subtly different object from another immutable one, _Jeka_ provides :
  * Methods starting with `with` when a property is to be replaced by another.
  * Methods starting with `and` when a collection property is to be replaced by the same one plus an extra element.
  * Methods starting with `minus` when a collection property is to be replaced by the same one minus a specified element.
* To modify a mutable object, _Jeka_ provides :
  * Methods starting with `set` to replace a single property value by another.
  * Methods starting with `add` to add a value to a collection property.
  Those methods return the object itself for chaining.

## Domains Covered by the API

The previous example demonstrates how the Java/project API can be used to build and publish Java projects. This API
relies on other lower level ones provided by _Jeka_. In a glance these are the domains covered by the _Jeka_ APIs :

* __Files :__ File trees, filters, zip, path sequence
* __System :__ Launching external process, Logging, Meta-info
* __Cryptography :__ PGP signer
* __Dependency management :__ Dependency management, publishing on repositories
* __Java :__ Compilation, javadoc, resource processor, manifest, packager, classloader, classpath, launching
  * __Testing :__ Launching tests and get reports
  * __Project :__ Project structure to build
* __Tooling :__ Eclipse integration, intellij integration, Maven interaction, Git
* __Support :__ Set of utility class with static methods to handle low-level concerns


## Files

File manipulation is a central part for building software.
Jeka embraces JDK7 *java.nio.file* API by adding some concepts around, to provide a powerful fluent style API performing
recurrent tasks with minimal effort.

The following classes lie in `dev.jeka.core.api.file` package:

* `JkPathFile` A simple wrapper for files (not folders). It provides copying, interpolation, checksum, deletion and creation methods.

* `JkPathSequence` An Immutable sequence of `java.nio.file.Path` providing methods for filtering or appending.

* `JkPathMatcher` An immutable `java.nio.file.PathMatcher` based on `java.nio.file` glob pattern or regerxp.
  Used by `JkPathTree` to filter in/out files according name patterns.

* `JkPathTree` An Immutable root folder (or a zip file) along a `PathMatcher` providing operations to copy, navigate, zip or iterate.
  This is a central class in Jeka API.

* `JkPathTreeSet` An Immutable set of `JkPathTree`. Helpful to define set of sources/resources and create jar/zip files.

* `JkResourceProcessor` A mutable processor for copying a set of files, preserving the structure and
  replacing some text by other text. Typically, used for replacing token as `${server.ip}` by an actual value.

Examples

```Java
// creates a file and writes the content of the specified url.
JkPathFile.of("config/my-config.xml").createIfNotExist().replaceContentBy("http://myserver/conf/central.xml");

// copies all non java source files to another directory preserving structure
JkPathTree.of("src").andMatching(false, "**/*.java").copyTo("build/classes");

// One liner to zip an entire directory
JkPathTree.of("build/classes").zipTo(Paths.get("mylib.jar"));

```

## System

The `dev.jeka.core.api.system` package provides system level functions :

* `JkInfo` Provides meta information as the running version of Jeka.

* `JkLocator` Provides information about where is located repository cache or Jeka user home.

* `JkLog` Provides API to log Jeka event. It supports hierarchical logs through `#startTask`
  and `#endtask` methods.

* `JkProcess` Launcher for external process.

* `JkPrompt` One-liner to ask user input.

## Dependency Management

Dependency management API let define, fetch and publish dependencies. Api classes belong to `dev.jeka.core.api.depmanagement` [package](https://github.com/jerkar/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/api/depmanagement)

### Concepts

#### Dependency

For Jeka, a _dependency_ is something that can be resolved to a set of files by a `JkDependencyResolver`.
Generally a dependency resolves to 1 file (or folder) but it can be 0 or many.

A dependency is always an instance of `JkDependency`.

Jeka distinguishes mainly 3 types of dependency :

* __Arbitrary files__ located on the file system (represented by `JkFileSystemDependency` class). These files are assumed to be present on the file system when the build is running.
* __Files produced by a computation__ (represented by `JkComputedDependency` class). These files may be present on file system or not. If they are not present, the computation is run in order to produce the missing files. Generally the computation stands for the build of an external project.
* __Reference to module__ (represented by `JkModuleDependency`) hosted in a binary repository (Ivy or Maven for instance) : Jeka can consume and resolve transitively any artifact located in a repository as you would do with Maven, Ivy or Gradle.

For the last, Jeka is using _Ivy 2.5.0_ under the hood.
Jeka jar embeds Ivy and executes it in a dedicated classloader to be hidden for client code.

![image](images/JkDependency.png)

**JkModuleDependencies (dependency on module through coordinates)**

This is for declaring a dependency on module hosted in _Maven_ or _Ivy_ repository. Basically you instantiate a `JkModuleDepency` from it's group, name and version.

```Java
    JkDependencySet.of()
        .and(JkPopularModule.GUAVA, "18.0")
        .and("com.orientechnologies:orientdb-client:[2.0.8, 2.1.0[")
        .and("mygroup:mymodule:myclassifier:0.2-SNAPSHOT");
```
There is many way to indicate a module dependency, see Javadoc for browsing possibilities.

Note that :

* A version ending by `-SNAPSHOT` has a special meaning : Jeka will consider it _"changing"_. This means that it won't cache it locally and will download the latest version from repository.
* As Jeka relies on Ivy under the hood, it accepts dynamic versions as mentioned [here](http://ant.apache.org/ivy/history/latest-milestone/ivyfile/dependency.html).
* Dependency files are downloaded in _[USER HOME]_/.jeka/cache/repo

**JkFileSystemSependency (dependency on local files)**

Just mention the path of one or several files. If one of the files does not exist at resolution time (when the dependency is actually retrieved), build fails.

```Java
    JkDependencySet.of().andFiles("libs/my.jar", "libs/my.testingtool.jar");
``` 

**JkComputedDependenciy (dependency on files produced by computation)**

It is typically used for _multi-modules_ or _multi-techno_ projects.

The principle is that if the specified files are not present, the computation is run in order to generate the missing files.
If some files still missing after the computation has run, the build fails.

This mechanism is quite simple yet powerful as it addresses following use cases :

* Dependencies on files produced by an artifact producer (`JkArtifactProducer`). A `JkProject` is an artifact producer.
* Dependencies on files produced by external build tool (Ant, Maven, Gradle, SBT, Android SDK, Make, npm ...).
* ... In other words, files produced by any means.

The generic way is to construct this kind of dependency using a `java.lang.Runnable`.

The following snippet constructs a set of dependencies on two external projects : one is built with Maven, the other with
_Jeka_.
```Java
Path mavenProject = Paths.get("../a-maven-project");
JkProcess mavenBuild = JkProcess.of("mvn", "clean", "install").withWorkingDir(mavenProject);
Path mavenProjectJar = mavenProject.resolve("target/maven-project.jar");
JkJavaProject externalProject = JkJavaProject.ofSimple(Paths.get("../a-jeka-project")); 
JkDependencySet deps = JkDependencySet.of()
    .and(JkComputedDependency.of(mavenBuild, mavenProjectJar))
    .and(externalProject);
```

#### DependencySet 

A _dependencySet_ (`JkDependencySet`) is an ordered bunch of dependencies used for a given purpose (compilation,
war packaging, testing, ...). It can contain any kind of `JkDependency`. See [here](https://github.com/jerkar/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/api/depmanagement/JkDependencySet.java)

_dependencySet_ also defines :

* A **version provider** to define which version of a module we should use in case it is not explicitly mentioned. It is possible to populate a version provider by passing a _BOM_ coordinate.
* A set of transitive dependency exclusion rules.

It is designed as an immutable object where we can apply set theory operations for adding, removing or
merging with other dependencies and _dependencySet_.
 
<details>
 <summary>Example of dependency set</summary>


```Java
JkDependencySet deps = JkDependencySet.of()
    .and("com.google.guava") 
    .and("org.slf4j:slf4j-simple")
    .and("com.orientechnologies:orientdb-client:2.0.8")
    .andFile("../libs.myjar")
    .withVersionProvider(myVersionProvider);
```
 </details>

Note that :

* Module version and scopes can be omitted when declaring dependencies. Versions can be provided by a `JkVersionProvider`.
* Instances of `JkDependencySet` can be combined together in order to construct large _dependencySet_ from smaller ones.
* `JkDependencySet#ofTextDescription` provides a mean to instantiate a dependency set from a simple text.

 <details>
 <summary>Example of text describing dependencies</summary>

```
- COMPILE+RUNTIME
org.springframework.boot:spring-boot-starter-thymeleaf
org.springframework.boot:spring-boot-starter-data-jpa

- RUNTIME
com.h2database:h2
org.liquibase:liquibase-core
com.oracle:ojdbc6:12.1.0

- TEST
org.springframework.boot:spring-boot-starter-test
org.seleniumhq.selenium:selenium-chrome-driver:3.4.0
org.fluentlenium:fluentlenium-assertj:3.2.0
org.fluentlenium:fluentlenium-junit:3.2.0

- COMPILE
org.projectlombok:lombok:1.16.16
```
 </details>
 
</details>


#### Transiitivity

Mainstream build tools use a single concept ('scope' or 'configuration') to determine both :

1. Which part of the build needs the dependency
2. Which transitive dependencies to fetch along the dependency. 
3. If the dependency must be part of the transitive dependencies according a configuration. 

This confusion leads in dependency management systems that are bloated, difficult to reason about and not quite flexible.
Gradle comes with a proliferation of 'configurations' to cover most use case combinations,
while Maven narrows 'scopes' to a fewer but with limitations and not-so-clear transitivity/publish rules.

In the opposite, Jeka distinguishes clearly the three purposes :

1. Jeka uses distinct _dependencySet_ instances for each part of the build (compile, runtime, test,...). Each can be
   defined relatively to another using set theory operations.
2. For each dependency, we can decide its transitivity, that is, the transitive dependencies fetched along the dependency.
3. For publishing, we can optionally re-define a specific _dependencySet_, exposing exactly what we want.

Jeka defines by default, 3 levels of transitivity :

- NONE : Not transitive
- COMPILE : Also fetch transitive dependencies declared with scope 'compile' in the dependency published pom.
- RUNTIME : Also fetch transitive dependencies declared with any scope in the dependency published pom.

Reminder : on Maven repositories, published poms can declare only two scopes for transitive dependencies : 'compile'
and 'runtime'.

For Ivy repositories, it is possible to declare a specific transitivity that maps to a slave 'configuration'.

The below example shows a JkJavaProject declaration using explicit transitivity.

```Java
JkJavaProject.of().simpleFacade()
    .setCompileDependencies(deps -> deps
            .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
            .and("javax.servlet:javax.servlet-api:4.0.1"))
    .setRuntimeDependencies(deps -> deps
            .and("org.postgresql:postgresql:42.2.19")
            .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
            .minus("javax.servlet:javax.servlet-api"))
    .setTestDependencies(deps -> deps
            .and(Hint.first(), "org.mockito:mockito-core:2.10.0")
    )
```
It results in :
```
Declared Compile Dependencies : 2 elements.
  com.google.guava:guava:23.0 transitivity:NONE
  javax.servlet:javax.servlet-api:4.0.1
  
Declared Runtime Dependencies : 2 elements.
  com.google.guava:guava:23.0 transitivity:RUNTIME
  org.postgresql:postgresql:42.2.19
  
Declared Test Dependencies : 4 elements.
  org.mockito:mockito-core:2.10.0
  com.google.guava:guava:23.0 transitivity:RUNTIME
  org.postgresql:postgresql:42.2.19
  javax.servlet:javax.servlet-api:4.0.1
```
Dependencies without any transitivity specified on, will take default transitivity for
their purpose, namely COMPILE for compile dependencies, and RUNTIME for runtime and test dependencies.

The API allows to redefine the transitivity declared in a upper dependency set.

Note that transitivity can only apply to `JkModuleDependency` (like <i>com.google.guava:guava:23.0</i>)
and `JkLocalProjectDependency`.

### Resolve Dependencies

The `JkDependencyResolver` class is responsible JkDependencyResolver.of(JkRepo.ofMavenCentral());to resolve dependencies by returning `JkResolveResult` from a
`JkdependencySet`.

```java
JkDependencySet deps = JkDependencySet
                            .of("org.apache.httpcomponents:httpclient:4.5.3")
                            .andFile("libs/my.jar");

// Here, module dependencies are fetched from Maven central repo
JkDependencyResolver resolver = JkDependencyResolver.of(JkRepo.ofMavenCentral());  
JkResolveResult result = resolver().resolve(deps);
```

From the result you can :

* Navigate in the resolved dependency tree as :

```java
JkDependencyNode slfjApiNodeDep = result.getDependencyTree().getFirst(JkModuleId.of("org.slf4j:slf4j-api"));
System.out.println(slfjApiNode.getModuleInfo().getResolvedVersion());
```

* Get the direct list of artifact files

```java
JkPathSequence sequence = result.getFiles();  
sequence.forEach(System.out::println); // print each files part of the dependency resolution
```

### Publication

Jeka is able to publish on both Maven and Ivy repository. This includes repositories as [Sonatype Nexus](http://www.sonatype.org/nexus/).

Maven and Ivy have different publication model, so Jeka proposes specific APIs according you want to publish on a Maven or Ivy repository.

#### Publish to a Maven repository

Jeka proposes a complete API to pubish on Maven repository. POM files will be generated by Jeka according
provided elements.

The following snippet demonstrate a pretty sophisticated publishing on Maven :

```java
    JkVersionedModule versionedModule = JkVersionedModule.of("org.myorg:mylib:1.2.6");
    JkDependencySet deps = JkDependencySet.of()
            .and("org.slf4j:slf4j-simple", COMPILE_AND_RUNTIME)
            .and("junit:junit:4.11", TEST);
    JkMavenPublication mavenPublication = JkMavenPublication.of(Paths.get("org.myorg.mylib.jar"))

            // the following are optional but required to publish on public repositories.
            .and(Paths.get("org.myorg.mylib-sources.jar"), "sources")
            .and(Paths.get("org.myorg.mylib-javadoc.jar"), "javadoc")
            .withChecksums("sha-2", "md5")
            .withSigner(JkPgp.of(Paths.get("myPubring"), Paths.get("mySecretRing"), "mypassword"))
            .with(JkMavenPublicationInfo.of("My sample project",
                    "A project to demonstrate publishing on Jeka",
                    "http://project.jeka.org")
                    .andApache2License()
                    .andDeveloper("djeang", "myemail@gmail.com", "jeka.org", "http://project.jeka.org/"));

    // A complex case for repo (credential + signature + filtering) 
    JkRepo repo = JkRepo.of("http://myserver/myrepo")
            .withOptionalCredentials("myUserName", "myPassword")
            .with(JkRepo.JkPublishConfig.of()
                        .withUniqueSnapshot(false)
                        .withNeedSignature(true)
                        .withFilter(mod -> // only accept SNAPSHOT and MILESTONE
                            mod.getVersion().isSnapshot() || mod.getVersion().getValue().endsWith("MILESTONE")
                        ));
    
    // Actually publish the artifacts
    JkPublisher publisher = JkPublisher.of(repo);
    publisher.publishMaven(versionedModule, mavenPublication, deps);
```

Notice that Jeka allows to :

- Publish more than one artifact.
- Produce & publish checksum files for each published artifact.
- Mention to use unique snapshot ([What is it ?](http://stackoverflow.com/questions/1243574/how-to-stop-maven-artifactory-from-keeping-snapshots-with-timestamps)).
- Feed generated pom with data necessary to publish on [central repository](https://maven.apache.org/guides/mini/guide-central-repository-upload.html).
- Sign published artifact with PGP
- Publish to multiple repository by creating the publisher using a `JkRepoSet` instead of a `JkRepo`.

To sign with PGP, no need to have PGP installed on Jeka machine. Jeka uses <a href="https://www.bouncycastle.org/">Bouncy Castle</a> internally to sign artifacts.

#### Publish to a Ivy repository

Publishing on Ivy repo is pretty similar than on Maven though there is specific options to Ivy.

```java
    JkVersionedModule versionedModule = JkVersionedModule.of("org.myorg:mylib:1.2.6-SNAPSHOT");
    JkDependencySet deps = JkDependencySet.of()
            .and("org.slf4j:slf4j-simple", COMPILE_AND_RUNTIME)
            .and("junit:junit:4.11", TEST);

    JkIvyPublication publication = JkIvyPublication.of(Paths.get("org.myorg.mylib.jar"), "master")
            .and(Paths.get("org.myorg.mylib-sources.jar"));

    JkRepo repo = JkRepo.ofIvy(Paths.get("ivyrepo"));

    JkPublisher publisher = JkPublisher.of(repo);
    publisher.publishIvy(versionedModule, publication, deps, JkJavaDepScopes.DEFAULT_SCOPE_MAPPING,
            Instant.now(), JkVersionProvider.of());
```

## Project Building

Jeka features high-level and low-level classes to deal with Java builds and JVM concepts.

### Java Tool Base API

Base classes are used as foundation for implementing Jeka high-level build API but they can be used directly in a low level build description.
These classes belong to `dev.jeka.core.api.java` [package](https://github.com/jerkar/jeka/tree/master/dev.jeka.core/src/main/java/dev/jeka/core/api/java).

* `JkClassLoader` and `JkUrlClassloader` Wrap a `java.lang.ClassLoader` adding convenient methods and classpath scanning capability.

* `JkJarPacker` A simple utility tyo create Jar or fat Jar file from compiled classes.

* `JkJavaCompiler` Wraps either a Java Compiler tool, nor a *javac* process.

* `JkJavadocProcessor` A Java source processor producing standard Javadoc

* `JkJavaProcess` A utility to launch Java process (from class dirs or jars)

* `JkManifest` Stands for the manifest file to include in jar files.

### Testing API

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


### Project API

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


## Third Party Tool Integration

The `dev.jeka.core.api.tooling` package provides integration with tools developers generally deal with.

### Eclipse

`JkEclipseClasspathGenerator` and `JkEclipseProjectGenerator` provides method to generate a proper .classpath and .project file respectively.

`JkEclipseClasspathApplier` reads information from a .classpath file.

### Intellij

`JkIntellijImlGenerator` generates proper .iml files.

### Git

`JkGitWrapper` wraps common Git commands in a lean API.

### Maven

`JkMvn` wraps Maven command line in a lean API

`JkPom` reads POM/BOM to extract information like : declared dependencies, dependency management, repos,
properties, version and artifactId.


