## General

### Can def classes be hosted in a separate project than the build code ?
Yes. If you prefer for your Jeka code to lie in a distinct project, create a Jeka project in a sibling 
folder and mention where is located to the build project.

```java

Path projectPath = this.baseDir().resolve("../myProject");   
project.setBaseDir(projectPath);
      ...
```

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
    project.getConstruction()
            .getCompilation()
                .getCompiler()
                    .setCompilerTool(new EclipseCompiler());
}
```

### How can I generate Eclipse/Intellij without using ProjectJkBean ?

Just make your _KBean_ class implement `JkJavaIdeSupport`.








