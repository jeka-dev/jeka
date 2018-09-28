# Tool Part

## Lexical

The following terms are used all over the tool part section :

__[PROJECT DIR]__ : refers to the root folder of the project to build (the one where you would put pom.xml or build.xml file if you were using ANT or Maven).

__[JERKAR HOME]__ : refers to the folder where is intalled Jerkar. You should find _jerkar.bat_ and _jerkar_ shell scripts directly under this folder.

__[JERKAR USER HOME]__ : refers to the folder where Jerkar stores caches, binary repository and global user configuration.

<strong>Build Classes :</strong> Java source code containing build instructions. These files are edited by the users and are located under _[PROJECT DIR]/build/def_ directory.  
This term is also used to designate their compiled counterparts (.class files). 

<strong>Build Classpath :</strong> Classpath on which depends _build classes_ to get compiled and executed. It consists
in _Jerkar_ core classes but can be augmented with any third party lib or build classes located in another project.
  
<strong>Build Methods :</strong> Java methods member of _Build Classes_ invokable from Jerkar command line. These methods 
are hosted in classes extending `org.jerkar.tool.JkBuild` or `org.jerkar.tool.JkPlugin`. They must be public zero-args instance methods 
returning void. 
 
<strong>Options :</strong> This is a set of key-value used to inject parameters. Options can be mentioned as command line arguments, stored in specific files or be hard coded in build classes.


## In a Glance

The tool part of Jerkar consists in an engine able to run Java code as script (meaning directly callable from the command line). 
Generally this code is intended to build Java projects but it can be used for any purpose.

To be callable code must be wrapped in a class extending `org.jerkar.tool.JkBuild` or `org.jerkar.tool.JkPlugin`.

The code can be both **compiled jars** or **source files** (.java files).

In practice, you have a project structure respecting the following layout :

```
[Project Dir]
   |
   + build
      + def
         + MyBuild.java   <----- class extending JkBuild  
         + MyUtility.java
   + src
      + main
          + java
          + resources
   + ...
```

From __[Project Dir]__,  you can invoke any build method defined on `MyBuild` class. This class can be non-public.

For example, executing `jerkar myMethod1 myMethod2 -myParam1=foo` will instantiate a `MyBuild` instance, inject _"foo"_ in the `myParam1` field then invoke `myMethod1()` and `myMethod2()` in sequence.

A build class may look like :

```Java
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkImport;
import org.jerkar.tool.JkBuildd;
import com.google.common.base.MoreObjects;

@JkImport("commons-httpclient:commons-httpclient:3.1")  // <---- Build classes inside this project can use Guava and Http client libraries
@JkImport("com.google.guava:guava:21.0")
public class MyBuild extends JkBuild {    // <---- Callable build class (extending JkBuild)
    
    public String myParam1 = "myDefault";    // <----- Can be overrided with option

    @JkDoc("Performs some tasks using http client")    // <----- For self documentation purpose
    public void myMethod1() {                   // <----- Build method (callable from command line)
        HttpClient client = new HttpClient();
        GetMethod getMethod = new GetMethod("http://my.url/" + myParam1);
        ....
    }
    
    public void myMethod2() {   // <----- An other build method 
        MyUtility.soSomething();
        ...
    }

}
```

If your project does not supply any build class, then Jerkar will use`org.jerkar.tool.JkBuild`. In despite this class is
almost naked, you can perforl full java build using 'java' plugin as `jerkar clean java#pack`.

Executing `jerkar` or `jerkar help` command line displays all callable methods and options for the current _build class_.

The following chapters detail about how the mechanism works, what you can do and the limitations.

