
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

The following KBeans expose the `hello` and `bye` methods. The rendering can be configured 
through `nema` and `uppercase` attributes.

```Java
import dev.jeka.core.tool.JkDoc;

@JkDoc("Displays greeting messages")
public class Greeting extends KBean {

    public String name = "Bob";

    @JkDoc("If true, the message is shown in upper case.")
    public boolean uppercase;

    @JkDoc("Prints a hello message.")
    public void hello() {
        System.out.println(formatMessage("Hello " + name + "!"));
    }
    
    public void bye() {
        System.out.println(formatMessage("Goodbye " + name + "!"));
    }

    private String formatMessage(String message) {
        return uppercase ? message.toUpperCase() : message;
    }
}
```
To execute a method from the command line, run the following example:
```bash
jeka hello name=Alice uppercase=true
```
To show help for this KBean, run:
```bash
jeka greeting: --doc
```
This will display:
```text
Displays greeting messages.

Fields
      name=<String>   No description.
                        Default: Bob
      uppercase       If true, the message is shown in upper case.
                        Default: false
Methods
  bye    No Description.
  hello  Prints a hello message.
```

## Location

KBeans can exists as source code in the local project *jeka-src* folder, at root or any package,  or 
as class in the Jeka classpath.

**Multiple KBeans in jeka-src**

Many KBeans may coexist in a single *jeka-src* dir. In this case, use KBean names to precise on 
which bean to invoke, as:

```bash
jeka greeting: hello bye other: foo
```
In the above example, three methods coming from 2 distinct KBean are invoked.

**Classpath KBeans**

Jeka bundles a collection of KBeans for building projects, creating Docker images, performing Git operations, and more.

For example, running:
```bash
jeka project: compile
```
will compile the source code located in the *src/main/java* directory, using dependencies specified in the *dependencies.txt* file.

To display the documentation for the `project` KBean, run:
```bash
jeka project: --doc
```

To list all available KBeans in the classpath, execute:
```bash
jeka --doc
```

KBeans can be added to the classpath like any third-party dependency.  
This can be done by setting the `jeka.inject.classpath` property in the *jeka.properties* file as follows:
```properties
jeka.inject.classpath=dev.jeka:springboot-plugin   dev.jeka:openapi-plugin:0.11.8-1
```

KBeans can also be included directly in the source code using the `@JkDep` annotation:
```java
import dev.jeka.core.tool.JkDep;

@JkDep("dev.jeka:springboot-plugin")
@JkDep("dev.jeka:openapi-plugin:0.11.8-1")
class Build extends KBean {
...
```

Additionally, KBeans can be dynamically added from the command line like this:
```bash
jeka --classpath=dev.jeka:openapi-plugin:0.11.8-1 openapi:--doc
```

Jeka discovers KBeans automatically by scanning the classpath.

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

- The KBean specified by the `jeka.kbean.default` property (if this property is set).
- If the property is not set, it defaults to the first KBean found in the _jeka-src_ directory, sorted alphabetically by fully qualified class name.

### Example
The following command:
```
jeka doSomething aProperty=xxxx
```  
executes the `doSomething` method of the default KBean.

To explicitly reference the default KBean and avoid ambiguity, use `:` as the prefix.

### Examples
- `jeka : --doc` displays the documentation of the default KBean.
- `jeka --doc` displays the overall documentation.


## KBean Collaboration

There's 2 goals for making KBean collaboration:

1. Invoke a KBean from another one
2. Configure a KBean from another one

### Invoke KBean from another one

#### Using @JkInject

```Java 
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;
import dev.jeka.core.tool.JkInject;

@JkDoc("A simple example to illustrate KBean concept.")
public class Build extends KBean {

    @JkInject
    ProjectKBean projectKBean;
    
    @JkInject
    MavenKBean mavenKBean;

    @JkDoc("Clean, compile, test, create jar files, and publish them.")
    public void packPublish() {
        projectKBean.clean();
        projectKBean.pack();
        mavenKBean.publishLocal();
    }

}
```
Both `ProjectKBean` and `MavenKBean` are created and injected into the `Build` KBean during initialization.  

For multi-module projects, use `JkInject` to access sub-module KBeans.

```java
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;
import dev.jeka.core.tool.JkInject;

import java.util.List;

@JkDoc("A simple example to illustrate KBean concept.")
public class MasterBuild extends KBean {

    @JkInject("./foo")
    ProjectKBean fooProject;

    @JkInject("./bar")
    ProjectKBean barProject;

    @JkDoc("For all sub-modules: clean, compile, test, create jar files, and publish them.")
    public void buildAll() {
        List.of(fooProject, barProject).forEach(projectKbean -> {
            projectKbean.clean();
            projectKbean.pack();
            MavenKBean mavenKBean = projectKbean.load(MavenKBean.class);
            mavenKBean.publishLocal();
        });
    }
}
```
In this example, JeKa initializes KBeans from the sub-modules *./foo* and *./bar*, then injects them into the `MasterBuild` KBean.

