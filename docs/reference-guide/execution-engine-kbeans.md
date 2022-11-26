## KBeans

_KBean_ is the central concept of execution engine. _KBeans_ are classes where are declared executable methods. 
There is only one _KBean_ instance by _KBean_ class in a given Jeka project.

_KBean_ classes share the following characteristics :

* Extend `JkBean`
* May declare `public void` methods taking no arguments. All these methods are invokable from command line.
* May declare `public` fields _(aka KBean properties)_. These field values can be injected from command line.
* They are supposed to be instantiated by the execution engine and not from user code. 

### Simple Example

The follwing KBeans exposes `cleanPublish` method which delegate the creation of jar files to the 'project' KBean.
`ProjectJkBean` is available on Jeka classpath as it is part of the standard KBeans bundled in Jeka distribution.

The _configure_ method will be actually invoked at the first `ProjectJkBean#getProject()` call.

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

A _KBean command_ is an instance method of a KBean class that can be invoked from command line. In order to be considered as a _command_, a method must :

* be `public`
* be an instance method
* take no argument
* return `void`

### KBean Properties

A _KBean property_ is a `public` instance field of a KBean class. Its value can be injected from command line.

Fields can be annotated with `@JkInjectProperty("my.prop.name")` to inject the value of a _property_ in.

For more details about field accepted types, see `dev.jeka.core.tool.FieldInjector#parse` [method](https://github.com/jerkar/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/FieldInjector.java).

_KBean properties_ can also been nested composite objects, see example in `ProjectJkBean#pack` [field](https://github.com/jerkar/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/tool/builtins/project/ProjectJkBean.java).


### Naming KBeans

In order to be referenced conveniently, _KBeans_ accept to be called by a name. For a given _JkBean_ class, ccepted names are :

1. Full qualified class name
2. Uncapitalized simple class name (e.g. 'myBuild' matches 'org.example.MyBuild')
3. Uncapitalizes simple class Name without 'JkBean' suffix (.g. 'project' matches 'dev.jeka.core.tool.builtin.project.ProjectJkBean')

!!! tip
    Execute `jeka`, at the root of a project to display _KBeans_ present in _Jeka classpath_.

### Document KBeans 

_KBean_ classes, methods and properties can be annotated with `@JkDoc` annotation in orderder to provide self documentation.

Text within these annotations is displayed when invoking `help` method on console.

### Invoke KBeans 

#### From Command Line

KBean methods can be invoked from command line using 

`jeka [kbeanName]#methoName [kbeanName]#[propertyName]=xxx`  

Many methods/properties can be invoked in a single command line.

!!! info
    _[kbeanName]#_ prefix can be omitted. By default, it will be resolved on the first KBean found in _def_ dir. 
    Search is made by fully qualified class name alphabetical order.
    
    

#### From IntelliJ Jeka Plugin

[IntelliJ Jeka Plugin] allows to invoke methods directly from the editor or the explorer tool window.

#### From naked IDE

_KBean_ methods can also be launched/debugged from IDE using classic `main` methods.

In _KBean_ class, declare one or many main methods as :

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
_KBean_ must be instantiated using `JkInit#instanceOf` in order it be setup in proper state.

The arguments passed in `main` method are interpreted as command line arguments.

Launching or debugging this way is performant as all build classes and their dependencies are already on classpath. Therefore, no compilation or dependency resolution is needed.


!!! warning
    Be careful to launch the _main_ method using _module dir_ as _working dir_. On _IntelliJ_, this is not the default (it uses _project dir_).

    To change _intelliJ_ defaults, follow : **Edit Configurations | Edit configuration templates... |  Application | Working Directory : $MODULE_DIR$**.


Sometimes, you may need to mimic closer the command line behavior, for debugging purpose or to pass '@' arguments.

* Create an IDE launcher for a Java Application
* Set `dev.jeka.tool.Main` as Java main class.
* Set the same command line arguments as you would do for invoking from command line (Do not include _jeka_ command).


### Let KBeans cooperate

Generally _KBeans_ interact with each other by declaring KbBeans using `JkBean#getBean(MyBean.class)` method as shown in [this example](#simple-example).

When a _KBean_ depends on another one, it's good to declare it as an instance field of the first bean, as this 
dependency will be mentioned in the auto-generated documentation and showed explicitly in IDE tool.

### KBeans in Multi-Projects

In multi-project, it's quite common that a _KBean_ accesses to a _KBean_ instance coming from another project. 
You can achieve it in a statically typed way.

* In _master_ _KBean_, declare a field of type `JkBean` (e.g. ´JkBean importedBuild;`). It doesn't have to be public.
* Annotate it with `@JkInjectProject` mentioning the relative path of the imported project (e.g. `@JkInjectProject("../anotherModule")).
* Execute `jeka intellij#iml` or `jeka eclipse#files`.
* Redefine the declared type from `JkBean` to the concrete type of imported _KBean_
* Now, master _KBean_ can access the imported _KBean_ in a static typed way.
* See example [here](https://github.com/jerkar/jeka/blob/master/dev.jeka.master/jeka/def/MasterBuild.java).
* Be careful that the imported _KBean_ deals with file paths using `JkBean#getBaseDir` in order it can be safely executed from any working directory.

### Standard KBeans

There is a bunch of _KBeans_ bundled within _Jeka_. Those _KBeans_ are always present.

#### project

`ProjectJkBean` provides a wrapper around of a `JkProject` for building JVM-based projects. This _KBean_ initialise 
a default sensitive project object and provides classic method for building project (compile, package in jar, publish ...)

This _KBean_ proposes extension point through its `configure(JkProject)` method. This way, other _KBeans_ can 
modify the properties of the project to build.

#### intellij

`IntellijJkBean` provides methods for generating metadata files for _IntelliJ_ IDE. Content of _iml_ file is computed 
according the `JkProject` object found in _project KBean_.

This _KBean_ proposes extension point through its `configure` methods in order to modify the resulting iml
(e.g. use a module dependency instead of a library dependency).

#### scaffold

`ScaffoldjJkBean` provides methods for project directories and files to create a new _Jeka_ project. Basically, 
it creates a project ready to create vanilla automation tasks.

This _KBean_ offers extension point in order other KBean can augment the scaffolded structure. For example, 
executing `jeka scaffold#run` will create a basic Jeka project while `jeka scaffold#run project#` will create 
a project ready to build a JVM-based project. 

#### git

`GitJkBean` exposes some common git command combos. It can also auto-inject version inferred from Git into *project* KBean.
