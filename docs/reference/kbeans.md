
_KBean_ is the central concept of the execution engine. _KBeans_ are classes with declared executable methods.  
There is only one _KBean_ instance per _KBean_ class in any given Jeka base directory.

_KBean_ classes share the following characteristics:

* They extend the `KBean` class.
* They may declare `public void` methods without arguments. All these methods can be invoked from the command line.
* They may declare `public` fields _(also known as KBean properties)_. These field values can be injected from the command line.  
  Additionally, they can have non-public fields annotated with `@JkDoc`.
* They must provide a no-argument constructor.
* They may override the `init()` method.
* They must be instantiated by the execution engine and not by user code.

## Simple Example

The following KBeans expose the `cleanPublish` method, which delegates the creation of JAR files to the `project` KBean.  
`ProjectKBean` is available on the Jeka classpath as it is part of the standard KBeans bundled in the JeKa distribution.


```Java
import dev.jeka.core.api.project.JkProject;

@JkDoc("A simple example to illustrate KBean concept.")
public class SimpleJkBean extends KBean {

    final ProjectKBean projectKBean = load(ProjectKBean.class);  // Instantiate KBean or return singleton instance.

    @Override  
    protected void init() {  // When init() is invoked, projectKBean field instances has already been injected.
        projectKBean.project.flatFacade.compileDependencies
                .add("com.google.guava:guava:30.0-jre")
                .add("com.sun.jersey:jersey-server:1.19.4");
        projectKBean.project.flatFacade.testDependencies
                .add("org.junit.jupiter:junit-jupiter:5.8.1");
    }

    @JkDoc("Clean, compile, test, create jar files, and publish them.")
    public void cleanPublish() {
        projectKBean.cleanPack();
        projectKBean.publishLocal();
    }
    
}

```

## KBean Methods

A _KBean method_ is a specific method defined in a KBean class, designed to be executable from the command line interface. For successful recognition as a _command_, the method must adhere to the following criteria:

* It must be designated as `public`.
* It must be an instance method, not static or abstract.
* It must not require any arguments upon invocation.
* It must not return any value, as indicated by a `void` return type.

## KBean Attributes

A _KBean attribute_ is a `public` instance field of a KBean class. Its value can be injected from the command line or from a property file.  
Additionally, it can be a non-public field annotated with `@JkDoc`.

Attributes can be annotated with `@JkInjectProperty("my.prop.name")` to inject the value of a _property_ into the field.

We can also inject value using *jeka.properties

For more details on field accepted types, see the `dev.jeka.core.tool.FieldInjector#parse` [method](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/FieldInjector.java).

_KBean attributes_ can also represent nested composite objects. See the example in the `ProjectKBean#pack` [field](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/project/ProjectKBean.java).



## Naming KBeans

To be referenced conveniently, _KBeans_ can be identified by specific names. For any given _KBean_ class, the accepted names are:

1. Fully qualified class name.
2. Uncapitalized simple class name (e.g., `myBuild` matches `org.example.MyBuild`).
3. Uncapitalized simple class name without the `KBean` suffix (e.g., `project` matches `dev.jeka.core.tool.builtin.project.ProjectKBean`).

!!! tip
    Execute `jeka` at the root of a project to display the _KBeans_ available on the _Jeka classpath_.

## Document KBeans

_KBean_ classes, methods, and attributes can be annotated with the `@JkDoc` annotation to provide self-documentation.  
The text provided in these annotations is displayed when running the command:  
`jeka <kbeanName>: --doc`

## Invoke KBeans

### From the Command Line

_KBean_ methods can be executed directly from the command line using the syntax:

`jeka <kbeanName>: [methodName...] [attributeName=xxx...]`

**Example:** `jeka project: info pack tests.fork=false pack.jarType=FAT jacoco: sonarqube: run`

You can call multiple methods and set multiple attributes in a single command.
    

### From IntelliJ Jeka Plugin

