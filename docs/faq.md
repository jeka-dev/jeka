# Frequently Asked Questions

## Network - Security

### My organization prevents access to Maven Central. What can I do?

You can [configure Maven repositories](reference/properties.md#repositories) in a central place by editing the `[USER HOME]/.jeka/global.properties` file.

```properties
jeka.repos.download=https://my.company/repo

# You can specify username/password
jeka.repos.download.username=myName
jeka.repos.download.password=myPassw0rd!

# ... or specify Authorization header to avoid password in clear
jeka.repos.download.headers.Authorization=Basic hKXhhtggjREfg4P=
```

To fetch JeKa distributions, specify the `jeka.distrib.location` property, pointing to a folder. 
This property is better placed in the `jeka.properties` file, as it may vary from one project to another.

### My organization prevents downloading JDKs. What can I do?

You can specify a local location for each JDK version you are using as follows:
```properties
jeka.jdk.11=/my/path/to/jdk11/home
jeka.jdk.17=/my/path/to/jdk17/home
...
```
This information can be stored in the project's `jeka.properties` file, in `[USER HOME]/.jeka/global.properties`, or passed as environment variables.

### I'm behind a proxy, how should I configure JeKa?

JeKa just leverages the standard Java mechanism to handle proxies. For example, you can:

- Set the `JAVA_TOOL_OPTIONS` environment variable to `-Dhttps.proxyHost=my.proxy.host -Dhttps.proxyPort=8888`.
- Or specify proxy properties to the JeKa command line, such as: `-Dhttps.proxyHost=my.proxy.host -Dhttps.proxyPort=8888`.

See [here](https://stackoverflow.com/questions/120797/how-do-i-set-the-proxy-to-be-used-by-the-jvm) for more details on arguments.

## Legacy Tools

### How can I use Maven/Gradle in conjunction with JeKa?

Nothing prevents using JeKa alongside Maven or Gradle in the same project, except that in an IDE, synchronization may interfere between the two systems.

To avoid this, the `jeka-src` folder should exist in its own IntelliJ module. JeKa provides a simple way to achieve this.

From an existing *Maven*/*Gradle* project, execute:
```bash
jeka base: scaffold
```
Edit the `jeka.properties` file, and add:
```properties
@intellij.splitModule=true
```
Generate the `.iml` file by synchronizing within the IDE, or by running:
```bash
jeka intellij: sync
```
In IntelliJ, go to `Project Settings` -> `Modules` -> `Import Module` -> choose `[project dir]/.idea/xxxx-jeka.iml`.


### How can I migrate my project from Maven?

*JeKa* helps translate all dependencies declared in a *Maven* project into the equivalent *Java* code.

Assuming *Maven* is already installed and there is a `pom.xml` file at the root of the project, 
execute `jeka maven: migrateDeps` to display the *Java* code/configuration to 
copy-paste into a build class or `dependencies.txt` file.

## Performance—Caching

### How to cache downloaded dependencies in GitHub Actions?

*JeKa* caches downloaded dependencies (JDKs, JeKa distributions, Maven artifacts, Node.js executables, etc.) in a single 
directory at `[USER HOME]/.jeka/cache`.

When running as a *GitHub Action*, this directory is empty at the start of the build. We need to save/restore it in 
order to make it persist from one build to another.

For this, we can use the [cache action](https://github.com/actions/cache) as follows:
```yaml
    - name: Restore JeKa cache
      uses: actions/cache/restore@v4
      with:
        path: ~/.jeka/cache
        key: ${{ runner.os }}

    - name: Run some JeKa commands
      run: "./jeka project: pack ..."
      
    - name: Save JeKa cache
      uses: actions/cache/save@v4
      with:
        path: ~/.jeka/cache
        key: ${{ runner.os }}
```

## Errors

### JUnit Platform

I see this error message when I launch tests. What can I do?
```text
OutputDirectoryProvider not available; probably due to unaligned versions of the junit-platform-engine and junit-platform-launcher jars on the classpath/module path.
```
You can explicitly declare the JUnit component versions in `dependencies.txt` as:
```ini
[test]
org.junit.platform:junit-platform-launcher:1.12.2
org.junit.jupiter:junit-jupiter:5.12.2
```

## Misc

### How do I configure projects from code?

If you want to configure a project programmatically, either within the project itself or to create a plugin, you should access the `JkProject` instance directly instead of using the `ProjectKBean`.

The `ProjectKBean` initializes the project and configures it with its own settings in its `init` method. After that, it should not be modified. If you change the `ProjectKBean` instance in your code, the underlying `JkProject` instance will already have been configured by the `ProjectKBean`, meaning your changes will have no effect.

```java
public class Build extends KBean {

    JkProject project = load(ProjectKBean.class).project;
    
    @Override
    protected void init() {
        project.testing.testProcessor.engineBehavior.setProgressDisplayer(STEP);
    }
}
```

### How can I use the Eclipse compiler in JeKa?

JeKa can use any JSR199 Java compiler to compile your Java code. Just set the compiler instance you need as follows:

```java
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

@JkDep("org.eclipse.jdt.core.compiler:ecj:4.6.1")
public class Build extends KBean {

    @Override
    protected void init() {
        JkProject project = load(ProjectKBean.class).project;
        project.compilerToolChain.setCompileTool(new EclipseCompiler());
        
        // You may pass additional options to the compiler
        project.compilation.addJavaCompilerOptions("-nowarn");
    }
}
```

### How can I sync Eclipse/IntelliJ without using `ProjectKBean`?

[`ProjectKBean`](reference/kbeans-project.md) and [`BaseKBean`](reference/kbeans-base.md) provide IDE synchronization out-of-the-box, but you may prefer not to use them.

If you use a different structure to build your project, simply let your `KBean` implement `JkJavaIdeSupport` and implement the required method to provide the information necessary to generate IDE metadata files.

For synchronization, just execute `jeka intellij: sync` as usual.









