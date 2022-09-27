For Jeka, a _dependency_ is something that can be resolved to a set of files by a `JkDependencyResolver`.
Generally a dependency resolves to 1 file (or folder) but it can be 0 or many.

Compared to mainstream build tools, Jeka offers a simpler and more flexible model to deals 
with multiple dependency configurations required for building a project.

## Types of Dependency

A dependency is always an instance of `JkDependency`.

Jeka distinguishes mainly 3 types of dependency :

* __Arbitrary files__ located on the file system (represented by `JkFileSystemDependency` class). These files are assumed to be present on the file system when the build is running.
* __Files produced by a computation__ (represented by `JkComputedDependency` class). These files may be present on file system or not. If they are not present, the computation is run in order to produce the missing files. Generally the computation stands for the build of an external project.
* __Reference to module__ (represented by `JkModuleDependency`) hosted in a binary repository (Ivy or Maven for instance) : Jeka can consume and resolve transitively any artifact located in a repository as you would do with Maven, Ivy or Gradle.

For the last, Jeka is using _Ivy 2.5.0_ under the hood.
Jeka jar embeds Ivy and executes it in a dedicated classloader to be hidden for client code.

![image](images/JkDependency.png)

### Module dependency

This type of dependency is represented by `JkModuleDependency` class.
It stands for a Maven/Ivy dependency expressed with coordinates (e.g. _group:module:version).

This is for declaring a dependency on module hosted in _Maven_ or _Ivy_ repository. 
Basically you instantiate a `JkModuleDepency` from it's group, name and version.

```Java
JkDependencySet.of()
    .and(JkPopularModule.GUAVA, "18.0")
    .and("com.orientechnologies:orientdb-client:[2.0.8, 2.1.0[")
    .and("mygroup:mymodule:myclassifier:0.2-SNAPSHOT");
```

Many string formats are accepted to specify a module coordinate :

- _group_:_name_
- _group_:_name_:_version_
- _group_:_name_:_classifiers_:_version_
- _group_:_name_:_classifiers_:_extension_:_version_

_Classifiers_ can be either :

- an empty string to specify the default classifier
- a simple string as '_linux_' to specify a retrieve a single classifier variant
- a coma separated string as '_javadoc,sources,_' to specify several variants. Here we have specified javadoc + sources + default artefact.

_Version_ can be either :

