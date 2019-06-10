# General

### Can command classes and code to build be hosted in separate projects ?
Of course yes. If you prefer that your Jeka code lies in a distinct project, create a Jeka project in a sibling 
folder and mention where is located the project to build.

```java
...
@Override
protected void setup() {  // project to build lies in a sibling folder. 
      Path projectPath = this.baseDir().resolve("../myProject");   
      project().setSourceLayout(JkProjectSourceLayout.ofMavenStyle().withBaseDir(projectPath));
      ...
```

### My command class does not compile so I can't invoke any Jeka method as 'help' or 'scaffold#run'. What can I do ?

You can specify a built-in command class to run. This way, compilation won't occur.
For example `jeka -CommandClass=JkCommandshelp` or `jeka -CC=JkCommands scaffold#run java#"`.

# Compilation

### How can I choose the JDK used to compile ?

Jeka uses the JDK it is running on to compile production or test code. 
If code must be build on a another JDK version, you can specify JDK path for different version.
Just mention it as option, for example in your _[JEKA HOME]/options.properties_ file.

```
jdk.6=/software/jdk6
jdk.7=/software/jdk7
jdk.9=/software/jdk9
...
```

This way, if one of your project source code is declared to be in a specific Java version, the relevant JDK version will be used automatically to compile it.

### How can I use Eclipse compiler in Jeka ?

Jeka can use any JSR199 Java compiler to compile your Java code. Just set the compiler instance you need as :

```java
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

@JkImport("org.eclipse.jdt.core.compiler:ecj:4.6.1")
public class Build extends JkCommands{
    ...
    maker().setCompiler(JkJavaCompiler.of(new EclipseCompiler()));
    ...
}
```






