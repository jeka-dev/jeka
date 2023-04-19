## General

### I'm behind a firewall that prevent me to access to Maven central, what should I do ?

Define the `jeka.repos.download` property in your USER_HOME/.jeka/global.properties file
Alternatively, you can define the JEKA_REPOS_DOWNLOAD environment variable.

See [here](https://jeka-dev.github.io/jeka/reference-guide/execution-engine-properties/#repositories) for more details.

### I'm behind a proxy, how should I configure Jeka ?

JeKa just leverage the standard Java mechanism to handle proxy. For example, You can :
* Set the `JAVA_TOOL_OPTIONS` environment variable as `-Dhttps.proxyHost=my.proxy.host -Dhttps.proxyPort=8888`
* Or specify proxy properties to the jeka command line, as :  `-Dhttps.proxyHost=my.proxy.host -Dhttps.proxyPort=8888`

See [here](https://stackoverflow.com/questions/120797/how-do-i-set-the-proxy-to-be-used-by-the-jvm) for more details on arguments.

### Can def classes be hosted in a separate project than the build code ?

Yes. If you prefer for your Jeka code to lie in a distinct project, create a Jeka project in a sibling 
folder and mention where is located to the build project.

```java

Path projectPath = this.baseDir().resolve("../myProject");   
project.setBaseDir(projectPath);
      ...
```

### How can I use Maven or Gradle in conjunction with JeKa in Intellij ?

Maven, Gradle and other build tools manage the intellij dependencies integration by their own.
This means that JeKa can interfere with this tool by generating .iml files in the module supposed by this tool.

The solution consist in creating an intellij module at [myproject]/jeka location. For this :
- Add `intellij#dedicatedJekaModule=true` in file [myproject]/jeka/local.properties.
- Run `jeka intellij#iml` in working dir [myproject] : this will generate a [myproject]/jeka/myproject-jeka.iml file
- Go to menu **Project structure..."
  - Edit [myproject] module to remove *jeka/def* from *Test Source Folders*
  - Import new module by selecting [myproject]/jeka/myproject-jeka.iml

That's it. You can now work with Jeka as usual, still using [myproject] as working dir.

### My JkClass does not compile, so I can't invoke any Jeka method as 'scaffold#run'. What can I do ?

Use `-dci` option in command line.

### How can I migrate from Maven ?

_Jeka_ helps translate all dependencies declared in a _Maven_ project into the equivalent _Java_ code.

Assuming _Maven_ is already installed and there is a _pom.xml_ file at the root of the project, 
execute `jeka maven#migrateToCode` or `jeka maven#migrateToDependencies.txt` to display _Java_ code/configuration to 
copy-paste in a build class or *dependencies.txt* file..


## Compilation

### How can I choose the JDK used to compile ?

Jeka uses the JDK it is running on to compile production or test code. 
If code must be compiled on a another JDK version, you can specify JDK path for different version.
Simply mention it as option, for example in your _[JEKA HOME]/options.properties_ file.

```
jeka.jdk.9=/software/jdk9
```

This way, if one of your project source code is declared to be in a specific Java version, the relevant JDK version will be picked up automatically.

### How can I use Eclipse compiler in Jeka ?

Jeka can use any JSR199 Java compiler to compile your Java code. Just set the compiler instance you need as :

```java
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

@JkInjectClasspath("org.eclipse.jdt.core.compiler:ecj:4.6.1")
public class Build extends JkBean {
    
    ...
    project.compilation.compiler.setCompilerTool(new EclipseCompiler());
}
```

### How can I generate Eclipse/Intellij without using ProjectJkBean ?

Just make your _KBean_ class implement `JkJavaIdeSupport`.








