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

!!! tip
    You can document your script by annotation Class, public fields or public method with `@JkDoc` annotation.
    This will be visible when executing: `jeka script: --help`.
    Note that only part before first breaking line of the doc content will be displayed as summary.


### Add dependencies

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

#### Use BOM dependencies

In some cases, we may need to use a BOM dependency which provides versioning information on other dependencies we might use.

```java
@JkInjectClasspath("com.google.cloud:libraries-bom<:pom:5.0.0")
@JkInjectClasspath("com.google.cloud:google-cloud-storage")
@JkInjectClasspath("com.google.cloud:oogle-cloud-bigquery")
```

#### Dependencies on file system

There is 2 way of adding local file system dependencies :

  - simply add a jar in *jeka-boot* dir (create this dir if not present)
  - annotate class with `@JkInjectClasspath()`

```java
@JkInjectClasspath("../other-project/mylib.jar")
@JkInjectClasspath("../other-project/my-classes")
```

### Add compilation directives

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

Creating many script classes in a single project isn't a common use case but it will help to understand 
some concepts related to [KBeans](../reference/kbeans.md)

1. In the existing project, create a new class *Build.java* at the root of *jeka-src*. This class should extend `KBean`.
2. Add a *public void no-args* method *foo* in this class
```java
import dev.jeka.core.tool.KBean;

public class Build extends KBean {

    public void foo() {
        System.out.println("Method 'foo()' is running.");
    }
}
```
and execute `jeka foo` to notice that this method is actually run.

3. Execute `jeka hello`. You should get an error message as : *ERROR: Unmatched argument at index 0: 'hello'*.

This is due that we did not mention the KBean class to use as default when invoking method.
By default, JeKa width-first explores *jeka-src* to find the first class implementing `KBean`, and so *Build.java* win.

Execute `jeka script: hello` to invoke `hello` method from `Script`class.

You can check the actual default KBean, by executing `jeka --info` and check for the *Default KBean* entry.

!!! note
    A given `KBean` class can accept many names to be referenced :

    - Its fully qualified class name (as org.eaxample.kbeans.MyCoolKBean)
    - Its short class name (as MyCoolKBean))
    - Its short class name withj uncapitalized first-letter (as myCoolKBean)
    - If the class name is ending with 'KBean', the KBean suffix can omitted (as myCool)

!!! Tip
    You can change the default KBean by mentioning `jeka.default.kbean=script` in *jeka.properties* file.

### Configure script execution

We can override the value of *public* field by specifying the value in *jeka.properties* as :
```properties
@script.name=Everybody
```

Execute 'jeka hello', you should get
```
Hello Everybody
```

### Make KBeans interact with each other

KBean mechanism plays a central role in JeKa ecosystem. In the following section, we will play around  
it to make you more familiar with.

1. Set `jeka.default.kbean=script` property in the *jeka.properties* file and remove `@script.name=Everybody` 
   added in previous step.
   ```properties
   jeka.default.kbean=script
   ```
   Also, make sure that *Script.java* and *Build.java*  are still present in *jeka-src* dir.

2. Add the following method in *Script.java*
```java
@Override
protected void init() {
    Script script = load(Script.class);  //  Get the singleton Script instance
    script.name = "Mates";           
}
```

3. Now, execute `jeka script: hello build:`. This will initialize *script* and *build* KBean singletons then
   invoke `Script.hello()` method.
   This result in displaying `Hello Mates` in the console. This means that *build* KBean has configured *script* successfully.

    !!! note
        When Jeka *initialize* a KBean, this means that it instantiates it prior invoking its `ìnit` method.

4. Now, execute `jeka script: hello`, you'll notice that this display back `Hello world`. This is due that 
   `build` is not initialized anymore.

   You can force it to be always initialized, by adding `@build=` property to *jeka-properties* file.
   
   Add it and retry `jeka script: hello`. Now it should display `Hello Mates`

## Classpath KBeans

As we just see, many KBeans can live together in 