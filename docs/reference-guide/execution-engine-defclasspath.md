## Def classes

Def classes are the compilation results of Java/Kotlin source files located in _jeka/def_. 
The execution engine compiles these files on the fly prior adding them to the [Jeka classpath](#jeka-classpath).

It is possible to specify compilation options by annotating a _def class_ as :

```Java
@JkCompileOption("-deprecation")
@JkCompileOption({"-processorPath", "/foo/bar"})
class MyBuild extends JkBean {
  ...
}
```

!!! note
    * _jeka/def_ can also contain classpath resources.
    * Classes with names starting with `_` are skipped from compilation.

Java source files definded in *def* dir are compiled using the running JDK.

Kotlin sources are compiled using Kotlin versions specified by `jeka.kotlin.version` [property](#properties) if present or 
using Kotlin compilers specified by _KOTLIN_HOME_ environment variable. 


## Jeka Classpath

Under the hood, Jeka simply executes Java byte code within a flat classloader.
This classloader classpath is constructed from :

* jar files present in _jeka/boot_ dir
* dependencies injected via the command line and annotations
* compiled def classes
* Jeka classpaths from imported projects

### Injected Dependencies

It's possible to transitively inject dependencies into classpaths by either annotating _def classes_ or by mentionning it in command line.

!!! note
    By default, _jeka_ fetches dependencies from maven central (https://repo.maven.apache.org/maven2).

    You can select another default repository by setting the `jeka.repos.download.url` property. 
    We recommend storing this value in your [USER DIR]/.jeka/global.properties file to be reused across projects.

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
    Update your IDE dependencies right after adding this annotation in order for it to be 
    used inside your _def classes_.
    `jeka intellij#iml` or `jeka eclipse#files`

!!! warning
    Dependencies imported via `@JkInjectClasspath` are imported for all def classes and not only for the annotated class.


#### Inject from Command Line

Specify path or module coordinates in the command line using '@' as `@my.org:a-jeka-plugin:1.0.0` or `@../libs/myutils.jar`.

This feature is meant to dynamically invoke _KBeans_.

### Imported Projects

Jeka supports multi-module projects. _Jeka classpath_ of a given project can include _Jeka classpath_ of another one.

```Java
@JkInjectProject("../core")
class MyBuild extends JkBean {
  ...
}
```
Now, classes from project '../core' can be reused in this build class.
