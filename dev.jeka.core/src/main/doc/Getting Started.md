# Lexical

The following concepts are used all over the tool section :

__[PROJECT DIR]__ : Refers to the root folder of the project to build (or to run commands on). This is where you would put pom.xml or build.xml files.

__[JEKA HOME]__ : Refers to the folder where Jeka is installed. You should find _jeka.bat_ and _jeka_ shell scripts at the root of this folder.

__[JEKA USER HOME]__ : Refers to the folder where Jeka stores caches, binary repository and global user configuration. By default it is located at [USER DIR]/.jeka.

__Def Classes :__ Java source files located under _[PROJECT DIR]/jeka/def_. They are compiled on the flight by Jeka when invoked from the command line.

__Def Classpath :__ Classpath on which depends _def classes_ to get compiled and _command classes_ to be executed. 
By default, it consists in _Jeka_ core classes. it can be augmented with any third party lib or def Classpath coming 
from another project. 
Once _def classes_ sources have been compiled, _def Classpath_ is augmented with their _.class_ counterpart.

__Command Classes :__ Classes extending `JkCommands`. Their _commands_ can be invoked from the command line and 
their pubic fields set from the command line as well. Generally _def classes_ contains one _command class_ though there can be many or 
none. Command class can be a _def class_ but can also be imported from a library or external project.

__Commands :__ Java methods member of _command classes_ and invokable from Jeka command line. 
They must be instance method (not static), public, zero-args and returning void. Every method verifying these constraints is considered as a _command_.
 
__Options :__ This is a set of key-value used to inject parameters. Options can be mentioned 
as command line arguments, stored in specific files or hard coded in _command classes_.


# Install Jeka

