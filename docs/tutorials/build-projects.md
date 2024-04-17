# Building Projects

Here, We use JeKa script and kbean capabilities to build a Java project.

## Scaffold new project

Execute: `jeka project: scaffold` to create a typical project structure, from where 
you can directly start coding.

Execute: `jeka intellij: iml` to sync the project with Intellij.

If the project seems not being reflected in IntelliJ, go to the Intellij project root dir, then execute `jeka intellij: initProject`.

## Configure project with text

We can configure basic aspects of a project using text configuration only.

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

By default, JeKa produce regular jar files. Nevertheless it can be configured to produce fat jar (jar including all dependencies).


Edit *jeka.properties* and add :
```properties
@project.pack.jarType=FAT
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

Compile sources
```shell
jeka project: compile
```

Clean result and compile
```shell
jeka project: clean compile
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

Execute the main method from compiled classes
```shell
jeka project: runMain programArgs="myArg1 myArg2"
```

Execute the main method from the packed jar.
```shell
jeka project: runJar programArgs="myArg1 myArg2"
```

Create an executable Docker file
```shell
jeka docker: build
```

Create an executable Docker file
```shell
jeka docker: run programArgs
```