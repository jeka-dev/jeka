# Build Projects

In this tutorial, we'll use the `project` KBean to build a Java application or library. 
This KBean provides build methods and a project layout similar to those of Maven and Gradle.

If you prefer a lighter structure, you can go to [Build Base](build-base.md).

**Prerequisite:** Jeka must be [installed](../installation.md).

!!! tip
    Run `jeka project: --doc` to see all available options.

## Scaffold a New Project

Run `jeka project: scaffold` to create a standard project structure, ready for you to start coding right away.

You’ll get the following project structure:
```
.
├── src                  
│   ├── main             <- Java code and reources
│   │   ├── java
│   │   └── resources    
│   └── test             <- Java code and reources for tests
│       ├── java
│       └── resources 
├── jeka-src             <- Optional Java (or Kotlin) code for building the project
│   └── Build.java      
├── jeka-output          <- Generated dir where artifacts as jars, classes, reports or doc are generated
├── dependencies.txt     <- Dependency lists for compile, runtime and testing
├── jeka.properties      <- Build configuration  (Java and jeka version, kben configurations, ...)
├── jeka.ps              <- Optional Powershell script to boot Jeka on Windows
├── jeka                 <- Optional bash script to boot Jeka on Linuw/MacOS
└── README.md            <- Describes available build commands for building the project
```

The *jeka.ps* and *jeka* OS scripts are needed only if you want to build your project on a machine where Jeka is not installed.  
*Build.java* can also be removed if you don't need advanced settings.  

You can choose a simpler code layout structure by setting the following properties:
```properties title="jeka.properties"
@project.layout.style=SIMPLE
@project.layout.mixSourcesAndResources=true
```
You'll end up with the following code layout:
```
.
├── src       <- Contains both Java code and resooources    
├── test      <- Contains both Java code and resooources for testing
```

Additionally, you can copy-paste JAR files into the following directory structure to automatically include them as dependencies:
```
.
├── libs                  
│   ├── compile          <- Jars included in compile, runtime and test classpaths 
│   ├── compile-only     <- Jars included in compile and test classpaths
│   ├── runtime          <- Jars included in runtime and test classpaths
│   └── test             <- Jars included in test classpath
```

## Sync with IntelliJ

Run: `jeka intellij: iml --force` to sync the project with IntelliJ.  
If changes don't appear in IntelliJ, go to the project's root directory, then run: `jeka intellij: initProject`.

## Add Dependencies

Dependencies are listed in the *dependencies.txt* file as shown below:

```txt title="dependencies.txt"
== COMPILE ==
com.google.guava:guava:33.4.0-jre
org.projectlombok:lombok:1.18.32

== RUNTIME ==
org.postgresql:postgresql:42.7.4
-org.projectlombok:lombok  # indicate to not include lombok in runtime classpath.

== TEST == 
org.junit.jupiter:junit-jupiter:5.8.1
```

You can also reference a local JAR by specifying its relative path, such as `mylibs/libs.jar`, 
instead of using Maven coordinates.

!!! info
    For a *compile-only* dependency, add it to the `== COMPILE ==` section and remove it from the `== RUNTIME ==` section using the `-` symbol.

## Configure JAR Type
By default, JeKa produces regular JAR files. 
You can configure it to create a *fat JAR* (JAR that includes all dependencies) 
or a *shade JAR* (same as fat JAR but with relocated packages to avoid classpath collisions).

To configure, edit *jeka.properties* and add:
```properties title="jeka.properties"
@project.pack.jarType=FAT
```
This configures the project to create a single jar, also known as a *fat jar*.  
This is ideal for bundling applications. This properties accepts `REGULAR`, `FAT`, and `SHADE` values.

If you are bundling a library, you may prefer to create an additional *shade* jar, in addition of the regular one, so consumers 
can choose which one to pick.

To do this, edit *jeka.properties* and add:
```properties title="jeka.properties"
# Create an extra jar suffixed with 'all*
@project.pack.shadeJarClassifier=all

# Instruct Maven plugin to publish it as well.
@maven.publication.extraArtifacts=all
```

## Handle Versioning

Setting a version or group/moduleId is optional.  
To set a specific version, add `@project.version=1.0-SNAPSHOT` to *jeka.properties*.
```properties title="jeka.properties"
@project.moduleId=org.mygroup:my-module-id
@project.version=1.0.0-SNAPSHOT
```
You can override any property by passing a command-line argument, for example: `-D@project.version=1.0.0`.

