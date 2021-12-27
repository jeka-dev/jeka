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

_KBean_ is the central concept of execution engine. 

* Extending `JkBean`
* May declare `public void` methods taking no arguments. All these methods are invokable from command line.
* May declare `public` fields _(aka KBean properties)_. These field values can be injected from command line.
* May override `init` method to perform specific initialisation tasks.
* May override `postInit` method to perform tasks once all KBeans has been initialized.
* They are supposed to be instantiated by the execution engine and not from user code. 
 

* KBean methods can be invoked from command line as`jeka [kbeanName]#methoName [kbeanName]#[propertyName]=xxx` or 
from the IDE using a basic `main` method (see later).
* Many methods/properties can be invoked in a single command line.
* _[kbeanName]#_ prefix can be omitted. By default, it will be resolved on the first KBean found in _def_ dir.  

In a given project, there can only be one _KBean_ instance per _KBean_ class, but if you work with a multi-project
build there can be several in classpath (one per project).

Generally _KBeans_ interact with each other inside their `init` method. They access each other using `getRuntime().getRegistry().get(MyBean.class)`.

When a _KBean_ depends on another one, it's good to declare it as an instance property of the first bean as this 
dependency will be mentioned in the auto-generated documentation.

### Create a Basic KBean

* Create a class extending `JKBean` in _def_ source dir.
* Declare a public field of a simple type (String, boolean, int, float, enum, date, composite objects of simple types).
    It can be declared with a default value (e.g. `public int yourFieldName = 3;`).
* Declare a `public void` method taking no arguments. Implement it in a way it depends on the declared field.
* Execute `jeka yourMethodName yourFiedName=5` on console at root of you project. It runs !

___Extras___

* Annotate class, fields and methods with `@JkDoc` to provide help support.
* Execute `jeka help` to see _KBean_ description in help console. 
* _def_ may contain several classes. They can be helpers or other _KBEANS_. If you want a class not to be compiled, name it with a leading '_'.
* Fields can be annotated with `@JkInjectProperty("my.prop.name")` to inject the value of a _property_ in.
* For more details about accepted field injected types, see `dev.jeka.core.tool.FieldInjector#parse` method.
* _KBean_ properties can also been nested composite objects, see example in `dev.jeka.core.tool.builtins.project.ProjectJkBean#pack` field.


### Import _KBean_ from other Projects

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
