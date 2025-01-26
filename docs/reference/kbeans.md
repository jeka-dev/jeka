
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

- The KBean specified by the `jeka.kbean.default` property (if set).
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

## Lifecycle

This diagram shows how KBean instances are created or retrieved.
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