A better approach might be to infer the version from Git. The version will be set to *[TAG NAME]* if the workspace is on a tag, or *[BRANCH]-SNAPSHOT* if it is not.

The Manifest file will also include Git information (commit, dirty, branch, etc.). To enable this, use the following property:
```properties title="jeka.properties"
@project.gitVersioning.enable=true
```

## Pre-defined Build Commands

```title="Common Options"
--clean (-c)      # Deletes µjeka-output* dir prior running
--verbose (-v)    # Displays verbos traces
```

``` title="Run Code Directly"
jeka --program (-p) arg0 arg1 ...  # Runs the first native executable or jar found in *jeka-output*, execute a build prior if nothing found.
```

``` title="From *project* KBean"
jeka project: compile    # Compiles sources
jeka project: test       # Compiles sources + runs tests
jeka project: pack       # Compiles sources + runs tests + creates jars
jeka project: runJar     # Runs the jar generated by the above command
jeka project: info       # Displays project configuration info
jeka project: depTree    # Displays dependency trees 
```

``` title="From *native* KBean"
jeka native: compile     # Compiles to native executable
```

``` title="From *docker* KBean"
jeka docker: build          # Builds a Docker image for the project
jeka docker: info           # Displays info about generated image.
jeka docker: buildNative    # Builds a Docker image containing the native executable
jeka docker: infoNative     # Displays info about generated native image 
```

``` title="From *maven* KBean"
jeka maven: publishLocal      # Publish artifacts in the local repository
jeka maven: publish           # Publish in the defined Maven repository
```

## Perform static analysis using SonarQube and JaCoCo

Jacoco and Sonarqube are not included in the Jeka distribution but are available as separate plugins.  
To use them, we need to add them to the classpath from Maven Central.

!!! note
    We don't need to specify the plugin versions because Java can automatically select the correct one.

```properties title="jeka.properties"
jeka.inject.classpath=dev.jeka:sonarqube-plugin dev.jeka:jacoco-plugin
@jacoco=

jeka.cmd.pack-quality=project: pack sonarqube: run

sonar.host.url=http://localhost:9000
```

- `@jacoco=` initializes Jacoco KBean to run be run when project test willbe run.
- `sonar.host.url=http://localhost:9000` SonarQube can be configured simply by setting the `sonar.*`properties.

Build and run the project by running: `jeka project:pack sonarqube:run`

## Configure Programmatically
By configuring the project programmatically, you gain full control: adding logic, extra tasks, or detailed configurations.  
The configuration can depend on any Java library by specifying its coordinates in a `@JkDep` annotation.

!!! info
    Programmatic configuration complements properties-bases configuration, rather than replaces it. Properties configuration 
    are still effective until it is overridden by programmatic configuration.

```java title="jeka-src/Build.java"
@JkDep("org.apache.pdfbox:pdfbox:3.0.3")
class Build extends KBean {

    @JkDoc("If true, the generated doc will include PDF documents")
    public boolean includePdfDoc;
    
    @JkDoc("If true, the produced jar will include a JDBC driver")
    public boolean includeJdbcDriver;

    private final JkProject project = load(ProjectKBean.class).project;

    @Override
    protected void init() {
        var publication = load(MavenKBean.class).getMavenPublication();
        publication.putArtifact(JkArtifactId.of("doc", "zip"), this::generateDoc);
        if (includeJdbcDriver){
            project.packaging.runtimeDependencies.add("org.postgresql:postgresql:42.7.4");
        }
    }

    @JkDoc("Performs...")
    public void extraAction() {
        // Perform an arbitrary action
    }

    private void generateDoc(File targetZipFile) {
        Path docDir = getBaseDir().resolve("doc");
        if (includePdfDoc) {
            // Create docs using pdf library...
        }
        JkPathTree.of(docDir).zipTo(targetZipFile);
    }
    
}
```

The `init()` method is designed for configuring the `JkProject` instance.

The command `jeka extraAction includePdfDoc=true project: pack maven: publish` does:

- Executes the `extraAction` method.
- Create jar.
- Publish *javadoc*, *sources*, *binary* jars along the *doc* zip file containing PDF docs.
  The *javadoc* and *sources* artifacts are automatically generated before publishing, if not already present.  
  The *doc* artifact is also created during publication, as it is explicitly registered.

!!! note
    The `JkProject` class represents everything needed to build a JVM project. It i does:s a large object model that includes both configuration and methods to *build* the project. [See Reference](../reference/api-project.md)
 