The [IntelliJ Jeka Plugin](https://plugins.jetbrains.com/plugin/24505-jeka) enables invoking KBean methods directly from the IDE, 
either from the code editor or the project explorer tool window.

### From a Plain IDE Setup

_KBean_ methods can also be launched or debugged in an IDE by invoking the `dev.jeka.core.tool.Main` method and passing the corresponding command-line arguments.

**Example:**  
Invoking the `dev.jeka.core.tool.Main` method with arguments `project:` and `compile` will instantiate the `ProjectKBean` class and invoke its `compile` method.

!!! warning
    Ensure that the _main_ method is launched with the **module directory** set as the **working directory**.  
    In IntelliJ, the default working directory is the _project directory_, which may cause issues.

    To update IntelliJ defaults:  
    - Navigate to **Run | Edit Configurations... | Application | Working Directory**  
    - Set the value to `$MODULE_DIR$`.


## Default KBean

The _[kbeanName]_ prefix is optional and defaults to:

- The KBean specified by the `jeka.default.kbean` property (if set).
- Otherwise, the first KBean found in the _jeka-src_ directory, ordered alphabetically by fully qualified class name.

Example: `jeka doSomething aProperty=xxxx` invokes the `doSomething` method of the default KBean.

Use `:` to explicitly reference the default KBean and avoid ambiguity.  
Example: `jeka : --doc` shows the default KBean's documentation, while `jeka --doc` displays overall documentation.


## KBean Collaboration

_KBeans_ can interact with one another by declaring dependencies using the `KBean#load(MyBean.class)` method, as shown in the [simple example](#simple-example).

Alternatively, you can use the `KBean#find(MyKBean.class)` method, which returns an `Optional<KBean>` containing the instance only if it already exists in the context.

When a _KBean_ depends on another, it is best practice to declare the dependency as an instance field in the dependent _KBean_. This approach has several benefits:
- The dependency is explicitly documented in the auto-generated documentation.
- It is visible in IDE tools, making the relationship clear.


## Multi-Project setup

In multi-project scenarios, it is common for a _KBean_ in one project to access a _KBean_ instance from another project. This can be achieved in a statically typed manner:

1. In the **master** _KBean_, declare a field of type `KBean` (e.g., `KBean importedBuild;`). This field does not need to be public.
2. Annotate the field, by specifying the relative path of the imported project (e.g., `@JkInjectRunbase("../anotherModule")`).
3. Run the command `jeka intellij: iml` or `jeka eclipse: files` to refresh project metadata.
4. Change the declared field type from `KBean` to the concrete type of the imported _KBean_.
5. The master _KBean_ can now access the imported _KBean_ in a type-safe manner.
6. For an example, see [this implementation](https://github.com/jerkar/jeka/blob/master/dev.jeka.master/jeka/def/MasterBuild.java).

!!! tip
    Ensure that the imported _KBean_ uses `KBean#getBaseDir` for handling file paths. This practice ensures safe execution from any working directory.

## Bundled KBeans

There are a bunch of _KBeans_ bundled within _Jeka_. Those _KBeans_ are always present.

### project

[`ProjectKBean`](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/project/ProjectKBean.java) 
acts as a wrapper around a [`JkProject`](api-project.md) to facilitate the building of JVM-based code hosted in a project structure.
This _KBean_ provides core methods for fundamental build tasks, including **compiling**, **testing**, and **packaging**.

To work effectively with this KBean, it's helpful to have an [overview](api-project.md) of the capabilities offered by the `JkProject` object.

**Key Features**
- Resolves dependencies, compiles code, and runs tests.
- Creates various types of JAR files out-of-the-box, including regular, fat, shaded, source, and Javadoc JARs.
- Infers project versions from Git metadata.
- Executes packaged JARs.
- Displays dependency trees and project setups.
- Scaffolds skeletons for new projects.

Additionally, `ProjectKBean` serves as a central point of interaction for other KBeans, enabling them to access project details and extend or enhance the build process.

**Example for getting information about source files:**
```Java
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

class MyBuild extends KBean {

    final JkProject project = load(ProjectKBean.class).project;
    
    private List<Path> allSourceFiles;
    
    protected void init() {
        allSourceFiles = project.compilation.layout.resolveSources().getFiles();
        ...
    }
}
```

**Example taken from JeKa:**

- [Jacoco KBean](https://github.com/jeka-dev/jeka/blob/master/plugins/dev.jeka.plugins.jacoco/src/dev/jeka/plugins/jacoco/JacocoKBean.java): 
A KBean that reads te underlying `JkProject` and modifies its testing behavior.
- [Sonarqube KBean](https://github.com/jeka-dev/jeka/blob/master/plugins/dev.jeka.plugins.sonarqube/src/dev/jeka/plugins/sonarqube/SonarqubeKBean.java):
A KBean that reads te underlying `JkProject` to extract information.
- [Protobuf KBean](https://github.com/jeka-dev/jeka/blob/master/plugins/dev.jeka.plugins.protobuf/src/dev/jeka/plugins/protobuf/ProtobufKBean.java):
  A KBean that adds a Proto-buffer code generation to the underlying `JkProject`.

### base

[`BaseKBean`](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/base/BaseKBean.java), similar to `ProjectKBean`, facilitates building JVM-based code hosted entirely in the *jeka-src* folder with a simpler classpath organization.

- **Single Classpath**: By default, there is a single classpath. However, if a `_dev` package exists in the code structure, its contents are excluded when creating JARs, native executables, or Docker images. Typically, build and test classes are placed in `_dev` for application builds.
- **Dependency Declaration**: Dependencies are declared by annotating any class with the `@JkDep` annotation. Dependencies within the `_dev` package are excluded from production artifacts.

**Key Features**

- Resolves dependencies, compiles code, and runs tests.
- Creates various types of JAR files out-of-the-box: regular, fat, shaded, source, and Javadoc JARs.
- Infers project versions from Git metadata.
- Executes packaged JARs.
- Displays dependency trees and project setups.
- Scaffolds skeletons for new projects.

**Example**

- [Base Application](https://github.com/jeka-dev/demo-base-application): The `BaseKBean` is set as the default KBean in `jeka.properties`. The accompanying `README.md` file details the available `base:` methods that can be invoked.

### native

[`NativeKBean`](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/tooling/nativ/NativeKBean.java) enables native compilation for *project* and *base* KBeans.

**Key Features**

- Compiles classes into native executables.
- Automatically applies AOT metadata.
- Simplifies resource inclusion.
- Handles static linkage with minimal configuration.

**Example of Configuration in jeka.properties:**
```properties
@native.includeAllResources=true
@native.staticLink=MUSL
@native.metadataRepoVersion=0.10.3
```

Invocation: `jeka native: compile`

### docker

[`DockerKBean`](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/tooling/docker/DockerKBean.java) allows the creation of Docker images for both *project* and *base* KBeans. It supports generating JVM-based images as well as minimalist Docker images containing only the native executable.

**Key Features:**

- Efficiently create layered and secure Docker images for JVM applications
- Generate secure, optimized Docker images for native applications
- Infer image name/version from the project
- Optionally switch to a non-root user (configurable)
- Customize the generated image via Java API

**Example Invocation:**
- `jeka docker:buildNative`: Builds a native Docker image of your application.

**Example Configuration:**
```properties
@docker.nativeBaseImage=gcr.io/distroless/static-debian12:nonroot
```

**Example For Image customization:**
```java
protected void init() {
    load(DockerKBean.class).customizeJvmImage(dockerBuild -> dockerBuild
            .addAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.32.0", "")
            .setBaseImage("eclipse-temurin:21.0.1_12-jre-jammy")
            .setAddUserTemplate(JkDockerBuild.TEMURIN_ADD_USER_TEMPLATE)
            .nonRootSteps   // inserted after  USER nonroot
                .addCopy(Paths.get("jeka-output/release-note.md"), "/release.md")
                .add("RUN chmod a+rw /release.md ")
    );
}
```
This KBean allows customizing the Docker image programmatically using the [Jeka libs for Docker](api-docker.md).

It’s easy to see the customization result by executing `jeka docker: info`. 
This will display details about the built image, including the generated Dockerfile. 
You can also visit the generated Docker build directory, 
which contains all the Docker context needed to build the image with a Docker client.

### maven 

[`MavenKBean`](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/tooling/maven/MavenKBean.java) provides ability to publish artifacts on a 
Maven repository. The artifacts are those produces by *project* or *base* Kbeans. 
It also provides convenient mean to migrate from Maven prajects.

**Key Features:**

- publish on local or remote repositories, artifacts produced by projects
- display info about publication, especially transitive dependencies published along the atifacts
- property or programmatic configuration for published POM metadata and dependencies
- property or programmatic configuration for publication repository

### git

[`GitKBean`](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/tooling/git/GitKBean.java) provides convenient git command combos such as:

- Displaying a list of commit messages since the last tag.
- Pushing remote tags with guards to ensure the local workspace is clean.

### intellij

[`IntellijKBean`](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/tooling/ide/IntellijKBean.java) provides methods for generating metadata files for _IntelliJ_ IDE. 
The content of an _iml_ file is computed according the `JkBuildable` object found in found in the base directory.

This _KBean_ proposes methods to customize generated *iml* file.

```java title="Configuration in a Build.java class"
@Override
protected void init() {
    load(IntellijKBean.class)
            .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core")
            .setModuleAttributes("dev.jeka.core", JkIml.Scope.COMPILE, null);

```

### admin

Provides convenient methods to perform global configuration tasks as editing *global.properties* file or updating 
embedded jeka boot scripts.




