# Tool Part

The tool part of Jerkar consists in an engine able to run Java code as script (meaning directly callable from the command line). 
Generally this code is intended to build Java project but it can used for any purpose.

To be callable code must be wrapped in a class extending `org.jerkar.tool.JkBuild` or `org.jerkar.tool.JkPlugin`.

The code can be both **compiled jar** or **source file** (.java files).

In practice, you have a project structure respecting the following schema :

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

For example, executing `jerkar myMethod1 myMethod2 -myParam1=foo` will instantiate a `MyBuild` instance, inject _"foo"_ in the `myParam1` field then will invoke `myMethod1()` and `myMethod2()` in sequence.

A build class may look like :

```
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkImport;
import org.jerkar.tool.JkBuildd;
import com.google.common.base.MoreObjects;

@JkImport("commons-httpclient:commons-httpclient:3.1")  <---- Build classes inside this project can use Guava and Http client libraries
@JkImport("com.google.guava:guava:21.0")
public class MyBuild extends JkBuild {    <---- Callable build class (extending JkBuild)
    
    public String myParam1 = "myDefault";    <----- Can be overrided with option

    @JkDoc("Performs some tasks using http client")    <----- For self documentation purpose
    public void myMethod1() {                    <----- Build method (callable from command line)
        HttpClient client = new HttpClient();
        GetMethod getMethod = new GetMethod("http://my.url/" + myParam1);
        ....
    }
    
    public void myMethod2() {   <----- An other build method 
        MyUtility.soSomething();
        ...
    }

}
```

The following chapters detail about how the mechanism works, what you can do and the limitations.

