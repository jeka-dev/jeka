## KBeans

_KBean_ is the central concept of the execution engine. _KBeans_ are classes with declared executable methods. 
There is only one _KBean_ instance by _KBean_ class in any given Jeka project (JkRuntime).

_KBean_ classes share the following characteristics :

* They extend `KBean` class.
* They may declare `public void` methods without taking any arguments. All these methods can be invoked from the command line.
* They may declare `public` fields _(aka KBean properties)_. These field values can be injected from the command line.
  These can also be non-public fields annotated with `@JkDoc`.
* They must provide a no-arg constructor
* They may override `init()` method
* They must be instantiated by the execution engine and not from the user code. 

### Simple Example

The following KBeans expose the `cleanPublish` method which delegates the creation of jar files to the 'project' KBean.
`ProjectKBean` is available on the Jeka classpath as it is part of the standard KBeans bundled in the JeKa distribution.

```Java
import dev.jeka.core.api.project.JkProject;

@JkDoc("A simple example to illustrate KBean concept.")
public class SimpleJkBean extends KBean {

    final ProjectKBean projectKBean = load(ProjectKBean.class);  // Instantiate KBean or return singleton instance.

    @Override  
    protected void init() {  // When init() is invoked, projectKBean field instances has already been injected.
        projectKBean.project.simpleFacade()
                .addCompileDeps(
                        "com.google.guava:guava:30.0-jre",
                        "com.sun.jersey:jersey-server:1.19.4"
                )
                .addTestDeps(
                        "org.junit.jupiter:junit-jupiter:5.8.1"
                );
    }

    @JkDoc("Clean, compile, test, create jar files, and publish them.")
    public void cleanPublish() {
        projectKBean.cleanPack();
        projectKBean.publishLocal();
    }
    
}

```

### KBean Commands

A _KBean command_ is an instance method of a KBean class that can be invoked from the command line. In order to be considered as a _command_, a method must :

* be `public`
* be an instance method
* take no argument
* return `void`

### KBean Attributes

A _KBean attribute_ is a `public` instance field of a KBean class. Its value can be injected from the command line, 
or from property file. 
It can be also non-public fields annotated with `@JkDoc`.

Attributes can be annotated with `@JkInjectProperty("my.prop.name")` to inject the value of a _property_ within.

For more details on field accepted types, see `dev.jeka.core.tool.FieldInjector#parse` [method](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/FieldInjector.java).

_KBean properties_ can also be nested composite objects, see example in `ProjectKBean#pack` [field](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/project/ProjectKBean.java).

### Naming KBeans

In order to be referenced conveniently, _KBeans_ can be called by names. For any given _JkBean_ class, accepted names are :

1. Full qualified class name
2. Uncapitalized simple class name (e.g. 'myBuild' matches 'org.example.MyBuild')
3. Uncapitalized simple class Name without 'JkBean' suffix (.g. 'project' matches 'dev.jeka.core.tool.builtin.project.ProjectKBean')

!!! tip
    Execute `jeka`, at the root of a project to display _KBeans_ present in _Jeka classpath_.

### Document KBeans 

_KBean_ classes, methods, and properties can be annotated with `@JkDoc` annotation in order to provide self documentation.

Text within these annotations is displayed when invoking `help` method on the console.

### Invoke KBeans 

#### From Command Line

KBean methods can be invoked from the command line using 

`jeka [kbeanName]#methoName [kbeanName]#[propertyName]=xxx`  

Many methods/properties can be invoked in a single command line.

!!! info
    _[kbeanName]_ prefix can be omitted. By default, it will be resolved on the bean mentioned by the *-kb=* option, 
    or the first KBean found in _def_ dir, if the option is not present. 
    Search is executed by alphabetical order of fully qualified class names. 
    Example : `jeka #toSomething #aProperty=xxxx`
    It is also possible to refer to the default KBean by using *kb#* prefix in place of *#*.
    Example : `jeka kb#toSomething kb#aProperty=xxxx`


#### From IntelliJ Jeka Plugin

[IntelliJ Jeka Plugin] allows the invocation of methods directly from the editor or the explorer tool window.

#### From naked IDE

_KBean_ methods can also be launched/debugged from IDE using classic `main` methods.

In _KBean_ class, declare one or several main methods as :

```Java
 public static void main(String[] args) {
        JkInit.instanceOf(MyBuild.class, args).cleanPack();
    }

  public static class Release {
      public static void main(String[] args) {
          JkInit.instanceOf(MyBuild.class, args, "-runIT").release();
      }
  }
```
_KBean_ must be instantiated using `JkInit#instanceOf` in order for it to be set up properly. 

The arguments passed in the `main` method are interpreted as command line arguments.

Launching or debugging this way is performant as all build classes and their dependencies are already on classpath. Therefore, no compilation or dependency resolution is needed.


!!! warning
    Be careful to launch the _main_ method using _module dir_ as _working dir_. On _IntelliJ_, this is not the default (it uses _project dir_).

    To change _intelliJ_ defaults, follow : **Edit Configurations | Edit configuration templates... |  Application | Working Directory : $MODULE_DIR$**.


Sometimes, you may need to mimic the command line behavior more closely, for debugging purposes or to pass '@' arguments.

* Create an IDE launcher for a Java Application
* Set `dev.jeka.tool.Main` as Java main class.
* Set the same command line arguments as you would do for invoking from command line (Do not include _jeka_ command).


### Let KBeans cooperate

Generally _KBeans_ interact with each other by declaring KBeans using `KBean#load(MyBean.class)` method as shown in [this example](#simple-example).

When a _KBean_ depends on another one, it's good to declare it as an instance field of the first bean, as this 
dependency will be mentioned in the auto-generated documentation and showed explicitly in IDE tool.

### KBeans in Multi-Projects

In a multi-project, it's quite common that a _KBean_ accesses a _KBean_ instance from another project. 
You can achieve this in a statically typed way.

* In _master_ _KBean_, declare a field of type `KBean` (e.g. ´JkBean importedBuild;`). It doesn't have to be public.
* Annotate it with `@JkInjectProject` mentioning the relative path of the imported project (e.g. `@JkInjectProject("../anotherModule")).
* Execute `jeka intellij#iml` or `jeka eclipse#files`.
* Redefine the declared type from `KBean` to the concrete type of imported _KBean_
* Now, master _KBean_ can access the imported _KBean_ in a static typed way.
* See example [here](https://github.com/jerkar/jeka/blob/master/dev.jeka.master/jeka/def/MasterBuild.java).
* Be mindful that the imported _KBean_ must deal with file paths using `KBean#getBaseDir` in order for it to be safely executed from any working directory.

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

#### scaffold

`ScaffoldjKBean` provides methods for project directories and files to create a new _Jeka_ project. Basically, 
it creates a project ready to create vanilla automation tasks.

This _KBean_ offers an extension point in order for another KBean to augment the scaffolded structure. For example, 
executing `jeka scaffold#run` will create a basic Jeka project while `jeka scaffold#run project#` will create 
a project ready to build a JVM-based project. 

#### git

`GitKBean` exposes some common git command combos. It can also auto-inject a version inferred from Git into *project* KBean.
