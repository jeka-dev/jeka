# Getting Started

Let's create some basic script to understand basic concepts.

!!! Note
    Use the `jeka --help` command to start navigate in command line help.

## Create a basic script

Execute `jeka base: scaffold` to create a basic Jeka structure.

The *jeka-src/Script.java* contains a Java code that can be invoked as a script from command line.

If you use *IntelliJ*, execute `jeka intellij: iml` to synchronize the IDE metadata. 
If IntelliJ does not reflect changes, execute `jeka intellij: initProject`

Execute `jeka hello`. A *Hello World* message is printed on console.

Execute `jeka hello name=JeKa`. A *Hello JeKa* message is printed on console.

Rename `hello`  method (to `hi` for say) and the greeting message in the script.
Now execute `jeka hi` : you notice that your change has been automatically taken in account 
without any extra action from your part.

You can add as many *public void no-args* methods or *public fields* in your scripts.
The accepted *public field* types are mentioned [here](https://picocli.info/#_built_in_types).

### Configure script

We can override the value of *public* field by specifying the value in *jeka.properties* 
file as `@script.name=Everybody`('script' being the simple class name with camel-case).

!!! Note
    Execute `jeka : --help` to display specific help on the current script.

## Add dependencies

Your script can depends on libs located in a Maven repository, or on folder/jar located on file system.

Annotate `Script` class with  :
```java
import dev.jeka.core.tool.JkInjectClasspath;

@JkInjectClasspath("com.github.lalyos:jfiglet:0.0.9")
class Script extends KBean {
```
... and execute `jeka intellij: iml` to use the imported library in IDE.

Add a method with following body:
```java
public void ascii() throws Exception {
    System.out.println(FigletFont.convertOneLine("Hello"));
}
```
... and execute `jeka ascii` to display :
```
  _   _      _ _       
 | | | | ___| | | ___  
 | |_| |/ _ \ | |/ _ \ 
 |  _  |  __/ | | (_) |
 |_| |_|\___|_|_|\___/ 
```
This library has no transitive dependency, but it could have. Try to import any transitive you like 
and execute `jeka --info`. This displays runtime information about JeKa run, including classpath.

You can add as many `@JkInjectClasspath` annotation on the class.

### Use BOM dependencies

In some cases, we may need to use a BOM dependency which provides versioning information on other dependencies we might use.

```java
@JkInjectClasspath("com.google.cloud:libraries-bom<:pom:5.0.0")
@JkInjectClasspath("com.google.cloud:google-cloud-storage")
@JkInjectClasspath("com.google.cloud:oogle-cloud-bigquery")
```

### Dependencies on file system

There is 2 way of adding local file system dependencies :

  - simply add a jar in *jeka-boot* dir (create this dir if not present)
  - annotate class with `@JkInjectClasspath()`

```java
@JkInjectClasspath("../other-project/mylib.jar")
@JkInjectClasspath("../other-project/my-classes")
```

## Add compilation directives

Classes from *jeka-src* are compiled behind-the-scene prior of being executed.

We can inject some compilation directive to the compiler by annotating the Script class 
with `@JkInjectCompileOption()`. 

For example we can turn off some warning messages using 
```java
@JkInjectCompileOption("-Xlint:-options")
```

## Multi-file scripts

On the previous example, *jeka-src* dir is containing a single *Script.java* file.

There can be as many Script ot utility class you need and located in any package structure, as 
for any Java program.

For example, you can move *Script.java* file in *org.example* package and the script 
will still work exactly as it used to.

### Multiple Scripts

Creating many scripts class in one project isn't a common use case but it will help to understand 
some concepts related to [KBeans](reference/kbeans.md)
