## General

### I'm behind a firewall that prevent me to access to Maven central, what should I do ?

Define the `jeka.repos.download` property in your USER_HOME/.jeka/global.properties file
Alternatively, you can define the JEKA_REPOS_DOWNLOAD environment variable.

See [here](https://jeka-dev.github.io/jeka/reference-guide/execution-engine-properties/#repositories) for more details.

### I'm behind a proxy, how should I configure Jeka ?

JeKa just leverages the standard Java mechanism to handle proxy. For example, You can :

- Set the `JAVA_TOOL_OPTIONS` environment variable as `-Dhttps.proxyHost=my.proxy.host -Dhttps.proxyPort=8888`
- Or specify proxy properties to the jeka command line, as :  `-Dhttps.proxyHost=my.proxy.host -Dhttps.proxyPort=8888`

See [here](https://stackoverflow.com/questions/120797/how-do-i-set-the-proxy-to-be-used-by-the-jvm) for more details on arguments.


### How can I migrate from Maven ?

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

### How can I generate Eclipse/Intellij without using ProjectKBean ?

Just make your _KBean_ class implement `JkJavaIdeSupport`.








