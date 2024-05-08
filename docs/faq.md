# Frequented Asked Questions

## General

### I'm behind a firewall that prevent to access to Maven Central, what can I do ?

If you cannot access to Maven Central repository for whatever reason, you should use 
a Maven repository that is accessible from your organisation. 

This [section](https://jeka-dev.github.io/jeka/reference-guide/execution-engine-properties/#repositories) explains how you can setup the upload repository globally or for a given project.

Alternatively, you can define the JEKA_REPOS_DOWNLOAD environment variable.

See [here](https://jeka-dev.github.io/jeka/reference-guide/execution-engine-properties/#repositories) for more details.

### I'm behind a proxy, how should I configure Jeka ?

JeKa just leverages the standard Java mechanism to handle proxy. For example, You can :

- Set the `JAVA_TOOL_OPTIONS` environment variable as `-Dhttps.proxyHost=my.proxy.host -Dhttps.proxyPort=8888`
- Or specify proxy properties to the jeka command line, as :  `-Dhttps.proxyHost=my.proxy.host -Dhttps.proxyPort=8888`

See [here](https://stackoverflow.com/questions/120797/how-do-i-set-the-proxy-to-be-used-by-the-jvm) for more details on arguments.

### How can I use Maven/Gradle in conjunction with JeKa ?

Nothing prevents to use JeKa in conjunction of Maven or Gradle in the same project,
except that in IDE, synchronisation may interfere between the 2 systems.

To avoid that, JeKa proposes a simple solution :

1. Creates the project in IDE as you normally do for Maven or Gradle
2. Execute : `jeka base: scaffold`. This generates the folder/file structure for JeKa
3. Add `@intellij.imlFile=jeka-src/.idea/jeka-src.iml` in *jeka.properties* file.
4. Execute : `jeka intellij: iml`. The iml file should be created under *jeka-src* folder.
5. Execute : `jeka intellij: initProject`. To create a module according the generated iml file.

!!! note
    'jeka-src' should now live in its own Intellij module. If your IDE does not reflect 
    this state after executing step 5, just close and re-open the project.

Now *jeka-src* live in its own IntelliJ module.
Simply execute `jeka intellij: iml`to sync JeKa without impacting Maven/Gradle.


!!! warning
    Do not remove the `@intellij.imlFile=jeka-src/.idea/jeka-src.iml` property from *jeka.property* file, otherwise you will
    still face sync issues.

### How can I migrate my project from Maven ?

_Jeka_ helps translate all dependencies declared in a _Maven_ project into the equivalent _Java_ code.

Assuming _Maven_ is already installed and there is a _pom.xml_ file at the root of the project, 
execute `jeka maven: showPomDeps` to display _Java_ code/configuration to 
copy-paste in a build class or *dependencies.txt* file.


## Compilation

### How can I use Eclipse compiler in Jeka ?

Jeka can use any JSR199 Java compiler to compile your Java code. Just set the compiler instance you need as :

```java
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

@JkInjectClasspath("org.eclipse.jdt.core.compiler:ecj:4.6.1")
public class Build extends JkBean {
    
    ...
    project.compilerToolChain.setCompileTool(new EclipseCompiler());
}
```

### How can I sync  Eclipse/Intellij without using ProjectKBean ?

`ProjectKBean` KBean provides IDE sync out-of-the-box but you may prefer to not use it.

Just let your KBean implements `JkJavaIdeSupport` and implement the unique method that is 
proving info necessary to generate IDE metadata file.

Then, for sync, just execute `jeka intellij: iml` as usual.








