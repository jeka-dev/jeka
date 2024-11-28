## KBeans

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

### Simple Example

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

### KBean Methods

A _KBean method_ is a specific method defined in a KBean class, designed to be executable from the command line interface. For successful recognition as a _command_, the method must adhere to the following criteria:

* It must be designated as `public`.
* It must be an instance method, not static or abstract.
* It must not require any arguments upon invocation.
* It must not return any value, as indicated by a `void` return type.

### KBean Attributes

A _KBean attribute_ is a `public` instance field of a KBean class. Its value can be injected from the command line or from a property file.  
Additionally, it can be a non-public field annotated with `@JkDoc`.

Attributes can be annotated with `@JkInjectProperty("my.prop.name")` to inject the value of a _property_ into the field.

For more details on field accepted types, see the `dev.jeka.core.tool.FieldInjector#parse` [method](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/FieldInjector.java).

_KBean attributes_ can also represent nested composite objects. See the example in the `ProjectKBean#pack` [field](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/project/ProjectKBean.java).

### Naming KBeans

To be referenced conveniently, _KBeans_ can be identified by specific names. For any given _KBean_ class, the accepted names are:

1. Fully qualified class name.
2. Uncapitalized simple class name (e.g., `myBuild` matches `org.example.MyBuild`).
3. Uncapitalized simple class name without the `KBean` suffix (e.g., `project` matches `dev.jeka.core.tool.builtin.project.ProjectKBean`).

!!! tip
    Execute `jeka` at the root of a project to display the _KBeans_ available on the _Jeka classpath_.

### Document KBeans

_KBean_ classes, methods, and attributes can be annotated with the `@JkDoc` annotation to provide self-documentation.  
The text provided in these annotations is displayed when running the command:  
`jeka <kbeanName>: --doc`

### Invoke KBeans

#### From the Command Line

_KBean_ methods can be executed directly from the command line using the syntax:

`jeka <kbeanName>: [methodName...] [attributeName=xxx...]`

**Example:** `jeka project: info pack tests.fork=false pack.jarType=FAT jacoco: sonarqube: run`

You can call multiple methods and set multiple attributes in a single command.

!!! info
    The _[kbeanName]_ prefix is optional. By default, it resolves to:
      - The bean specified by the `jeka.default.kbean` properties if any.
      - Or the first KBean found in the _jeka-src_ directory (sorted alphabetically by fully qualified class names).

    **Examples:**
    `jeka doSomething aProperty=xxxx`  
    `jeka : doSomething aProperty=xxxx`  

The ` : ` symbol explicitly refers to the default KBean.

#### From IntelliJ Jeka Plugin

The [IntelliJ Jeka Plugin](https://plugins.jetbrains.com/plugin/24505-jeka) enables invoking KBean methods directly from the IDE, 
either from the code editor or the project explorer tool window.

#### From a Plain IDE Setup

_KBean_ methods can also be launched or debugged in an IDE by invoking the `dev.jeka.core.tool.Main` method and passing the corresponding command-line arguments.

**Example:**  
Invoking the `dev.jeka.core.tool.Main` method with arguments `project:` and `compile` will instantiate the `ProjectKBean` class and invoke its `compile` method.

!!! warning
    Ensure that the _main_ method is launched with the **module directory** set as the **working directory**.  
    In IntelliJ, the default working directory is the _project directory_, which may cause issues.

    To update IntelliJ defaults:  
    - Navigate to **Run | Edit Configurations... | Application | Working Directory**  
    - Set the value to `$MODULE_DIR$`.


### Let KBeans Cooperate

_KBeans_ can interact with one another by declaring dependencies using the `KBean#load(MyBean.class)` method, as shown in the [simple example](#simple-example).

Alternatively, you can use the `KBean#find(MyKBean.class)` method, which returns an `Optional<KBean>` containing the instance only if it already exists in the context.

When a _KBean_ depends on another, it is best practice to declare the dependency as an instance field in the dependent _KBean_. This approach has several benefits:
- The dependency is explicitly documented in the auto-generated documentation.
- It is visible in IDE tools, making the relationship clear.


### KBeans in Multi-Project Setups

In multi-project scenarios, it is common for a _KBean_ in one project to access a _KBean_ instance from another project. This can be achieved in a statically typed manner:

1. In the **master** _KBean_, declare a field of type `KBean` (e.g., `KBean importedBuild;`). This field does not need to be public.
2. Annotate the field, by specifying the relative path of the imported project (e.g., `@JkInjectRunbase("../anotherModule")`).
3. Run the command `jeka intellij: iml` or `jeka eclipse: files` to refresh project metadata.
4. Change the declared field type from `KBean` to the concrete type of the imported _KBean_.
5. The master _KBean_ can now access the imported _KBean_ in a type-safe manner.
6. For an example, see [this implementation](https://github.com/jerkar/jeka/blob/master/dev.jeka.master/jeka/def/MasterBuild.java).

!!! tip
    Ensure that the imported _KBean_ uses `KBean#getBaseDir` for handling file paths. This practice ensures safe execution from any working directory.

### Bundled KBeans

There are a bunch of _KBeans_ bundled within _Jeka_. Those _KBeans_ are always present.

#### project

`ProjectKBean` provides a wrapper around a `JkProject` for building JVM-based projects. This _KBean_ initialises 
a default sensitive project object and provides a classic method for building a project (compile, package in jar, publish ...)

This _KBean_ proposes an extension point through its `configure(JkProject)` method. This way, other _KBeans_ can 
modify the properties of the project to be built.

#### intellij

`IntellijKBean` provides methods for generating metadata files for _IntelliJ_ IDE. The content of an _iml_ file is computed 
according the `JkProject` object found in _project KBean_.

This _KBean_ proposes an extension point through its `configure` methods in order to modify the resulting iml
(e.g. using a module dependency instead of a library dependency).


#### git

`GitKBean` exposes some common git command combos. It can also auto-inject a version inferred from Git into *project* KBean.
