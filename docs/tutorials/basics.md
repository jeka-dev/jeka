# Getting Started - Basics

**Prerequisite:** Jeka must be [installed](../installation.md).

Let's create some simple scripts to understand the basic concepts.

## Create a basic script

To create a basic Jeka structure, execute:
```
jeka base: scaffold
```

This generates a *jeka-src/Script.java* file, which is an example of Java code invokable from command line.

```java
class Script extends KBean {

    @JkDoc("Person to whom the greeting is intended")
    public String name = "World";

    @JkDoc("Print greeting on console")
    public void hello() {
        String greetings = "Hello " + name + " !";
        System.out.println(greetings);
    }

}
```

!!! tip
    If you use *IntelliJ*, execute `jeka intellij: iml` to synchronize the IDE metadata. 
    If IntelliJ does not reflect changes, execute `jeka intellij: initProject`
    By default, 'jeka-src' is declared as a test source folder of the Intellij module.
    You can make it live in its own module by executing `jeka intellij: jekaSrcAsModule`

Execute `jeka hello`. A *Hello World* message is printed on console.
```{ .txt .no-copy }
Hello World !
```
Execute `jeka hello name=JeKa`. A *Hello JeKa* message is printed on console.
``` { .txt .no-copy }
Hello JeKa !
```

Add a similar `hi` method in *Script.java* 
```java
public void hi() {
    System.out.println("Hi " + name + " !");
}
```
and execute `jeka hi`. you notice that your change has been automatically taken in account 
without any extra action from your part.

You can add as many *public void no-args* methods or *public fields* in your scripts.
The accepted *public field* types are mentioned [here](https://picocli.info/#_built_in_types).

!!! tip
    You can document your script by annotation Class, public fields or public method with `@JkDoc` annotation.
    This will be visible when executing: `jeka script: --doc`.
    Note that only part before first breaking line of the doc content will be displayed as summary.

## Define JDK version

We can select which JDK will run the script bu using [properties](reference/properties)

Edit *jeka.properties*:
```properties
jeka.java.version=23
```
Executing `jeka hello` will trigger a download of JDK 22 (if not already present), prior executing 
the script. JeKa caches the downloaded JDKs in *[USER HOME]/.jeka/cache/jdks*.

This is possible to choose another distribution by using the following properties :
```properties
jeka.java.version=21
jeka.java.distrib=corretto
```
If you don't want JeKa manage distribution for you, you can choose explicitly the SDK location:
```properties
jeka.java.version=17
jeka.sdk.17=/my/jdks/17-corretto
```
!!! note
    The properties can also be set by using *system properties* or *OS Environment variables*.
    Continuous Integration machine can define env variables as `jeka.sdk.17` to override the SDK location.


## Define JeKa version

Your script may depends of some unstable JeKa APIs. To make sure your script will always work, 
whatever JeKa version is installed at client/user side, mention the following properties in 
*jeka.properties*, as :
```properties
jeka.version=0.11.12
```

## Add dependencies

Your script can depends on libs located in a Maven repository, or on folder/jar located on file system.

Annotate `Script` class with  :
```java
import dev.jeka.core.tool.JkDep;

@JkDep("com.github.lalyos:jfiglet:0.0.9")
class Script extends KBean {
```
... and execute `jeka intellij: iml` to use the imported library in IDE.

Add a method with following body:
```java
public void ascii() throws Exception {
    System.out.println(FigletFont.convertOneLine("Hello"));
}
```
... and execute `jeka ascii`. This will display on console :
``` { .txt .no-copy }
  _   _      _ _       
 | | | | ___| | | ___  
 | |_| |/ _ \ | |/ _ \ 
 |  _  |  __/ | | (_) |
 |_| |_|\___|_|_|\___/ 
```
This library has no transitive dependency, but it could have. Try to import any library with transitive 
dependencies and execute `jeka --inspect`. 
This displays runtime information about JeKa run, including the resulting classpath.

You can add, as many `@JkDep` annotations you need, on the class.

!!! note
    JeKa also accept JBang notation for declaring dependencies.
    You can use `//DEPS com.github.lalyos:jfiglet:0.0.9"` in place of `@JkDep("com.github.lalyos:jfiglet:0.0.9")`

### Use BOM dependencies

In some cases, we may need to use a BOM dependency which provides versioning information on other dependencies we might use.

```java
@JkDep("com.google.cloud:libraries-bom::pom:5.0.0")
@JkDep("com.google.cloud:google-cloud-storage")
@JkDep("com.google.cloud:oogle-cloud-bigquery")
```

### Dependencies on file system

There is 2 way of adding local file system dependencies :

  - simply add a jar in *jeka-boot* dir (create this dir if not present)
  - annotate class with `@JkDep()`

```java
@JkDep("../other-project/mylib.jar")
@JkDep("../other-project/my-classes")
```

### Define dependencies with properties

Dependencies can also be mentioned using the `jeka.classpath=` property in *jeka.properties* file.

```
jeka.classpath=\
  com.google.cloud:libraries-bom::pom:5.0.0 \
  com.google.cloud:google-cloud-storage \
  com.google.cloud:oogle-cloud-bigquery
```

## Add compilation directives

Classes from *jeka-src* are compiled behind-the-scene prior of being executed.

We can inject some compilation directive to the compiler by annotating the Script class 
with `@JkInjectCompileOption()`. 

For example we can turn off some warning messages using 
```java
@JkCompileOption("-Xlint:-options")
```

## Multi-file scripts

*jeka-src* can host as many scripts and utility classes as you need. For now, we have a single 
class located in default package, but we could have located *Script.java* in *org.example* the same way.

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

3. Execute `jeka hello`. You should get the following error message
   ``` { .txt .no-copy }
   ERROR: Unmatched argument at index 0: 'hello'
   ```
    This is because,  we did not mention the *KBean* to use as default when invoking method.
    JeKa explores *jeka-src*, with width-first strategy, to find the first class implementing `KBean`. 
    In this case, *Build.java* won.
   
    Execute `jeka foo`. It should display:
    ``` { .txt .no-copy }
    Method 'foo()' is running.
    ```
    
4.  To execute a method of a specific *KBean*, we should mention it explicitly as: `jeka [kbean]: [method]`.

    Execute: `jeka script: hello`. This should display on console:
    ``` { .txt .no-copy }
    Hello World !
    ```
    
5.  We can specify the *KBean* to use as default using `jeka.kbean.default=`property in *jeka.properties*.
    ```properties
    jeka.kbean.default=script
    ```

    You can check the actual default KBean, by executing `jeka --inspect` and check for the *Default KBean* entry.
    
    !!! note
        A given `KBean` class can accept many names to be referenced :
    
        - Its fully qualified class name (as org.eaxample.kbeans.MyCoolKBean)
        - Its short class name (as MyCoolKBean))
        - Its short class name withj uncapitalized first-letter (as myCoolKBean)
        - If the class name is ending with 'KBean', the KBean suffix can omitted (as myCool)