We can create or load a KBean on the fly using the `KBean#load` method. 
This means we only need to declare one KBean per sub-module. 

Another option is to inject `JkRunbase` and make all calls through it:

```java
import dev.jeka.core.tool.JkRunbase;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import dev.jeka.core.tool.builtins.tooling.maven.MavenKBean;
import dev.jeka.core.tool.JkInject;

import java.util.List;

@JkDoc("A simple example to illustrate KBean concept.")
public class MasterBuild extends KBean {

    @JkInject("./foo")
    JkRunbase foo;

    @JkInject("./bar")
    JkRunbase bar;

    @JkDoc("For all sun-modules: clean, compile, test, create jar files, and publish them.")
    public void buildAll() {
        List.of(foo, bar).forEach(runbase -> {
            ProjectKBean projectKBean = runbase.load(ProjectKBean.class);
            projectKbean.clean();
            projectKbean.pack();
            MavenKBean mavenKBean = runbase.load(MavenKBean.class);
            mavenKBean.publishLocal();
        });
    }
}
```

For larger sub-project structures, use `KBean#getImportedKBeans()` to list all sub-modules, either recursively or not.

```java
@JkDoc("For all sub-modules: compile, test, create jar files, and publish them.")
public void buildAll() {
    this.getImportedKBeans().get(ProjectKBean.class, true).forEach(ProjectKBean::pack);
    this.getImportedKBeans().get(MavenKBean.class, true).forEach(MavenKBean::publish);
}
```

#### Using #load and #find methods

As we saw earlier, you can dynamically retrieve a `KBean` using the `KBean#load(Class)` method.  
This method forces the initialization of the `KBean` if it is not already present.  
It is useful when you need a specific `KBean` only within certain methods.

```java
public void createNativeExec() {
    load(NativeKBean.class).compile();
}

```

On the other hand, the `KBean#find(Class)` method returns an `Optional<? extends KBean>`,  
which is empty if the specified `KBean` is not initialized.  
This is helpful for performing conditional tasks based on the presence of a `KBean`.

```java
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;

public void cleanup() {
    find(DockerKBean.class).ifPresent(dockerKBean -> {
        // Do something
    });
}
```

### Configure Kbean from another one.

Wether t-you want to create a JeKa extension or just configure a build, the technic is 
the same: create a KBean and configure an existing one.

For example, to configure a build, you can create a Build class as:

```java
class Build extends KBean {

    public boolean skipIT;

    @JkPostInit
    private void postInit(ProjectKBean projectKBean) {
        JkProject project = projectKBean.project;
        project.flatFacade.dependencies.compile
                .add("com.google.guava:guava:33.3.1-jre")
                .add("org.openjfx:javafx-base:21");
        project.flatFacade.dependencies.test
                .add("org.junit.jupiter:junit-jupiter:5.8.1");
        project.flatFacade
                .addTestExcludeFilterSuffixedBy("IT", skipIT);
    }

    @JkPostInit
    private void postInit(MavenKBean mavenKBean) {
        
        // Customize the published pom dependencies
        mavenKBean.getMavenPublication().customizeDependencies(deps -> deps
                .withTransitivity("com.google.guava:guava", JkTransitivity.RUNTIME)
                .minus("org.openjfx:javafx")
        );
    }

}
```
This KBean defines a `Build` class that customizes the `project` and `maven` KBeans.  
The `postInit` methods are invoked only if their respective KBean is present.

For example, when executing `jeka project: pack`, the `ProjectKBean` will be initialized with the settings provided by 
command-line arguments and `@project...=` properties defined in the *jeka.properties* file. 
The instance will then be passed to the `postInit` method before invoking the `pack` method.

When executing `jeka maven: publish`, the `project` KBean will be implicitly loaded and configured, 
followed by the same process for the `maven` KBean, before invoking the `publish` method.

The reason the `maven` KBean implicitly loads the `project` KBean is that this behavior
is declared in `MavenKBean#init` method as:

```java
public class MavenKBean extends KBean {
    
    @Override
    protected void init() {
        requireBuildable();  //  load `project` or `base` kbean if not present
    }
    
}
```

## Lifecycle

Before Kbean methods are executed, Kbeans are configured as described in the detailed sequence:

```mermaid
sequenceDiagram
    participant RB as Run Base
    participant IN as Initializer
    participant CI as KBean classes to init
    participant PR as Pre-initializer
    participant PO as Post-initializer
    participant KB as KBean
    participant RK as Registered KBeans

    RB->>CI:  Add initializer KBean
    RB->>CI:  Add KBean classes declared in properties and cmd-line
    RB->>CI:  Add classes declared via @JkInject 
    RB->>IN:  Register pre-init methods
    IN->>CI:  Get class to init
    IN->>PR:  For each class to init
    PR->>PR:  Discover transitive pre-initializer classes
    PR->>PR:  Register Pre-initializer methods
    PR-->IN:  
    IN-->RB:  
    RB->>IN:  For each class to init
    IN->>IN:  Setup current base directory
    IN->>KB:  new
    KB->>KB:  Capture the current base dir
    KB->>KB:  Load KBeans imported from other Runbase via @JkInject('./...')
    IN->>RK:  Register the kbean singleton
    IN->>PR:  Pre-initialize the singleton
    IN->>KB:  Inject properties (e.g. @project.version=0.1)
    IN->>KB:  Inject command-line values  (e.g. project: version=0.1)
    IN->>KB:  Invoke init()
    KB->>KB:  Specific KBean initialisation code (as declaring required classes)
    KB->>CI:  Register required classes
    KB->>PO:  Register required classes
    KB-->>IN:  
    IN->>PO:  Register initialized class
    IN-->RB: 
    RB->>IN:  Post initialize
    IN->>RK:  Get all registered KBeans
    IN->>PO:  Post initialize each registered KBean
    PO->>PO:  Apply registered methods to provided KBean
    PO-->IN:  
    IN-->RB:  
```
The sequence is split in 3 main phases:

### Discover

Determine which Kbean classes to initialize, according to KBean presents in *jeka-src*, command-line and properties.

If some KBean classes declare fields annotated with `@JkInject` (without referencing another basr dir),  
the field classes are included in the classes to init.

Once identified, classes are inspected to find declared *pre-init* methods. Collected methods are 
registered for a later usage.

### Initialisation

Classes to initialise are instantiated the initialized one by one, as follow:

- KBean class is instantiated. KBean parent constructor captures the current 
  base directory in its internal context for later usage. 
  Fields annotated with `@JkInject("./...")` are injected.
- The newly created instance is passed as argument of all *pre-init* methods collected in the previous phase.
- Values coming from *properties* are injected in the KBean fields.
- Values coming from the command-line are injected in the KBean fields.
- Field annotated with raw `@JkInject` are injected
- The `init()` method is invoked. This method is mainly used to declare require KBeans, or do initial setup.
  If `require(Class)` is invoked during the `init`method, those classes 
  are added to the initialization loop, and their post-initializer methods are registered.
- The kbean post-initializer methods are registered. Note that they are registered after the required classes.

### Post initialisation.

One all the KBean has been initialized, they maybe 


```mermaid
sequenceDiagram
    participant EE as Execution Engine or user code
    participant RB as Run Base
    participant KB as KBean
    participant OKB as Other KBeans

    EE->>RB:  Load KBean
    RB->>RB:  Find if singleton already exists
    RB-->>EE: Return the singleton if found
    RB->>KB:  new
    KB->>KB:  Setup current base directory
    KB-->>RB: 
    RB->>RB:  Register Singleton
    RB->>RB:  Consume the singleton by the @JkPreInitKBean methods.
    RB->>KB:  Inject properties (e.g. -D@project.version=0.1 or @project.moduleId=ac.me:foo)
    RB->>KB:  Inject command-line values  (e.g. project: version=0.1)
    RB->>KB:  Invoke init() method
    KB->>KB:  Specific KBean initialisation code (e.g. may be nothing)
    KB-->> OKB: May load or call other KBans
    KB-->>RB: 
    RB-->>EE: 

```

```mermaid
classDiagram
    class JkRunBase {
        +Path baseDir
        +KBean initKBean
        +KBean defaultKBean
        +JkProperties properties
        +List dependencies
  
        +KBean load()
        +KBean find()
        +List getKBeans()
        
    }

    class KBean {
        +JkRunbase runbase
    }

    JkRunBase "1" <--> "0..*" KBean
    JkRunBase --> "0..*" BaseDir: Imported Base Dirs (multi-modules)

    note for JkRunBase "There is only one JkRunBase per base folder.<br/>The base folder is the project root.<br/>In multi-module projects, usually one JkRunBase exists per module."
    note for BaseDir "This class doesn’t exist. It represents the base directory <br/>of another runbase in a multi-module project."

```



## Multi-Project setup

In multi-project scenarios, it is common for a _KBean_ in one project to access a _KBean_ instance from another project. This can be achieved in a statically typed manner:

1. In the **master** _KBean_, declare a field of type `KBean` (e.g., `KBean importedBuild;`). This field does not need to be public.
2. Annotate the field, by specifying the relative path of the imported project (e.g., `@JkInjectRunbase("../anotherModule")`).
3. Run the command `jeka intellij: iml` or `jeka eclipse: files` to refresh project metadata.
4. Change the declared field type from `KBean` to the concrete type of the imported _KBean_.
5. The master _KBean_ can now access the imported _KBean_ in a type-safe manner.
6. For an example, see [this implementation](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.master/jeka-src/MasterBuild.java).

!!! tip
    Ensure that the imported _KBean_ uses `KBean#getBaseDir` for handling file paths. This practice ensures safe execution from any working directory.

