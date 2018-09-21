# General

## Can build classes and code to build be hosted in separate projects ?
Of course yes. If you prefer your build code lives in a distinct project, you can create a Jerkar project in a sibling 
folder and mention where is located the project to build.

```
public class Build extends JkJavaProjectBuild {

   @Override
    protected void afterOptionsInjected() {
         Path projectPath = this.baseDir().resolve("../myProject");   // project to build lies in a sibling folder. 
         project().setSourceLayout(project().getSourceLayout().withBaseDir(projectPath));
         maker().setOutLayout(maker().getOutLayout().withOutputDir(this.outputDir())); // Force output files here
         ...
    }

```

# Compilation

## How can I choose the JDK used to compile ?

Jerkar uses the JDK it is running on to compile your production or test code. 
If your code must be build on a another JDK version, you can specify JDK path for different version. For such, just mention it as option.

```
jdk.6=c:/software/jdk6
jdk.7=c:/software/jdk7
...
```

As such, if one of your project source code is declared to be in a specific Java version, the relevant JDK version will be used to compile it.

## How can I use Eclipse compiler in Jerkar ?

Jerkar can use any JSR199 Java compiler to compile your Java code. Just set the compiler instance you need as :

```
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

@JkImport("org.eclipse.jdt.core.compiler:ecj:4.6.1")
public class Build extends JkJavaProjectBuild {
    ...
    maker().setCompiler(JkJavaCompiler.of(new EclipseCompiler()));
    ...
}
```






