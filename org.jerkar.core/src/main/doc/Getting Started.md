# Lexical

These terms are used in this document, this short lexical disambiguates their meanings.

__[JERKAR HOME]__ : refers to the folder where _Jerkar_ is intalled. You should find _jerkar.bat_ and _jerkar_ shell files directly under this folder.

__[JERKAR USER HOME]__ : refers to the folder where Jerkar stores caches, binary repository and global user configuration.

__[USER HOME]__ : User Home within the meaning of Windows or Unix.


# Install Jerkar

1. download and unzip the lastest *core-x.x.x-distrib.zip* file found on [snapshot repository](https://oss.sonatype.org/content/repositories/snapshots/org/jerkar/core/) or [release repository](https://repo1.maven.org/maven2/org/jerkar/core/) to the directory you want to install Jerkar (_[JERKAR HOME]_)
2. make sure that either a valid JDK is on your _PATH_ environment variable or that a _JAVA_HOME_ variable is pointing on
3. add _[JERKAR HOME]_ to your _PATH_ environment variable
4. For now, Jerkar only runs with JDK8 (though it can build Java project of any Java version). If your defaulf JDK version is not 8 or your _JAVA_HOME_ environment variable does not point to a JDK8 then add a _JERKAR_JDK_ environment variable pointing on a JDK8 (_JERKAR_JDK_/bin/java must point on a java execuable). 
5. execute `jerkar help` in the command line. You should get an output starting by : 

```
Usage:
jerkar (method | pluginName#method) [-optionName=<value>] [-pluginName#optionName=<value>] [-DsystemPropName=value]

Execute the specified methods defined in run class or plugins using the specified options and system properties.
Ex: jerkar clean java#pack -java#pack.sources=true -LogVerbose -other=xxx -DmyProp=Xxxx
...
```
**Tips:** You can display Jerkar metadata information by adding `-LH` (or `-LogHeaders`) to the command line. You should get the following output.
```
 _______           _
(_______)         | |
     _ _____  ____| |  _ _____  ____
 _  | | ___ |/ ___) |_/ |____ |/ ___)
| |_| | ____| |   |  _ (/ ___ | |
 \___/|_____)_|   |_| \_)_____|_|
                                     The 100% Java build tool.


Working Directory : C:\Users\djeang\IdeaProjects\jerkar
Java Home : C:\Program Files (x86)\Java\jdk1.8.0_121\jre
Java Version : 1.8.0_121, Oracle Corporation
Jerkar Version : null
Jerkar Home : C:\Users\djeang\IdeaProjects\jerkar\org.jerkar.core\jerkar\output\distrib
Jerkar User Home : C:\Users\djeang\.jerkar
Jerkar Run Repositories : [https://repo.maven.apache.org/maven2/, file:/C:/Users/djeang/.jerkar/maven-publish-dir/]
Jerkar Repository Cache : C:\Users\djeang\.jerkar\cache\repo
Jerkar Classpath : C:\Users\djeang\IdeaProjects\jerkar\org.jerkar.core\jerkar\output\distrib\org.jerkar.core.jar
Command Line : -LH help
Specified System Properties : none.
Standard Options : RunClass=null, LogVerbose=false, LogHeaders=true, LogMaxLength=230
Options :   LH=null  LML=230  jdk.9=C:/Program Files (x86)/Java/jdk9.0.1 jdk.10=C:/Program Files (x86)/Java/jdk10.0.2  repo.download.url=https://repo.maven.apache.org/maven2/
Compile and initialise run classes ...
│ Initializing class org.jerkar.tool.JkRun at C:\Users\djeang\IdeaProjects\jerkar ...
│ │ Run instance initialized with options []
│ └ Done in 57 milliseconds.
└ Done in 336 milliseconds.
Jerkar run is ready to start.
Method : help on org.jerkar.tool.JkRun
Usage:
jerkar (method | pluginName#method) [-optionName=<value>] [-pluginName#optionName=<value>] [-DsystemPropName=value]

Execute the specified methods defined in run class or plugins using the specified options and system properties.
Ex: jerkar clean java#pack -java#pack.sources=true -LogVerbose -other=xxx -DmyProp=Xxxx
...

Method help succeeded in 660 milliseconds.
  ______                                     _
 / _____)                                   | |
( (____  _   _  ____ ____ _____  ___  ___   | |
 \____ \| | | |/ ___) ___) ___ |/___)/___)  |_|
 _____) ) |_| ( (__( (___| ____|___ |___ |   _
(______/|____/ \____)____)_____|___/(___/   |_|

                                               Total run duration : 1.159 seconds.

```
# Configure your IDE

For now, there is no valuable Jerkar Visual Plugin for your IDE but you can work pretty well without thanks to Intellij and Eclipse Jerkar plugin whose generating IDE metadata files (.iml and .classpath among others).

But first you have to instruct your IDE where is located Jerkar distribution and repositories.

## Intellij

Declare the 2 path variables (go settings -> Apparence & behavior -> Path Variables)
 * `JERKAR_HOME` which point to _[Jerkar Home]_, 
 * `JERKAR_REPO` which point to _[Jerkar User Home]/cache/repo_

## Eclipse 
Declare the 2 classpath variables in Eclipse.

1. Open the Eclipse preference window : _Window -> Preferences_
2. Navigate to the classpath variable panel : _Java -> Build Path -> Classpath Variables_
3. Add these 2 variables :
    * `JERKAR_HOME` which point to _[Jerkar Home]_, 
    * `JERKAR_REPO` which point to _[Jerkar User Home]/cache/repo_.
    
## Note 
By default _[Jerkar User Home]_ point to _[User Home]/.jerkar_ but can be overridden by defining the environment 
variable `JERKAR_USER_HOME`. 


# Basic automation project

First, let's create a simple automation project that read content from url and display it on the console. 

## Create a project

1. Create the root directory of your project (here 'sample1').
2. Open a terminal/console and cd to *sample1* directory. Jerkar should be always executed from the root of the project.
3. Execute `jerkar scaffold#run intellij#` under this directory (replace `intellij#` by `eclipse#` if you're using eclipse)  
This will generate a project skeleton as follow :
```
sample1
   + .idea
       + sample1.iml    <----- Intellij metadata containing project dependencies (At least org.jerkar.core)
   + jerkar             
      + def             <-----  Java code that build your project goes here
         + Build.java   
      + output          <---- Genererated files are supposed to lie here  
```
4. Import the project in your IDE. Eveything should be Ok, in particular *Build.java* should compile and execute within your IDE.

```java
import org.jerkar.tool.JkRun;
import org.jerkar.tool.JkInit;

import java.net.MalformedURLException;
import java.net.URL;

class Build extends JkRun {

    public static void main(String[] args) throws Exception {
        Build build = JkInit.instanceOf(Build.class, args);
        build.clean();
    }

}
```

## Add a run method (method invokable from command line)

Add the following method to the Build java source.

```java
import org.jerkar.api.utils.JkUtilsIO;
import org.jerkar.tool.JkRun;
import org.jerkar.tool.JkInit;

import java.net.MalformedURLException;
import java.net.URL;

class Build extends JkRun {

    public void displayGoogle() throws MalformedURLException {
        String content = JkUtilsIO.read(new URL("https://www.google.com/"));
        System.out.println(content);
    }

    public static void main(String[] args) throws Exception {
        JkInit.instanceOf(Build.class, args).displayGoogle();
    }

}

```
Execute `jerkar displayGoogle` on a terminal and you should see the Google source displayed.

Execute `jerkar help` and the output should mention your new method.

```
...
From class Build :
  Methods :
    displayGoogle : No description available.

From class org.jerkar.tool.JkRun :
  Methods :
    clean : Cleans the output directory except the compiled run classes.
    help : Displays all available methods and options defined for this run class.
...
```

Any public instance method with no-args and returning `void` fits to be a run method.

You can also launch/debug the method directly from your IDE, using the *main* method.

## Self document your method

Add the following annotation to the newly created method.

```java
@JkDoc("Fetch Google page and display its source on the console.")
public void displayGoogle() throws MalformedURLException {
    String content = JkUtilsIO.read(new URL("https://www.google.com/"));
    System.out.println(content);
}
```

Execute `jerkar help` and the output should mention doculentation.
```
From class Build :
  Methods :
    displayGoogle : Fetch Google page and display its source on the console.
```

## Add an option (parameter)

May you like to see Google page source but you probably want to apply this method to any other url.

To make it parametrizable, just declare the url in a public field so its value can be injected from command line.

```java
class Build extends JkRun {

    @JkDoc("The url to display content.")   // Optional self documentation
    public String url = "https://www.google.com/";

    @JkDoc("Fetch Google page and display its source on the console.")
    public void displayContent() throws MalformedURLException {
        String content = JkUtilsIO.read(new URL(url));
        System.out.println(content);
    }

    public static void main(String[] args) throws Exception {
        JkInit.instanceOf(Build.class, args).displayContent();
    }

}
```

Execute `jerkar displayContent -url=https://github.com/github` and you should see the Github page source displayed.

If you execute `jerkar help` you should see the url option mentioned.

```
...
From class Build :
  Methods :
    displayContent : Fetch Google page and display its source on the console.
  Options :
    -url (String, default : https://www.google.com/) : The url to display content.
...
```

## Use 3rd party libs in your build class

You can mention inline the external library you need to compile and execute your build class. For exemple, you main need *Apache HttpClient* library to perform some non basic HTTP tasks.

1. Annotate your class with module you want to use. There can be many.

```java
@JkImport("org.apache.httpcomponents:httpclient:jar:4.5.8")  // Can import files from Maven repos
@JkImport("../local_libs/my-utility.jar")   // or simply located locally
class Build extends JkRun {
   ...
}
```

2. Execute `jerkar intellij#generateIml` or `jerkar eclipse#generateFiles` to add properly the dependencies to your IDE (You may need to refresh it).

3. You can add code depending on the imported libs

```java
import org.apache.http.client.methods.HttpPost;
...
public void post() {
    HttpPost httpPost = new HttpPost();
    httpPost.setHeader("content-type", "application/json");
    ...
}
```
Execute *post* method as usual : `jerkar post`.

## Import a Jerkar build from another project

Imagine that you want to want to reuse *displayContent* method from project _sample1_ in another Jerkar project named _sample2_. Let's create a new _sample2_ project located in a sibling folder than _sample1_.

1. Execute `mkdir sample2` then `cd sample2` followed by `jerkar scaffold#run intellij#` (or `jerkar scaffold#run eclipse#`)
2. Rename sample2 Build class 'Sample2Build` to avoid name collision. Be carefull, rename its filename as well unless Jerkar will fail.
3. Add add Field of type JkRUnn annoted with `JkImportProject` and the relative path of _sample1_ as value.
 
```java
class Sample2Build extends JkRun {

    @JkImportProject("../sample1")
    private JkRun project1Run;

    public void hello() throws MalformedURLException {
        System.out.println("Hello World");
    }
    
}
```
4. Execute `jerkar intellij#generateIml` (or `jerkar eclipse#generateFiles`) to add _sample1_ dependencies to your IDE. Now _Sampl2Build_ can refer to the _Build_ class of _sample1_.

5. Replace _JkRun_ Type by the _Build_ type from _sample1_ and use it in method implementation.

```java
class Sample2Build extends JkRun {

    @JkImportProject("../sample1")
    private Build project1Run;  // This Build come from sample1

    public void printUrlContent() throws MalformedURLException {
        System.out.println("Content of " + project1Run.url);
        project1Run.displayContent();
    }

}
```
Executing `jerkar printUrlContent` displays :

```
Content of https://www.google.com/
<!doctype html><html itemscope="" itemtype="http://schema.org/WebPage" lang="nl-BE"><head><meta content="text/html; charset=UTF-8" http-equiv="Content-Type"><meta content="/images/branding/googleg/1x/googleg_standard_color_128dp.pn
    g" itemprop="image"><title>Google</title><script nonce="JkJFrHNh1i7pdGGBGDk/tw==">(function(){window.google={kEI:'AyndXKnDGrLgkgW-kp7gAw',kEXPI:'0,1353747,57,1958,1640,782,698,527,731,223,1575,1257,1894,58,320,207,1017,167,438,
...
```

You can set directly the value of the url on the command line as option values are injected on all imported build recursively.

`jerkar printUrlContent -url=https://github.com/github` displays :

```
Content of https://fr.wikipedia.org
<!DOCTYPE html>
<html class="client-nojs" lang="fr" dir="ltr">
<head>
<meta charset="UTF-8"/>
<title>Wikipédia, l'encyclopédie libre</title>
...
```

## Restrictions

Except the following mentionned here, there is not known restriction about what you can do with you build class. You can define as meny class you want into def directoy. Organise it within Java packages or not. 

* Classes invoked from command lines must have the same name than their file.


# Build a Java project
Now let's start more complicated tasks as building a Java pojetc. It involves compilation, testing, packaging, dependency resolution, releasing, ...
There's many option to handle it in Jerkar :

* Use low level API (similar to ANT tasks)
* Use high level _JkJavaProject_ API
* Use Jerkar Java Plugin

The one you choose is a matter of taste, flexibility, verbosity, reusability and integration with existing tools.

## Build Java project using low-level API

_TODO_

## Build Java project using high level API

_TODO_

## Build Java project using Jerkar Java plugin.

1. Create the root directory of your project (here 'mygroup.myproject').
2. Execute `jerkar scaffold#run java#` under this directory. 
This will generate a project skeleton with the following build class at _[PROJECT DIR]/build/def/Build.java_

```
mygroup.myproject
   + jerkar             
      + def             <-----  Java code that build your project goes here
         + Build.java   
      + output          <---- Build artifacts are generated here
   + src
      + main
          + java        <----- Your project java sources and resources for production go here
      + test
          + java        <----- Your project java sources and resources for testing go here    
```
Explanation : `scaffold#run` invokes 'run' method on the 'scaffold' plugin.  `java#` forces the `java` plugin to be loaded. When loaded, 
'java' plugin has the effect to instruct scaffold plugin extra actions for generating a Java project.

By default the project mimics Maven layout convention so sources are supposed to lie in _src/main/java_.

Below is the content of the generated build class. Guava and Junit are pesent only fo demo purpose. You can remove it safely and add 
any dependency you need.

```Java
import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.java.project.JkJavaProject;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.JkRun;
import org.jerkar.tool.builtins.java.JkPluginJava;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.*;

class Build extends JkRun {

    final JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    /*
     * Configures plugins to be bound to this run class. When this method is called, option
     * fields have already been injected from command line.
     */
    @Override
    protected void setup() {
        JkJavaProject project = javaPlugin.getProject();
        project.addDependencies(dependencies());
    }

    private JkDependencySet dependencies() {  // Example of dependencies.
        return JkDependencySet.of()
                .and("com.google.guava:guava:21.0")
                .and("junit:junit:4.11", TEST);
    }

    public static void main(String[] args) {
        JkInit.instanceOf(Build.class, args).javaPlugin.clean().pack();
    }
    
}
```
Execute `jerkar java#info` to see an abstract of the project setup. 

## Build your project

1. Edit the Build.java source file above. For example, you can add compile dependencies.
2. Just execute `jerkar clean java#pack` under the project base directory. This will compile, run test and package your project in a jar file. You can also lauch the `main` method from your IDE.

## Extra function

If you want to create javadoc, jar sources and  jar tests or checksums : 
just execute `jerkar clean java#pack -java#pack.tests -java#pack.sources -java#pack.checksums=sha-256`.

Explanation  '-' prefix means that you want to set an option value. For example `-java#pack.sources` means that 
`JkPluginJava.pack.sources` will be injected the 'true' value.

You can also set it by default in the build class constructor :

```Java
    protected Build() {
        javaPlugin.pack.javadoc = true;
        javaPlugin.pack.sources = true;
        javaPlugin.pack.tests = true;
        javaPlugin.pack.checksums = "sha-256";
    }
```

## Explore functions and options provided out-of-thebox

Execute `jerkar help` to display all what you can do from the command line for the current project. As told on the help screen,
you can execute `jerkar aGivenPluginName#help` to display help on a specific plugin. 
The list of available plugins on the Jerkar classpath is displayed in help screen.


