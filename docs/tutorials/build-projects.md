# Building Projects

Here, We use JeKa script and kbean capabilities to build a Java project.

## Scaffold new project

Execute: `jeka project: scaffold` to create a typical project structure, from where 
you can directly start coding.

Execute: `jeka intellij: iml` to sync the project with Intellij.

If the project seems not being reflected in IntelliJ, go to the Intellij project root dir, then execute `jeka intellij: initProject`.

!!! tip
    A README.md file is generated at the root of the project, mentioning the usual commands to run.

## Configure project with properties

We can configure basic aspects of a project using properties configuration only.

### Add dependencies

Add `org.junit.jupiter:junit-jupiter:5.8.1` to the `== TEST ==` section in *dependencies.txt*, then execute `jeka intelllij: iml`

If you want to add a *compile-only* dependency, add it to the `== COMPILE ==` section then explicitly substract it from 
the `== RUNTIME ==` section using `-` symbol. 

Example:
```txt
== COMPILE ==
org.projectlombok:lombok:1.18.32

== RUNTIME ==
-org.projectlombok:lombok  # indicate to not include lombok in runtime classpath.

== TEST == 
org.junit.jupiter:junit-jupiter:5.8.1
```

### Configure jar type

By default, JeKa produce regular jar files. Nevertheless it can be configured to produce *fat jar* (jar including all dependencies), 
or *shade jar* (same with relocated packages to avoid classpath collision).

Edit *jeka.properties* and add :
```properties
@project.pack.jarType=FAT
```
This configure project to create a single jar which is a *fat jar*. This is perfectly fine for bundling applications.

If you bundle a library, you may prefer to create an additional *shade* jar, that embeds dependency jars gracefully.

For this, edit *jeka.properties* and add :
```properties
# Create an extra jar suffixed with 'all-deps*
@project.pack.shadeJarClassifier=all-deps

# Instruct Maven plugin to publish it as well.
@maven.publication.extraArtifacts=all-deps
```

### Configure Layout

By default, project structure adopt Maven one, meaning sources in *src/main/java* ans so on.

You can modify it by adding the following properties in *jeka.properties*.

```properties
# sources in src dir and tests in test dir
@project.layout.style=SIMPLE

# sources and resources sharing the same dir
@project.layout.mixSourcesAndResources=true
```
!!! tip
    List available options by executing: `jeka project: --help`.

### Use common plugins

#### Handle versioning with git

You can use Git to automatically handle versioning for your project. 
The version will value *[TAG]* if the workspace is currently on a tag, or *[BRANCH]-SNAPSHOT* if it is not.

The Manifest file will also be augmented with Git info (commit, dirty, branch, ...)

```properties
@git=
@git.handleVersioning=true
```
#### Perform static analysis using Sonarqube and Jacoco

We need to import the Sonarqube and Jacoco project. We also set a shorthand for easy invoking.

We can also select the exact version for each tool. 

SonarQube can be configured directly using native configuration settings present in the *jeka.properties* file. 
Properties related to the SonarQube project/version are automatically picked up from the project information.

```properties 
jeka.inject.classpath=dev.jeka:sonarqube-plugin dev.jeka:jacoco-plugin

jeka.cmd.pack_quality=project: pack jacoco: sonarqube: run logOutput=true
@jacoco.jacocoVersion=0.8.11
@sonarqube.scannerVersion=5.0.1.3006

sonar.host.url=http://localhost:9000
```

## Execute build actions

This is a list of common commands used when building projects. This commands may be combined 
and use in conjunction on `--clean (-c)` option for cleaning the output dir prior running.

Generates source code
```shell
jeka project: generateSources
```

Compile sources
```shell
jeka project: compile --clean
```

Compile (if needed) and execute tests
```shell
jeka project: test
```

Compile and test (if needed) and create artifacts as jars.
```shell
jeka project: pack
```

Same, but skipping the test execution
```shell
jeka poject: pack -Djeka.skip.tests=true
```

Display project info on console
```shell
jeka project: info
```

Display the tree of transitive dependencies
```shell
jeka project: depTree
```

Execute the main method from compiled classes
```shell
jeka project: runMain programArgs="myArg1 myArg2"
```

Execute the main method from the packed jar.
```shell
jeka project: runJar programArgs="myArg1 myArg2"
```

### Docker commands

Create an executable Docker file
```shell
jeka docker: build
```

Run the executable Docker file
```shell
jeka docker: run programArgs
```

### Maven publish commands

Publish on [Maven repository](reference/properties/#repositories)
```shell
jeka maven: publish 
```

Publish on local Maven repository
```shell
jeka maven: publishLocal
```

## Configure programmatically

Project can also be configured programmatically. This is often needed when we want to 
include extra build actions or adopt distinct behaviors on specific conditions.

When we have scaffolded the project, a *Build.java* class was created on purpose for customizing 
the project.

```java
class Build extends KBean {

    final JkProject project = load(ProjectKBean.class).project;

    /*
     * Configures KBean project
     * When this method is called, option fields have already been injected from command line.
     */
    @Override
    protected void init() {
        // configure project instance here
    }

}
```

The `init()`is the designed place for configuring the `JkProject` instance. 

The `JkProject` class models all what is needed to build a JVM project. This is a huge object model that 
contains both configuration and methods that actually *'build'*.

The API is too large and deep to be detailed here, we can just mention some useful links :

`JkProject.flatFacade()` propose a simplified access to the API for most frequent cases.

  - [Reference](reference/api-project)
  - [JkProject source code](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/api/project/JkProject.java)


### Example with Flat Facade

```java
class Build extends KBean {

    boolean runIntegrationTests;

    final JkProject project = load(ProjectKBean.class).project;

    
    @Override
    protected void init() {
        project.flatFacade()
                   // change dependency ordering : guava must be before commons-lang
                .customizeRuntimeDeps(deps -> deps    
                        .withMoving("com.google.guava:guava", before("commons-lang:commons-lang")))
                   // Set how tests execution are displayed on console
                .setTestProgressStyle(JkTestProcessor.JkProgressOutputStyle.BAR)
                   // Run test ending with 'IT' only on a specific condition
                .addTestIncludeFilterSuffixedBy("IT", runIntegrationTests);
    }

}
```

### Example with JkProject

```java
import dev.jeka.core.api.depmanagement.JkTransitivity;
import dev.jeka.core.api.depmanagement.artifact.JkArtifactId;
import dev.jeka.core.api.depmanagement.publication.JkMavenPublication;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;

class Build extends KBean {

    final JkProject project = load(ProjectKBean.class).project;

    final JkMavenPublication mavenPublication = load(MavenKBean.class).getMavenPublication();


    @Override
    protected void init() {

        // Add extra actions
        project.compilation.preCompileActions.append("Resources generation", this::generateSomeResources);
        project.packActions.append("Create bin distrib", this::createBinDistrib);

        // Customize Maven publication
          // -- customize published transitive dependencies
        mavenPublication.customizeDependencies(dep -> dep
                .withTransitivity("com.google.guava:guava", JkTransitivity.COMPILE)
        );
          // -- add a new artifact file to publish, along the method to create it
        mavenPublication.putArtifact(JkArtifactId.of("fat", "jar"), project.packaging::createFatJar);
          // -- describe the POM metadata
        mavenPublication.pomMetadata
                .addMitLicense()
                .addGithubDeveloper("John Doe", "Doe@djoe.com")
                .setProjectDescription("......");
    }

    private void generateSomeResources() {
        // .. do something
    }

    private void createBinDistrib() {
        // .. do something
    }

}
```

