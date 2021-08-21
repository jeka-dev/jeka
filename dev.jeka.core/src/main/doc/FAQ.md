# General

### Can def classes be hosted in separate project than the code to build ?
Yes. If you prefer that your Jeka code lies in a distinct project, create a Jeka project in a sibling 
folder and mention where is located the project to build.

```java
...
@Override
protected void setup() {  // project to build lies in a sibling folder. 
      Path projectPath = this.baseDir().resolve("../myProject");   
      java.getProject().simpleFacade().setBaseDir(projectPath);
      ...
```

### My JkClass does not compile so I can't invoke any Jeka method as 'help' or 'scaffold#run'. What can I do ?

You can specify a built-in commandSet class to run. This way, compilation won't occur.
For example `jeka -JKC` or `jeka -JKC scaffold#run java#"`.

# Compilation

### How can I choose the JDK used to compile ?

Jeka uses the JDK it is running on to compile production or test code. 
If code must be compiled on a another JDK version, you can specify JDK path for different version.
Just mention it as option, for example in your _[JEKA HOME]/options.properties_ file.

```
jdk.9=/software/jdk9
...
```

This way, if one of your project source code is declared to be in a specific Java version, the relevant JDK version will be picked up automatically.

### How can I use Eclipse compiler in Jeka ?

Jeka can use any JSR199 Java compiler to compile your Java code. Just set the compiler instance you need as :

```java
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

@JkDefClasspath("org.eclipse.jdt.core.compiler:ecj:4.6.1")
public class Build extends JkClass{
    java.getProject().getConstruction().getCompilation()
             .getCompiler().setCompilerTool(new EclipseCompiler());
}
```

### How can I generate Eclipse/Intellij without using Java plugin (JkPluginJava) ?

Just make your `JkClass class implements` implements `dev.jeka.core.api.java.project.JkJavaIdeSupport`.








