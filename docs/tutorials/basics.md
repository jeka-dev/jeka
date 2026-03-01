# JeKa Basics

**Prerequisite:** JeKa must be [installed](../installation.md).

JeKa is not just a build tool; it's a powerful companion for Java development that allows you to run Java code as scripts with zero ceremony. This tutorial will walk you through the core concepts of JeKa by creating and running simple scripts.

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

!!! tip "IntelliJ IDEA Users"
    The [JeKa IntelliJ Plugin](https://plugins.jetbrains.com/plugin/24505-jeka) is the recommended way to work with JeKa. 
    If you prefer using the CLI for synchronization, execute `jeka intellij: sync` to update IDE metadata.
    If IntelliJ does not reflect changes, you can re-initialize the project with `jeka intellij: initProject`.
    
    By default, `jeka-src` is declared as a test source folder. You can isolate it in its own module by executing `jeka intellij: sync splitModule=true`.

Execute `jeka hello`. A *Hello World* message is printed on the console.
```text
Hello World !
```

Execute `jeka hello name=JeKa`. A *Hello JeKa* message is printed on the console.
```text
Hello JeKa !
```

Add a similar `hi` method in `Script.java`:
```java
public void hi() {
    System.out.println("Hi " + name + " !");
}
```
... and execute `jeka hi`. You will notice that your change has been automatically taken into account 
without any extra action on your part.

You can add as many *public void no-args* methods or *public fields* in your scripts.
The accepted *public field* types are mentioned [here](https://picocli.info/#_built_in_types).

!!! tip
    You can document your script by annotating the class, public fields, or public methods with the `@JkDoc` annotation.
    This will be visible when executing: `jeka script: --doc`.
    Note that only the part before the first line break of the doc content will be displayed as a summary.

## Define JDK version

JeKa can automatically manage the JDK used to run your scripts. This ensures reproducibility across different machines.

Edit `jeka.properties`:
```properties
jeka.java.version=23
```
Executing `jeka hello` will now trigger a download of JDK 23 (if not already present) before executing the script. JeKa caches downloaded JDKs in `~/.jeka/cache/jdks`.

You can also specify a distribution:
```properties
jeka.java.version=21
jeka.java.distrib=corretto
```

If you prefer to use a local JDK, specify its path:
```properties
jeka.java.version=22
jeka.sdk.22=/my/jdks/22-corretto
```
!!! note
    Properties can also be set via system properties or environment variables (e.g., `JEKA_SDK_22` or `-Djeka.sdk.22=...`).

## Define JeKa version

To ensure your script always runs with a compatible JeKa version, regardless of the version installed on the host, add this to `jeka.properties`:
```properties
jeka.version=0.11.24
```

## Add dependencies

Your scripts can easily depend on external libraries from Maven repositories or local files.

Annotate the `Script` class:
```java
import dev.jeka.core.tool.JkDep;

@JkDep("com.github.lalyos:jfiglet:0.0.9")
class Script extends KBean {
}
```
... and execute `jeka intellij: sync` to update your IDE classpath.

Now use the library in a new method:
```java
public void ascii() throws Exception {
    System.out.println(FigletFont.convertOneLine("Hello"));
}
```
... and execute `jeka ascii`. This library has no transitive dependency, but it could have. Try to import any library with transitive dependencies and execute `jeka --inspect`. 
This displays runtime information about the JeKa run, including the resulting classpath.

!!! note
    JeKa also supports JBang-style dependency declarations:
    `//DEPS com.github.lalyos:jfiglet:0.0.9`

### Use BOM dependencies

For complex dependencies, you can import a Maven BOM:

```java
@JkDep("com.google.cloud:libraries-bom:5.0.0@pom")
@JkDep("com.google.cloud:google-cloud-storage")
@JkDep("com.google.cloud:google-cloud-bigquery")
```

### File system dependencies

You can add local JARs or class folders:
- Place JARs in the `jeka-boot` directory (create it if not present).
- Or use `@JkDep` with relative paths:
```java
@JkDep("../other-project/mylib.jar")
@JkDep("../other-project/my-classes")
```

### Dependencies in properties

Alternatively, define dependencies in `jeka.properties`:
```properties
jeka.classpath=\
  com.google.cloud:libraries-bom:5.0.0@pom \
  com.google.cloud:google-cloud-storage \
  com.google.cloud:google-cloud-bigquery
```

## Compilation directives

JeKa compiles `jeka-src` classes on-the-fly. You can pass options to the Java compiler using `@JkCompileOption`:

```java
@JkCompileOption("-Xlint:-options")
```

## Multi-file scripts & KBeans

`jeka-src` can host multiple scripts and utility classes. Understanding how JeKa finds and executes these classes (called **KBeans**) is essential.

1. Create a new class `Build.java` in `jeka-src`:
```java
import dev.jeka.core.tool.KBean;

public class Build extends KBean {
    public void foo() {
        System.out.println("Method 'foo()' is running.");
    }
}
```
Execute `jeka foo`. It works because `Build` is now the primary KBean found.

2. Now try `jeka hello`. You will get an error:
   `ERROR: Unmatched argument at index 0: 'hello'`
   
   This is because JeKa uses a discovery strategy to find a default KBean. When multiple KBeans exist, you may need to be explicit.

3. To execute a method from a specific KBean, use the format `jeka [kbean]: [method]`:
   `jeka script: hello`

4. You can define the default KBean in `jeka.properties`:
   ```properties
   jeka.kbean.default=script
   ```
   Now `jeka hello` will work again.

!!! note "KBean Naming"
    A KBean named `MyCoolKBean` can be referenced as:
    - `org.example.MyCoolKBean` (Fully qualified)
    - `MyCoolKBean` (Short name)
    - `myCoolKBean` (Uncapitalized)
    - `myCool` (Omitting the 'KBean' suffix)

## Configure default values

You can override KBean fields directly from `jeka.properties`:
```properties
@script.name=Everybody
```
Now `jeka hello` prints `Hello Everybody !`.

## KBean Interaction

KBeans can interact with each other. This is how JeKa plugins and complex build scripts are structured.

1. Ensure `jeka.kbean.default=script` is set and remove the `@script.name` override.
2. In `Build.java`, override the `init()` method:
```java
public class Build extends KBean {
    @Override
    protected void init() {
        Script script = load(Script.class); // Get the singleton instance
        script.name = "Mates";           
    }
}
```
3. Execute `jeka script: hello build:`. 
   `Hello Mates !`
   
   The `build:` part in the command line forces the initialization of the `build` KBean, which then configures the `script` KBean.

To make `build` always initialize, add this to `jeka.properties`:
   ```properties
   @build=on
   ```

## Classpath KBeans

*Local* KBeans are in `jeka-src`. *Classpath* KBeans are pre-compiled and available in the JeKa classpath (like plugins).

Run `jeka --doc` to see all available KBeans, including standard ones like `admin`.
Example: `jeka admin: openHomeDir`

### Adding Plugins

Adding a plugin is as simple as adding its JAR to the classpath via `jeka.classpath` or `@JkDep`.

```properties
jeka.classpath=\
  dev.jeka:springboot-plugin  \
  dev.jeka:sonarqube-plugin \
  dev.jeka:openapi-plugin:0.11.0.1
```

!!! note
    If you omit the version for `dev.jeka` group dependencies, JeKa uses its own version.

### Example: Running Node.js via JeKa

You can even add KBeans dynamically from the CLI:
```bash
jeka -cp=dev.jeka:nodejs-plugin nodeJs: version="20.12.2" cmdLine="npx cowsay Hello JeKa" exec
```

This should display:
```text
       ___________
      < Hello JeKa >
       -----------
              \   ^__^
               \  (oo)\_______
                  (__)\       )\/\
                      ||----w |
                      ||     ||
```

---
**Next steps:** Learn how to [Build a Base Application](build-base.md) or a [Full Project](build-projects.md).