1. Download and unzip the lastest *core-x.x.x-distrib.zip* file found on [snapshot](https://oss.sonatype.org/content/repositories/snapshots/org/jeka/core/) or [release](https://repo1.maven.org/maven2/org/jeka/core/) repository to the directory you want to install Jeka
2. Make sure that either a valid JDK is on your _PATH_ environment variable or that a _JAVA_HOME_ variable is pointing on.
   For now, Jeka only runs with JDK8 (though it can build Java project of any Java version). If your defaulf JDK version is not 8 or your _JAVA_HOME_ environment variable does not point to a JDK8 then add a _JEKA_JDK_ environment variable pointing on a JDK8 (_JEKA_JDK_/bin/java must point on a java execuable). 
3. Add _[JEKA HOME]_ to your _PATH_ environment variable
5. execute `jeka help` in the command line. You should get an output starting by : 

```
Usage:

jeka (method | pluginName#method) [-optionName=<value>] [-pluginName#optionName=<value>] [-DsystemPropertyName=value]

Executes the specified methods defined in command class or plugins using the specified options and system properties.

Ex: jeka clean java#pack -java#pack.sources=true -LogVerbose -other=xxx -DmyProp=Xxxx
...
```
<div class="alert alert-primary" role="alert">
You can display Jeka metadata information by adding `-LH` (or `-LogHeaders`) to the command line. You should get the following output.
</div>

```
 _______           _
(_______)         | |
     _ _____  ____| |  _ _____  ____
 _  | | ___ |/ ___) |_/ |____ |/ ___)
| |_| | ____| |   |  _ (/ ___ | |
 \___/|_____)_|   |_| \_)_____|_|
                                     The 100% Java build tool.


Working Directory : C:\Users\djeang\IdeaProjects\jeka
Java Home : C:\Program Files (x86)\Java\jdk1.8.0_121\jre
Java Version : 1.8.0_121, Oracle Corporation
Jeka Version : null
Jeka Home : C:\Users\djeang\IdeaProjects\jeka\dev.jeka.core\jeka\output\distrib
Jeka User Home : C:\Users\djeang\.jeka
Jeka Run Repositories : [https://repo.maven.apache.org/maven2/, file:/C:/Users/djeang/.jeka/maven-publish-dir/]
Jeka Repository Cache : C:\Users\djeang\.jeka\cache\repo
Jeka Classpath : C:\Users\djeang\IdeaProjects\jeka\dev.jeka.core\jeka\output\distrib\dev.jeka.jeka-core.jar
Command Line : -LH help
Specified System Properties : none.
Standard Options : RunClass=null, LogVerbose=false, LogHeaders=true, LogMaxLength=230
Options :   LH=null  LML=230  jdk.9=C:/Program Files (x86)/Java/jdk9.0.1 jdk.10=C:/Program Files (x86)/Java/jdk10.0.2  repo.download.url=https://repo.maven.apache.org/maven2/
Compile and initialise command classes ...
│ Initializing class JkCommands at C:\Users\djeang\IdeaProjects\jeka ...
│ │ Run instance initialized with options []
│ └ Done in 57 milliseconds.
└ Done in 336 milliseconds.
Jeka run is ready to start.
Method : help on JkCommands
Usage:
jeka (method | pluginName#method) [-optionName=<value>] [-pluginName#optionName=<value>] [-DsystemPropName=value]

Execute the specified methods defined in command class or plugins using the specified options and system properties.
Ex: jeka clean java#pack -java#pack.sources=true -LogVerbose -other=xxx -DmyProp=Xxxx
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

For now, there is no valuable Jeka Visual Plugin for your IDE but you can work pretty well without thanks to Intellij and Eclipse Jeka plugin whose generating IDE metadata files (.iml and .classpath).

But first you have to instruct your IDE where is located Jeka distribution and repositories.

## Intellij

Declare the 2 path variables (go settings -> Apparence & behavior -> Path Variables)
 * `JEKA_HOME` which point to _[Jeka Home]_, 
 * `JEKA_REPO` which point to _[Jeka User Home]/cache/repo_

## Eclipse 
Declare the 2 classpath variables in Eclipse.

1. Open the Eclipse preference window : _Window -> Preferences_
2. Navigate to the classpath variable panel : _Java -> Build Path -> Classpath Variables_
3. Add these 2 variables :
    * `JEKA_HOME` which point to _[Jeka Home]_, 
    * `JEKA_REPO` which point to _[Jeka User Home]/cache/repo_.
    
## Note 
By default _[Jeka User Home]_ point to _[User Home]/.jeka_ but can be overridden by defining the environment 
variable `JEKA_USER_HOME`. 


# Basic automation project

First, let's create a simple automation project that read content from url and display it on the console. 

## Create a project

1. Create the root directory of your project (here 'sample1').
2. Open a terminal/console and cd to *sample1* directory. Jeka should be always executed from the root of the project.
3. Execute `jeka scaffold#run intellij#` under this directory (replace `intellij#` by `eclipse#` if you're using Eclipse).  
This will generate a project skeleton as follow :
```
sample1
   + .idea
       + sample1.iml    <----- Intellij metadata containing project dependencies (At least dev.jeka.core)
   + jeka             
      + def             <-----  Java code that build your project goes here
         + Build.java   
      + output          <---- Genererated files are supposed to lie here  
```
4. Import the project in your IDE. Eveything should be Ok, in particular *Build.java* should compile and execute within your IDE.

```java
import dev.jeka.core.tool.JkCommands;
import dev.jeka.core.tool.JkInit;

class Build extends JkCommands {

    public static void main(String[] args) throws Exception {
        Build build = JkInit.instanceOf(Build.class, args);
        build.clean();
    }

}
```

## Add a run method (method invokable from command line)

Add the following method to the Build java source.

```java
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.tool.JkCommands;
import dev.jeka.core.tool.JkInit;

import java.net.MalformedURLException;
import java.net.URL;

class Build extends JkCommands {

    public void displayGoogle() throws MalformedURLException {
        String content = JkUtilsIO.read(new URL("https://www.google.com/"));
        System.out.println(content);
    }

    public static void main(String[] args) throws Exception {
        JkInit.instanceOf(Build.class, args).displayGoogle();
    }

}

```
Open a  console/:terminal in _sample1_ directory and execute `jeka displayGoogle`. You should see the Google source displayed.

Execute `jeka help` and the output should mention your new method.

```
...
From class Build :
  Methods :
    displayGoogle : No description available.

From class JkCommands :
  Methods :
    clean : Cleans the output directory except the compiled command classes.
    help : Displays all available methods and options defined for this command class.
...
```

Any public instance method with no-args and returning `void` fits to be a run method. You can call several run methods in a single row.

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

Execute `jeka help` and the output should mention doculentation.
```
From class Build :
  Methods :
    displayGoogle : Fetch Google page and display its source on the console.
```

## Add an option (parameter)

May you like to see Google page source but you probably want to apply this method to any other url.

To make it parametrizable, just declare the url in a public field so its value can be injected from command line.

```java
class Build extends JkCommands {

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

Execute `jeka displayContent -url=https://github.com/github` and you should see the Github page source displayed.

If you execute `jeka help` you should see the url option mentioned.

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

1. Annotate your class with modules or jar files you want to use.

```java
@JkImport("org.apache.httpcomponents:httpclient:jar:4.5.8")  // Can import files from Maven repos
@JkImport("../local_libs/my-utility.jar")   // or simply located locally
class Build extends JkCommands {
   ...
}
```

2. Execute `jeka intellij#generateIml` or `jeka eclipse#generateFiles` to add properly the dependencies to your IDE (You may need to refresh it).

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
Execute *post* method as usual : `jeka post`.

## Import a Jeka build from another project

Imagine that you want to want to reuse *displayContent* method from project _sample1_ in another Jeka project named _sample2_. Let's create a new _sample2_ project located in a sibling folder than _sample1_.

1. Execute `mkdir sample2` then `cd sample2` followed by `jeka scaffold#run intellij#` (or `jeka scaffold#run eclipse#`)
2. Rename sample2 _Build_ class 'Sample2Build` to avoid name collision. Be carefull, rename its filename as well unless Jeka will fail.
3. Add a field of type JkCommands annotated with `JkImportProject` and the relative path of _sample1_ as value.
 
```java
class Sample2Build extends JkCommands {

    @JkImportProject("../sample1")
    private JkCommands project1Run;

    public void hello() throws MalformedURLException {
        System.out.println("Hello World");
    }
    
}
```
4. Execute `jeka intellij#generateIml` (or `jeka eclipse#generateFiles`) to add _sample1_ dependencies to your IDE. Now _Sampl2Build_ can refer to the _Build_ class of _sample1_.

5. Replace _JkCommands_ Type by the _Build_ type from _sample1_ and use it in method implementation.

```java
class Sample2Build extends JkCommands {

    @JkImportProject("../sample1")
    private Build project1Run;  // This Build come from sample1

    public void printUrlContent() throws MalformedURLException {
        System.out.println("Content of " + project1Run.url);
        project1Run.displayContent();
    }

}
```
Executing `jeka printUrlContent` displays :

```
Content of https://www.google.com/
<!doctype html><html itemscope="" itemtype="http://schema.org/WebPage" lang="nl-BE"><head><meta content="text/html; charset=UTF-8" http-equiv="Content-Type"><meta content="/images/branding/googleg/1x/googleg_standard_color_128dp.pn
    g" itemprop="image"><title>Google</title><script nonce="JkJFrHNh1i7pdGGBGDk/tw==">(function(){window.google={kEI:'AyndXKnDGrLgkgW-kp7gAw',kEXPI:'0,1353747,57,1958,1640,782,698,527,731,223,1575,1257,1894,58,320,207,1017,167,438,
...
```

You can set directly the value of the url on the command line as option values are injected on all imported build recursively.

`jeka printUrlContent -url=https://github.com/github` displays :

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

Except the following mentioned below, there is not known restriction about what you can do with you build class. You can define as many classes as you want into def directoy. Organise them within Java packages or not. 

* Classes invoked from command lines must have the same name than their file.


# Build a Java project
Now let's start more complicated tasks as building a Java poject. It involves compilation, testing, packaging, dependency resolution, releasing, ...
There's many option to handle it in Jeka :

* Use low level API (similar to ANT tasks)
* Use high level _JkJavaProject_ API
* Use Jeka Java Plugin

The one you choose is a matter of taste, flexibility, verbosity, reusability and integration with existing tools.

## Build Java project using low-level API

_TODO_

## Build Java project using high level API

_TODO_

## Build Java project using Jeka Java plugin.

1. Create the root directory of your project (here 'mygroup.myproject').
2. Execute `jeka scaffold#run java#` under this directory. 
This will generate a project skeleton with the following build class at _[PROJECT DIR]/build/def/Build.java_

```
mygroup.myproject
   + jeka             
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
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkCommands;
import dev.jeka.core.tool.JkPluginJava;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.*;

class Build extends JkCommands {

    final JkPluginJava javaPlugin = getPlugin(JkPluginJava.class);

    /*
     * Configures plugins to be bound to this command class. When this method is called, option
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
Execute `jeka java#info` to see an abstract of the project setup. 

## Build your project

1. Edit the Build.java source file above. For example, you can add compile dependencies.
2. Just execute `jeka clean java#pack` under the project base directory. This will compile, run test and package your project in a jar file. You can also lauch the `main` method from your IDE.

## Extra function

If you want to create javadoc, jar sources and  jar tests or checksums : 
just execute `jeka clean java#pack -java#pack.tests -java#pack.sources -java#pack.checksums=sha-256`.

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

Execute `jeka help` to display all what you can do from the command line for the current project. As told on the help screen,
you can execute `jeka aGivenPluginName#help` to display help on a specific plugin. 
The list of available plugins on the Jeka classpath is displayed in help screen.


