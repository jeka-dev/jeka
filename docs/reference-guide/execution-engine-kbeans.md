## KBeans

_KBean_ is the central concept of the execution engine. _KBeans_ are classes with declared executable methods. 
There is only one _KBean_ instance by _KBean_ class in any given Jeka project.

_KBean_ classes share the following characteristics :

* They extend `JkBean`
* They may declare `public void` methods without taking any arguments. All these methods can be invoked from the command line.
* They may declare `public` fields _(aka KBean properties)_. These field values can be injected from the command line.
* They must be instantiated by the execution engine and not from the user code. 

### Simple Example

The following KBeans expose the `cleanPublish` method which delegates the creation of jar files to the 'project' KBean.
`ProjectJkBean` is available on the Jeka classpath as it is part of the standard KBeans bundled in the Jeka distribution.

The _configure_ method will be invoked at the first `ProjectJkBean#getProject()` call.

```Java
import dev.jeka.core.api.project.JkProject;

@JkDoc("A simple example to illustrate KBean concept.")
public class SimpleJkBean extends JkBean {

    ProjectJkBean projectBean = getBean(ProjectJkBean.class).configure(this::configure);

    @JkDoc("Version of junit-jupiter to use for compiling and running tests")
    public String junitVersion = "5.8.1";

    private void configure(JkProject project) {
        project.simpleFacade()
                .configureCompileDeps(deps -> deps
                        .and("com.google.guava:guava:30.0-jre")
                        .and("com.sun.jersey:jersey-server:1.19.4")
                )
                .configureTestDeps(deps -> deps
                        .and("org.junit.jupiter:junit-jupiter:" + junitVersion)
                );
    }

    @JkDoc("Clean, compile, test and create jar files.")
    public void cleanPack() {
        clean();
        projectBean.pack();
    }

    // The main method is only here for convenience in order to execute conveniently Jeka within the IDE
    // If you use IntelliJ plugin, you won't need it.
    public static void main(String[] args) {
        JkInit.instanceOf(SimpleProjectJkBean.class, args).cleanPublish();
    }


}

```

### KBean Commands

A _KBean command_ is an instance method of a KBean class that can be invoked from the command line. In order to be considered as a _command_, a method must :

* be `public`
* be an instance method
* take no argument
* return `void`

### KBean Properties

A _KBean property_ is a `public` instance field of a KBean class. Its value can be injected from the command line.

Fields can be annotated with `@JkInjectProperty("my.prop.name")` to inject the value of a _property_ within.

For more details on field accepted types, see `dev.jeka.core.tool.FieldInjector#parse` [method](https://github.com/jerkar/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/FieldInjector.java).

_KBean properties_ can also be nested composite objects, see example in `ProjectJkBean#pack` [field](https://github.com/jerkar/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/project/ProjectJkBean.java).


### Naming KBeans

In order to be referenced conveniently, _KBeans_ can be called by names. For any given _JkBean_ class, accepted names are :

1. Full qualified class name
2. Uncapitalized simple class name (e.g. 'myBuild' matches 'org.example.MyBuild')
3. Uncapitalized simple class Name without 'JkBean' suffix (.g. 'project' matches 'dev.jeka.core.tool.builtin.project.ProjectJkBean')

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
    _[kbeanName]_ prefix can be omitted. By default, it will be resolved on the first KBean found in _def_ dir. 
    Search is executed by alphabetical order of fully qualified class names. 
    Example : `jeka #toSomething #aProperty=xxxx`
    
    
    

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

Generally _KBeans_ interact with each other by declaring KBeans using `JkBean#getBean(MyBean.class)` method as shown in [this example](#simple-example).

When a _KBean_ depends on another one, it's good to declare it as an instance field of the first bean, as this 
dependency will be mentioned in the auto-generated documentation and showed explicitly in IDE tool.

### KBeans in Multi-Projects

In a multi-project, it's quite common that a _KBean_ accesses a _KBean_ instance from another project. 
You can achieve this in a statically typed way.

* In _master_ _KBean_, declare a field of type `JkBean` (e.g. ´JkBean importedBuild;`). It doesn't have to be public.
* Annotate it with `@JkInjectProject` mentioning the relative path of the imported project (e.g. `@JkInjectProject("../anotherModule")).
* Execute `jeka intellij#iml` or `jeka eclipse#files`.
* Redefine the declared type from `JkBean` to the concrete type of imported _KBean_
* Now, master _KBean_ can access the imported _KBean_ in a static typed way.
* See example [here](https://github.com/jerkar/jeka/blob/master/dev.jeka.master/jeka/def/MasterBuild.java).
* Be mindful that the imported _KBean_ must deal with file paths using `JkBean#getBaseDir` in order for it to be safely executed from any working directory.

### Standard KBeans

There are a bunch of _KBeans_ bundled within _Jeka_. Those _KBeans_ are always present.

#### project

`ProjectJkBean` provides a wrapper around a `JkProject` for building JVM-based projects. This _KBean_ initialises 
a default sensitive project object and provides a classic method for building a project (compile, package in jar, publish ...)

This _KBean_ proposes an extension point through its `configure(JkProject)` method. This way, other _KBeans_ can 
modify the properties of the project to be built.

#### intellij

`IntellijJkBean` provides methods for generating metadata files for _IntelliJ_ IDE. The content of an _iml_ file is computed 
according the `JkProject` object found in _project KBean_.

This _KBean_ proposes an extension point through its `configure` methods in order to modify the resulting iml
(e.g. using a module dependency instead of a library dependency).

#### scaffold

`ScaffoldjJkBean` provides methods for project directories and files to create a new _Jeka_ project. Basically, 
it creates a project ready to create vanilla automation tasks.

This _KBean_ offers an extension point in order for another KBean to augment the scaffolded structure. For example, 
executing `jeka scaffold#run` will create a basic Jeka project while `jeka scaffold#run project#` will create 
a project ready to build a JVM-based project. 

#### git

`GitJkBean` exposes some common git command combos. It can also auto-inject a version inferred from Git into *project* KBean.