## Configure default values

We can override the value of *public* fields of *KBeans* by using properties as :
```properties
@script.name=Everybody
```

Add the last properties tou your *jeka.properties* file and xecute 'jeka hello'. You should get ::
``` { .txt .no-copy }
Hello Everybody
```

### Make KBeans interact with each other

KBean mechanism plays a central role in JeKa ecosystem. In the following section, we will play around it to make you more familiar with.

1. Set `jeka.kbean.default=script` property in the *jeka.properties* file and remove `@script.name=Everybody` 
   added in previous step.
   ```properties
   jeka.kbean.default=script
   ```
   Also, make sure that *Script.java* and *Build.java*  are still present in *jeka-src* dir.

2. Add the following method in *Build.java*
    ```java
    public class Build extends KBean {
        
        @Override
        protected void init() {
            Script script = load(Script.class);  //  Get the singleton Script instance
            script.name = "Mates";           
        }
    ```
   The init() method is called when a Kbean singleton is initialized by JeKa engine.

3. Now, execute `jeka script: hello build:`. This initializes *script* and *build* KBean singletons then
   invokes `Script.hello()` method.
   ``` { .txt .no-copy }
   Hello Mates !
   ```
   What has happened ?
   JeKa has initialized *script* and *build* KBeans, then has invoked `Script.hello()` method.
   All *KBeans* to be initialized, are initialized prior any KBean method is invoked.


4. If you simply execute `jeka script: hello`, you'll notice that this display `Hello world`. This is because 
   `build` is not initialized anymore.

    You can force it to be always initialized, by adding `@build=` property to *jeka-properties* file.

    Add it and retry `jeka script: hello`. It should display `Hello Mates`

## Classpath KBeans

We distinct *local* KBeans (which are Java sources file defined in *jeka-src*) from *classpath* KBeans (which 
are compiled classes lying in the Jeka classpath).

Execute: `jeka --doc` to list all available KBeans. You'll notice the *standard KBeans* section that mentions 
all KBeans bundled with JeKa out-of-the-box (and always available). These are typically *classpath* KBeans.
 
For example, you can execute `jeka admin: openHomeDir` to open your*Jeka Home directory*.

### Add KBeans to classpath

Adding KBeans to classpath just consists in adding a dependency that contains a KBean class. 

You can use `jeka.classpath` properties as :
```properties
jeka.classpath=\
  dev.jeka:springboot-plugin  \
  dev.jeka:sonarqube-plugin \
  dev.jeka:openapi-plugin:0.11.0.1
```

or declare it using `@JkDep` annotation in any class from *jeka-src*.

!!! Note
    When omitting the version for a dependency of group 'dev.jeka', as in 'dev.jeka:springboot-plugin', 
    JeKa uses its own running version for resolving the coordinate.
    This is due that most of extension with 'dev.jeka' group share released in the same time than JeKa.

### Example with NodeJs

This is also possible to augment classpath dynamically from the command line, using the `-cp`option.

In this example, we'll add the nodeJs plugin. The plugin doanload Nodejs version 20.12.2 (if needed) 
and then execute the specified command line.

```shell
jeka -cp=dev.jeka:nodejs-plugin nodeJs: version="20.12.2" exec cmdLine="npx cowsay Hello JeKa"
```
This should display:
```
Directory not found /Users/jerome/temp-jeka-tests/client-js, use current dir as working dir.
Task: start-program >npx cowsay Hello
       ___________
      < Hello JeKa>
       -----------
              \   ^__^
               \  (oo)\_______
                  (__)\       )\/\
                      ||----w |
                      ||     ||
      npm notice 
      npm notice New minor version of npm available! 10.5.0 -> 10.9.1
      npm notice Changelog: <https://github.com/npm/cli/releases/tag/v10.9.1>
      npm notice Run `npm install -g npm@10.9.1` to update!
      npm notice 

```

You can have more info about *NodeJs* plugin by executing 
```shell
jeka -cp=dev.jeka:nodejs-plugin nodeJs: --doc
```
