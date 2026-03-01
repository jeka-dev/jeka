# Getting Started - Basics

**Prerequisite:** JeKa must be [installed](../installation.md).

Let's create some simple scripts to understand the basic concepts.

## Create a basic script

To create a basic JeKa structure, execute:
```bash
jeka base: scaffold
```

This generates a `jeka-src/Script.java` file, which is an example of Java code invokable from the command line.

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
    If IntelliJ does not reflect changes, execute `jeka intellij: initProject`.
    By default, `jeka-src` is declared as a test source folder of the IntelliJ module.
    You can make it live in its own module by executing `jeka intellij: jekaSrcAsModule`.

Execute `jeka hello`. A *Hello World* message is printed on the console.
```{ .txt .no-copy }
Hello World !
```
Execute `jeka hello name=JeKa`. A *Hello JeKa* message is printed on the console.
``` { .txt .no-copy }
Hello JeKa !
```

Add a similar `hi` method in `Script.java`:
```java
public void hi() {
    System.out.println("Hi " + name + " !");
}
```
and execute `jeka hi`. You will notice that your change has been automatically taken into account 
without any extra action on your part.

You can add as many *public void no-args* methods or *public fields* in your scripts.
The accepted *public field* types are mentioned [here](https://picocli.info/#_built_in_types).

!!! tip
    You can document your script by annotating the class, public fields, or public methods with the `@JkDoc` annotation.
    This will be visible when executing: `jeka script: --doc`.
    Note that only the part before the first line break of the doc content will be displayed as a summary.

## Define JDK version

We can select which JDK will run the script by using [properties](../reference/properties.md).

Edit `jeka.properties`:
```properties
jeka.java.version=23
```
Executing `jeka hello` will trigger a download of JDK 23 (if not already present), prior to executing 
the script. JeKa caches the downloaded JDKs in `[USER HOME]/.jeka/cache/jdks`.

It is possible to choose another distribution by using the following properties:
```properties
jeka.java.version=21
jeka.java.distrib=corretto
```
If you don't want JeKa to manage the distribution for you, you can choose the SDK location explicitly:
```properties
jeka.java.version=22
jeka.sdk.22=/my/jdks/22-corretto
```
!!! note
    The properties can also be set by using *system properties* or *OS Environment variables*.
    Continuous Integration machines can define env variables as `jeka.sdk.22` to override the SDK location.


## Define JeKa version

Your script may depend on unstable JeKa APIs. To ensure it always works, 
regardless of the JeKa version installed, add the following property to 
`jeka.properties`:
```properties
jeka.version=0.11.24
```

## Add dependencies

Your script can depend on libs located in a Maven repository, or on folders/jars located on the file system.

Annotate the `Script` class with:
```java
import dev.jeka.core.tool.JkDep;

@JkDep("com.github.lalyos:jfiglet:0.0.9")
class Script extends KBean {
}
```
... and execute `jeka intellij: iml` to use the imported library in the IDE.

Add a method with the following body:
```java
public void ascii() throws Exception {
    System.out.println(FigletFont.convertOneLine("Hello"));
}
```
... and execute `jeka ascii`. This will display on the console:
``` { .txt .no-copy }
  _   _      _ _       
 | | | | ___| | | ___  
 | |_| |/ _ \ | |/ _ \ 
 |  _  |  __/ | | (_) |
 |_| |_|\___|_|_|\___/ 
```
This library has no transitive dependency, but it could have. Try to import any library with transitive 
dependencies and execute `jeka --inspect`. 
This displays runtime information about the JeKa run, including the resulting classpath.

You can add as many `@JkDep` annotations as you need on the class.

!!! note
    JeKa also accepts JBang notation for declaring dependencies.
    You can use `//DEPS com.github.lalyos:jfiglet:0.0.9` in place of `@JkDep("com.github.lalyos:jfiglet:0.0.9")`.

### Use BOM dependencies

In some cases, we may need to use a BOM dependency which provides versioning information on other dependencies we might use.

```java
@JkDep("com.google.cloud:libraries-bom:pom:5.0.0")
@JkDep("com.google.cloud:google-cloud-storage")
@JkDep("com.google.cloud:google-cloud-bigquery")
```

### Dependencies on file system

There are two ways of adding local file system dependencies:

  - Simply add a jar in the `jeka-boot` directory (create this directory if not present).
  - Annotate the class with `@JkDep`.

```java
@JkDep("../other-project/mylib.jar")
@JkDep("../other-project/my-classes")
```

### Define dependencies with properties

Dependencies can also be mentioned using the `jeka.inject.classpath` property in the `jeka.properties` file.

```properties
jeka.inject.classpath=\
  com.google.cloud:libraries-bom:pom:5.0.0 \
  com.google.cloud:google-cloud-storage \
  com.google.cloud:google-cloud-bigquery
```

## Add compilation directives

Classes from `jeka-src` are compiled behind-the-scenes prior to being executed.

We can inject some compilation directives to the compiler by annotating the `Script` class 
with `@JkCompileOption`. 

For example, we can turn off some warning messages using:
```java
@JkCompileOption("-Xlint:-options")
```

## Multi-file scripts

`jeka-src` can host as many scripts and utility classes as you need. For now, we have a single 
class located in the default package, but we could have located `Script.java` in `org.example` the same way.

Creating many script classes in a single project isn't a common use case, but it will help to understand 
some concepts related to [KBeans](../reference/kbeans.md).

1. In the existing project, create a new class `Build.java` at the root of `jeka-src`. This class should extend `KBean`.
2. Add a *public void no-args* method `foo` in this class:
```java
import dev.jeka.core.tool.KBean;

public class Build extends KBean {

    public void foo() {
        System.out.println("Method 'foo()' is running.");
    }
}
```
and execute `jeka foo` to notice that this method is actually run.

3. Execute `jeka hello`. You should get the following error message:
   ```text
   ERROR: Unmatched argument at index 0: 'hello'
   ```
    This is because we did not mention the *KBean* to use as default when invoking the method.
    JeKa explores `jeka-src`, with a width-first strategy, to find the first class implementing `KBean`. 
    In this case, `Build.java` won.
   
    Execute `jeka foo`. It should display:
    ```text
    Method 'foo()' is running.
    ```
    
4. To execute a method of a specific *KBean*, we should mention it explicitly as: `jeka [kbean]: [method]`.

    Execute: `jeka script: hello`. This should display on the console:
    ```text
    Hello World !
    ```
    
5. We can specify the *KBean* to use as default using the `jeka.kbean.default=` property in `jeka.properties`.
    ```properties
    jeka.kbean.default=script
    ```

    You can check the actual default KBean by executing `jeka --inspect` and checking for the *Default KBean* entry.
    
    !!! note
        A given `KBean` class can accept many names to be referenced:
    
        - Its fully qualified class name (as `org.example.kbeans.MyCoolKBean`).
        - Its short class name (as `MyCoolKBean`).
        - Its short class name with an uncapitalized first-letter (as `myCoolKBean`).
        - If the class name ends with 'KBean', the 'KBean' suffix can be omitted (as `myCool`).

## Configure default values

We can override the value of *public* fields of *KBeans* by using properties as:
```properties
@script.name=Everybody
```

Add the last property to your `jeka.properties` file and execute `jeka hello`. You should get:
```text
Hello Everybody !
```

### Make KBeans interact with each other

The KBean mechanism plays a central role in the JeKa ecosystem. In the following section, we will play around with it to make you more familiar with it.

1. Set the `jeka.kbean.default=script` property in the `jeka.properties` file and remove `@script.name=Everybody` 
   added in the previous step.
   ```properties
   jeka.kbean.default=script
   ```
   Also, make sure that `Script.java` and `Build.java` are still present in the `jeka-src` directory.

2. Add the following method in `Build.java`:
    ```java
    public class Build extends KBean {
        
        @Override
        protected void init() {
            Script script = load(Script.class);  // Get the singleton Script instance
            script.name = "Mates";           
        }
    }
    ```
   The `init()` method is called when a KBean singleton is initialized by the JeKa engine.

3. Now, execute `jeka script: hello build:`. This initializes the `script` and `build` KBean singletons, then
   invokes the `Script.hello()` method.
   ```text
   Hello Mates !
   ```
   What has happened?
   JeKa has initialized the `script` and `build` KBeans, then has invoked the `Script.hello()` method.
   All *KBeans* to be initialized are initialized prior to any KBean method being invoked.


4. If you simply execute `jeka script: hello`, you'll notice that this displays `Hello World !`. This is because 
   `build` is not initialized anymore.

    You can force it to be always initialized by adding the `@build=` property to the `jeka.properties` file.

    Add it and retry `jeka script: hello`. It should display `Hello Mates !`.

## Classpath KBeans

We distinguish *local* KBeans (which are Java source files defined in `jeka-src`) from *classpath* KBeans (which 
are compiled classes lying in the JeKa classpath).

Execute `jeka --doc` to list all available KBeans. You'll notice the *standard KBeans* section that mentions 
all KBeans bundled with JeKa out-of-the-box (and always available). These are typically *classpath* KBeans.
 
For example, you can execute `jeka admin: openHomeDir` to open your *JeKa Home directory*.

### Add KBeans to classpath

Adding KBeans to the classpath just consists of adding a dependency that contains a KBean class. 

You can use `jeka.inject.classpath` properties as:
```properties
jeka.inject.classpath=\
  dev.jeka:springboot-plugin  \
  dev.jeka:sonarqube-plugin \
  dev.jeka:openapi-plugin:0.11.0.1
```

or declare it using the `@JkDep` annotation in any class from `jeka-src`.

!!! note
    When omitting the version for a dependency of group `dev.jeka`, as in `dev.jeka:springboot-plugin`, 
    JeKa uses its own running version for resolving the coordinate.
    This is because most extensions with the `dev.jeka` group are released at the same time as JeKa.

### Example with Node.js

It is also possible to augment the classpath dynamically from the command line, using the `-cp` option.

In this example, we'll add the Node.js plugin. The plugin downloads Node.js version 20.12.2 (if needed) 
and then executes the specified command line.

```bash
jeka -cp=dev.jeka:nodejs-plugin nodeJs: version="20.12.2" exec cmdLine="npx cowsay Hello JeKa"
```
This should display:
```text
Directory not found /Users/jerome/temp-jeka-tests/client-js, use current dir as working dir.
Task: start-program >npx cowsay Hello JeKa
       ___________
      < Hello JeKa >
       -----------
              \   ^__^
               \  (oo)\_______
                  (__)\       )\/\
                      ||----w |
                      ||     ||

```

You can get more info about the *Node.js* plugin by executing: 
```bash
jeka -cp=dev.jeka:nodejs-plugin nodeJs: --doc
```
