# Frequented Asked Questions

## Network - Security

### My organization prevents access to Maven Central. What can I do?

You can [configure Maven repositories](reference/properties/#repositories) in a central place by editing the *[USER HOME]/.jeka/global.properties* file.

To fetch JeKa distributions, specify the `jeka.distrib.location` property, pointing to a URL file. This property is better placed in the *jeka.properties* file, as it may vary from one project to another.

### My organization prevents downloading JDKs. What can I do?

You can specify a local location for each JDK version you are using as follows:
```properties
jeka.jdk.11=/my/path/to/jdk11/home
jeka.jdk.17=/my/path/to/jdk17/home
...
```
This information can be stored in the project's jeka.properties file, in [USER HOME]/.jeka/global.properties, or passed as environment variables.

### I'm behind a proxy, how should I configure Jeka ?

JeKa just leverages the standard Java mechanism to handle proxy. For example, You can :

- Set the `JAVA_TOOL_OPTIONS` environment variable as `-Dhttps.proxyHost=my.proxy.host -Dhttps.proxyPort=8888`
- Or specify proxy properties to the jeka command line, as :  `-Dhttps.proxyHost=my.proxy.host -Dhttps.proxyPort=8888`

See [here](https://stackoverflow.com/questions/120797/how-do-i-set-the-proxy-to-be-used-by-the-jvm) for more details on arguments.

## Legacy Tools

### How can I use Maven/Gradle in conjunction with JeKa?

Nothing prevents using JeKa alongside Maven or Gradle in the same project, except that in an IDE, synchronization may interfere between the two systems.

To avoid this, the `jeka-src` folder should exist in its own IntelliJ module. JeKa provides a simple way to accomplish this.

**Using IntelliJ Plugin:**

Right-click on the `jeka-src` folder, and a menu entry will allow you to turn the `jeka-src` directory into a module.

**Using the Command Line:**

In your existing Maven/Gradle project, do the following:

1. Execute: `jeka base:scaffold`. This generates the folder/file structure for JeKa.
2. Execute: `jeka intellij:jekaSrcAsModule` in the root directory.

This will add the property `@intellij.imlFile=jeka-src/.idea/jeka-src.iml` to your *jeka.properties* file.

!!! note
The `jeka-src` folder should now exist in its own IntelliJ module. If your IDE does not reflect this change, close and reopen the project.

Now, the `jeka-src` folder is managed in its own IntelliJ module. Simply execute `jeka intellij:iml` to sync JeKa without affecting Maven/Gradle.

!!! warning
Do not remove the `@intellij.imlFile=jeka-src/.idea/jeka-src.iml` property from the *jeka.properties* file, or you may face synchronization issues.

### How can I migrate my project from Maven ?

_JeKa_ helps translating all dependencies declared in a _Maven_ project into the equivalent _Java_ code.

Assuming _Maven_ is already installed and there is a _pom.xml_ file at the root of the project, 
execute `jeka maven: showPomDeps` to display _Java_ code/configuration to 
copy-paste in a build class or *dependencies.txt* file.

## Performance - Caching

### How to cache downloaded dependencies in _dev.Github-actions ?

_JeKa_ caches downloaded dependencies (JDKs, JeKa distros, Maven artifacts, NodeJs exe,...) in a single 
directory at *[USER HOME]/.jeka/cache*.

When running as *_dev.Github Action* this directory is empty at the start of the build. We need to save/restore it in 
order to make it persist from one build to another.

For this, we can use [cache action](https://github.com/actions/cache) as follow:
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

## Misc

### How do I configure projects from code ?

If you want to configure a project programmatically, either within the project itself or to create a plugin, you should access the `JkProject` instance directly instead of using the `ProjectKBean`.

The `ProjectKBean` initializes the project and configures it with its own settings in its `init` method. After that, it should not be modified. If you change the `ProjectKBean` instance in your code, the underlying `JkProject` instance will already have been configured by the `ProjectKBean`, meaning your changes will have no effect.

```java

public class Build extends KBean {

    JkProject project = load(ProjectKBean.class).project;
    
    void init() {
        project.testing.testProcessor.engineBehavior.setProgressDisplayer(STEP);
        ...
    }
}
```

### How can I use Eclipse compiler in Jeka ?

Jeka can use any JSR199 Java compiler to compile your Java code. Just set the compiler instance you need as :

```java
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

@JkDep("org.eclipse.jdt.core.compiler:ecj:4.6.1")
public class Build extends KBean {

    JkProject project = load(ProjectKBean.class).project;
    ...
    project.compilerToolChain.setCompileTool(new EclipseCompiler());
    
    // You may pass additional options to the compiler
    project.compilation.addJavaCompilerOptions(...);
}
```
This works out-of-the-box when using Java 8. Using Java 11+ may require specific 
options to pass to the compiler.

### How can I sync Eclipse/IntelliJ without using `ProjectKBean`?

`ProjectKBean` and `BaseKBean` provide IDE synchronization out-of-the-box, but you may prefer not to use them.

If you use a different structure to build your project, simply let your `KBean` implement `JkJavaIdeSupport` and implement the required method to provide the information necessary to generate IDE metadata files.

For synchronization, just execute `jeka intellij:iml` as usual.