- a static version number, as _1.0.2_
- a snapshot version, as _1.0.2-SNAPSHOT_ 
- a version range, as _[2.0.8, 2.1.0[_

Examples :

- _com.sun.jersey:jersey-server_ : specify artifact without version
- _com.sun.jersey:jersey-server:1.19.4_ : specify artifact with version
- _org.lwjgl:lwjgl:natives_linux:3.1.0_ : specify artifact having *natives_linux* classifier and _3.1.0_ version
- _org.lwjgl:lwjgl:natives_linux,:_ specify 2 artifacts having respectively *natives-linux* and *default* classifier
- _org.springframework.boot:spring-boot-dependencies::pom:2.5.6_ specify artifact having _.pom_ extension (to retrieve a BOM)

!!! note

    * A version ending by `-SNAPSHOT` has a special meaning : Jeka will consider it _"changing"_. This means that it won't cache it locally and will download the latest version from repository.
    * As Jeka relies on Ivy under the hood, it accepts dynamic versions as mentioned [here](http://ant.apache.org/ivy/history/latest-milestone/ivyfile/dependency.html).
    * Dependency files are downloaded in _[USER HOME]_/.jeka/cache/repo

Additionally, it's possible to define the transitivity of the dependency using :  

`JkModuleDependency.of("group:name:sources:zip:version").withTransitivity(JkTransitivity.NONE);`

By default, _Jeka_ uses the most relevant transitivity according the declaration context, so users don't need to specify it 
unless they want a specific one.

See later for more details about _transitivity_.

### File System Dependencies

This type of dependency is represented by `JkFileSystemDependency` class.

Just mention the path of one or several files. If one of the files does not exist at resolution time (when the dependency is actually retrieved), build fails.

```Java
JkDependencySet.of().andFiles("libs/my.jar", "libs/my.testingtool.jar");
``` 

### Computed Dependencies

This type of dependency is represented by `JkComputedDependency` class.

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
JkProcess mavenBuild = JkProcess.of("mvn", "clean", "install")
        .withWorkingDir(mavenProject);
Path mavenProjectJar = mavenProject.resolve("target/maven-project.jar");
JkJavaProject externalProject = 
        JkJavaProject.ofSimple(Paths.get("../a-jeka-project")); 
JkDependencySet deps = JkDependencySet.of()
    .and(JkComputedDependency.of(mavenBuild, mavenProjectJar))
    .and(externalProject);
```

## Dependency Set 

A _dependency set_ (`JkDependencySet`) is an ordered bunch of dependencies used for a given purpose (compilation,
war packaging, testing, ...). It can contain any kind of `JkDependency`. See [here](https://github.com/jerkar/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/api/depmanagement/JkDependencySet.java)

_dependencySet_ also defines :

* A version provider to define which version of a module we should use in case it is not explicitly mentioned. 
* A set of transitive dependency exclusion rules.

It is designed as an immutable object where we can apply set theory operations for adding, removing or
merging with other dependencies and _dependencySet_.

!!! example

    ```java
    JkDependencySet deps = JkDependencySet.of()
        .and("com.google.guava") 
        .and("org.slf4j:slf4j-simple")
        .and("com.orientechnologies:orientdb-client:2.0.8")
        .andFile("../libs.myjar")
        .withVersionProvider(myVersionProvider);
    ```

!!! note

    * Module version and scopes can be omitted when declaring dependencies. Versions can be provided by a `JkVersionProvider`.
    * Instances of `JkDependencySet` can be combined together in order to construct large _dependencySet_ from smaller ones.
    
#### Full Text Description

An entire dependency sets can be declared with full text description.
For this, just pass a string argument to `JkDependencySet#ofTextDescription` describing 
the module dependencies.

!!! example 

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

## Transitivity

For each dependency, mainstream build tools use a single concept (_scope_ or _configuration_) to determine both :

1. which part of the build needs the dependency
2. which transitive dependencies to fetch along the dependency
3. with which transitivity the dependency must be published

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

!!! notes
    On Maven repositories, published poms can declare only two scopes for transitive dependencies : 'compile' and 'runtime'.

For Ivy repositories, it is possible to declare a specific transitivity that maps to a slave 'configuration'.

The below example shows a JkJavaProject declaration using explicit transitivity.

```Java
JkJavaProject.of().simpleFacade()
    .configureCompileDeps(deps -> deps
            .and("com.google.guava:guava:23.0", JkTransitivity.NONE)
            .and("javax.servlet:javax.servlet-api:4.0.1"))
    .configureRuntimeDeps(deps -> deps
            .and("org.postgresql:postgresql:42.2.19")
            .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
            .minus("javax.servlet:javax.servlet-api"))
    .configureTestDeps(deps -> deps
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

## Resolve Dependencies

The `JkDependencyResolver` class is responsible JkDependencyResolver.of(JkRepo.ofMavenCentral());to resolve dependencies by returning `JkResolveResult` from a
`JkdependencySet`.

```java
JkDependencySet deps = JkDependencySet.of()
                            .and("org.apache.httpcomponents:httpclient:4.5.3")
                            .andFile("libs/my.jar");

// Here, module dependencies are fetched from Maven central repo
JkDependencyResolver resolver = 
        JkDependencyResolver.of(JkRepo.ofMavenCentral());  
JkResolveResult result = resolver().resolve(deps);
```

From the result you can :

* Navigate in the resolved dependency tree as :

```java
JkDependencyNode slfjApiNodeDep = result.getDependencyTree()
        .getFirst(JkModuleId.of("org.slf4j:slf4j-api"));
System.out.println(slfjApiNode.getModuleInfo().getResolvedVersion());
```

* Get the direct list of artifact files

```java
JkPathSequence sequence = result.getFiles();  
sequence.forEach(System.out::println); // print each files part of the result
```

## Publication

Jeka is able to publish on both Maven and Ivy repository. This includes repositories as [Sonatype Nexus](http://www.sonatype.org/nexus/).

Maven and Ivy have different publication model, so Jeka proposes specific APIs according you want to publish on a Maven or Ivy repository.

### Publish to a Maven repository

Jeka proposes a complete API to pubish on Maven repository. POM files will be generated by Jeka according
provided elements.

The following snippet demonstrate a pretty sophisticated publishing on Maven :

```java
JkVersionedModule versionedModule = 
        JkVersionedModule.of("org.myorg:mylib:1.2.6");
JkDependencySet deps = JkDependencySet.of()
        .and("org.slf4j:slf4j-simple", COMPILE_AND_RUNTIME)
        .and("junit:junit:4.11", TEST);
JkMavenPublication mavenPublication = 
        JkMavenPublication.of(Paths.get("org.myorg.mylib.jar"))

        // the following are optional but required to publish 
        // on public repositories.
        .and(Paths.get("org.myorg.mylib-sources.jar"), "sources")
        .and(Paths.get("org.myorg.mylib-javadoc.jar"), "javadoc")
        .withChecksums("sha-2", "md5")
        .withSigner(JkPgp.of(Paths.get("myPubring"), 
            Paths.get("mySecretRing"), "mypassword"))
        .with(JkMavenPublicationInfo.of("My sample project",
                "A project to demonstrate publishing on Jeka",
                "http://project.jeka.org")
                .andApache2License()
                .andDeveloper("djeang", "myemail@gmail.com", "jeka.org", 
                    "http://project.jeka.org/"));

// A complex case for repo (credential + signature + filtering) 
JkRepo repo = JkRepo.of("http://myserver/myrepo")
        .withOptionalCredentials("myUserName", "myPassword")
        .with(JkRepo.JkPublishConfig.of()
                    .withUniqueSnapshot(false)
                    .withNeedSignature(true)
                    .withFilter(mod -> // only accept SNAPSHOT and MILESTONE
                        mod.getVersion().isSnapshot() 
                        || mod.getVersion().getValue().endsWith("MILESTONE")
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

### Publish to a Ivy repository

Publishing on Ivy repo is pretty similar than on Maven though there is specific options to Ivy.

```java
JkVersionedModule versionedModule = 
        JkVersionedModule.of("org.myorg:mylib:1.2.6-SNAPSHOT");
JkDependencySet deps = JkDependencySet.of()
        .and("org.slf4j:slf4j-simple", COMPILE_AND_RUNTIME)
        .and("junit:junit:4.11", TEST);

JkIvyPublication publication = 
        JkIvyPublication.of(Paths.get("org.myorg.mylib.jar"), "master")
            .and(Paths.get("org.myorg.mylib-sources.jar"));

JkRepo repo = JkRepo.ofIvy(Paths.get("ivyrepo"));

JkPublisher publisher = JkPublisher.of(repo);
publisher.publishIvy(versionedModule, publication, deps, 
        JkJavaDepScopes.DEFAULT_SCOPE_MAPPING,
        Instant.now(), JkVersionProvider.of());
```

## Common Classes

* `JkRepo` and `JkRepoSet` represent both download and upload repositories. 

* `JkRepoFromProperties`provides configured repositories according global or project scopes _properties_.

* `JkDependencySet` represents a set of dependencies.

* `JkDependencyResolver` resolve dependencies to classpath and resolution result that allow resolution exploration.

* `JkModuleFileProxy` provides an smart way to get a file from its coordinates.
