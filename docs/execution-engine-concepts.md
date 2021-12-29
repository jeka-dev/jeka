## Def classes

Java or Kotlin source files present in _jeka/def_ dir. They can be declared at root or 
in packages.
These files are compiled on the fly using the running JDK then are added to the _Jeka classpath_.

It is possible to specify compilation options by annotating a _def class_ as :

```Java
@JkCompileOption("-deprecation")
@JkCompileOption({"-processorPath", "/foo/bar"})
class MyBuild {
  ...
}
```

!!! note
    * _jeka/def_ can also contain classpath resources.
    * Classes having a name starting by `_` are skipped from compilation.


## Jeka Classpath

Under the hood, Jeka simply executes Java byte code within a flat classloader.
Classloader's classpath is constructed from :

* jar files present in _jeka/boot_ dir
* dependencies injected via command line and annotation
* compiled def classes
* Jeka classpaths coming from imported projects.

### Injected Dependencies

It's possible to inject transitively dependencies into classpath by either annotating _def classes_ or by mentionning it in command line.

!!! note
    By default, _jeka_ fetch dependencies from maven central (https://repo.maven.apache.org/maven2).

    You can select another default repository by setting the `jeka.repos.download.url` options. 
    We recommend storing this value in your [USER DIR]/.jeka/options.properties file to be reused across projects.

    For more details, see `JkRepoFromOptions` javadoc.

#### Inject from Def Classes

Annotate a _def class_ with `@JkInjectClasspath` mentioning either a module coordinate or a path on the local file system.

```Java
@JkInjectClasspath("org.seleniumhq.selenium:selenium-remote-driver:4.0.0")
@JkInjectClasspath("../libs/myutils.jar")
class MyBuild {
  ...
}
```

!!! note
    Update your IDE dependencies right after adding this annotation in order it can be 
    used inside your _def classes_.
    `jeka intellij#iml` or `jeka eclipse#files`

!!! warning
    Dependencies imported via `@JkInjectClasspath` are imported for all def classes and not only for annotated class.


#### Inject from Command Line

Specify path or module coordinates in the command line using '@' as `@my.org:a-jeka-plugin:1.0.0` or `@../libs/myutils.jar`.

This feature is meant to invoke _KBeans_ dynamically.

### Imported Projects

Jeka supports multi-module projects in _Jeka classpath_ of a given project can include _Jeka classpath_ of another one.

```Java
@JkInjectProject("../core")
class MyBuild {
  ...
}
```

## KBeans

_KBean_ is the central concept of execution engine. _KBeans_ are classes where are declared executable methods. 
There is only one _KBean_ instance by _KBean_ class in a given Jeka project.

_KBean_ classes share the following characteristics :

* Extend `JkBean`
* May declare `public void` methods taking no arguments. All these methods are invokable from command line.
* May declare `public` fields _(aka KBean properties)_. These field values can be injected from command line.
* May override `init` and `postInit` methods to perform specific initialisation tasks.
* They are supposed to be instantiated by the execution engine and not from user code. 

### Simple Example

The follwing KBeans exposes `cleanPublish` method which delegate the creation of jar files to the 'project' KBean.
`ProjectJkBean` is available on Jeka classpath as it is part of the standard KBeans bundled in Jeka distribution.

The _init_ method configures the underlying _JkProject_ hold by the `ProjectJkBean`.


```Java
@JkDoc("A simple example to illustrate KBean concept.")
public class SimpleJkBean extends JkBean {

    ProjectJkBean projectBean = getRuntime().getBean(ProjectJkBean.class);

    @JkDoc("Version of junit-jupiter to use for compiling and running tests")
    public String junitVersion = "5.8.1";

    @Override
    protected void init() {
       projectBean.getProject().simpleFacade()
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
        clean(); projectBean.pack();
    }
    
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

#### From KBean main method


_KBean_ methods can also be launched/debugged from IDE.

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

#### From dev.jeka.core.tool.Main 

Sometimes, you may need to mimic closer the command line behavior, for debugging purpose or to pass '@' arguments.

* Create an IDE launcher for a Java Application
* Set `dev.jeka.tool.Main` as Java main class.
* Set the same command line arguments as you would do for invoking from command line (Do not include _jeka_ command).


### Let KBeans cooperate


Generally _KBeans_ interact with each other inside their `init` method. They access each other using 
`getRuntime().getBean(MyBean.class)`.

When a _KBean_ depends on another one, it's good to declare it as an instance property of the first bean as this 
dependency will be mentioned in the auto-generated documentation.


### KBeans in Multi-Projects

In multi-project build, it's quite common that a _KBean_ accesses to a _KBean_ instance coming from another project. 
You can achieve it in a statically typed way.

* In _master_ _KBean_, declare a field of type `JkBean` (e.g. ´JkBean importedBuild;`). It doesn't have to be public.
* Annotate it with `@JkInjectProject` mentioning the relative path of the imported project (e.g. `@JkInjectProject("../anotherModule")).
* Execute `jeka intellij#iml` or `jeka eclipse#files`.
* Redefine the declared type from `JkBean` to the concrete type of imported _KBean_
* Now, master _KBean_ can access the imported _KBean_ in a static typed way.
* See example [here](https://github.com/jerkar/jeka/blob/master/dev.jeka.master/jeka/def/MasterBuild.java).
* Be careful that the imported _KBean_ deals with file paths using `JkBean#getBaseDir` in order it can be safely executed from any working directory.


## Properties

Properties are pairs of String  _key-value_ that are used across Jeka system. It typically carries urls, local paths,
tool versions or credentials. They can be globally accessed using `JkProperties#get*` static method.

Properties can be defined at different level, in order of precedence :

* System properties : Properties can be defined using system properties as `-DpropertyName=value`. System properties can
  be injected from Jeka command line.
* OS environment variables : Properties can also be defined as OS environment variable.
* Project : Defined in _[Project Root]/jeka/project.properties_. Used typically for storing tool version (e.g. `jeka.kotlin.version=1.5.21`).
* Global : Defined in _[User Home]/.jeka/global.properties_ file. Used typically to define urls, local paths and credentials.

Standard properties :

* `jeka.jdk.X=` location of the JDK version X _(e.g. jeka.jdk.11=/my/java/jdk11)_. It is used to compile projects when 
  project JVM target version differs from Jeka running version.
* `jeka.repos.download.url` : Base url of the repository used to download dependencies
* `jeka.kotlin.version` : Version of Kotlin used for compiling both _def_ and Kotlin project sources.
