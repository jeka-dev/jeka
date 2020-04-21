# Install Jeka

Jeka deals with 3 directories :

__[PROJECT DIR]__ : Refers to the root folder of the project to build (or to run commands on). This is where you would put pom.xml or build.xml files.

__[JEKA HOME]__ : Refers to the folder where Jeka is installed. You should find _jeka.bat_ and _jeka_ shell scripts at the root of this folder.

__[JEKA USER HOME]__ : Refers to the folder where Jeka stores caches, binary repository and global user configuration. By default it is located at [USER DIR]/.jeka.



Thank to the wrapper feature, you don't need to install Jeka on your machine to run it... except if you are 
creating projects from scratch. 
As it's what we are going to do in this tutorial, you need to get it properly installed. 

1. Download and unzip the lastest *core-x.x.x-distrib.zip* file found on [snapshot](https://oss.sonatype.org/content/repositories/snapshots/org/jeka/core/) or [release](https://repo1.maven.org/maven2/org/jeka/core/) repository to the directory you want to install Jeka. 
2. Add _JEKA HOME_ (repositoty where you have unzipped the distribution) to your PATH environment. 
3. Make sure that either a valid JDK is on your _PATH_ environment variable or that a _JAVA_HOME_ variable is pointing on (_JAVA_HOME_/bin/java must point on a java executable). 
   Note that you can choose a specific JDK instance to run Jeka without affecting _JAVA_HOME_ variable by setting _JEKA_JDK_ environment variable (_JEKA_JDK_/bin/java must point on a java executable). 
   Required Jdk version for running Jeka is 8 or more (tested until 12). Jdk 7 is no more supported. 
4. execute `jeka help` in the command line. You should get an output starting by : 

```
Usage:

jeka (method | pluginName#method) [-optionName=<value>] [-pluginName#optionName=<value>] [-DsystemPropertyName=value]

Executes the specified methods defined in commandSet class or plugins using the specified options and system properties.

Ex: jeka clean java#pack -java#pack.sources=true -LogVerbose -other=xxx -DmyProp=Xxxx
...
```
<div class="alert alert-primary" role="alert">
You can display Jeka metadata information by adding `-LH` (or `-LogHeaders`) to the command line. You should get the following output.
</div>

```
 _______     _
(_______)   | |
     _ _____| |  _ _____
 _  | | ___ | |_/ |____ |
| |_| | ____|  _ (/ ___ |
 \___/|_____)_| \_)_____|

                           The pure Java build tool.


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
Compile and initialise commandSet classes ...
│ Initializing class JkCommandSet at C:\Users\djeang\IdeaProjects\jeka ...
│ │ Run instance initialized with options []
│ └ Done in 57 milliseconds.
└ Done in 336 milliseconds.
Jeka commands are ready to be executed.
Method : help on JkCommandSet
Usage:
jeka (method | pluginName#method) [-optionName=<value>] [-pluginName#optionName=<value>] [-DsystemPropName=value]

Execute the specified methods defined in commandSet class or plugins using the specified options and system properties.
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

While a visual Jeka plugin for Intellij exists (a plugin to be installed on Intellij), we will ignore it in this tutorial. 
You can still work pretty well thanks to Intellij and Eclipse Jeka plugin whose generating IDE metadata files (.iml and .classpath).

But first you have to instruct your IDE where is located Jeka distribution and repositories.

## Intellij

Declare the 2 path variables (go settings -> Apparence & behavior -> Path Variables)
 * `JEKA_HOME` which point to _[Jeka Home]_, 
 * `JEKA_USER_HOME` which point to _[Jeka User Home]_

## Eclipse 
Declare the 2 classpath variables in Eclipse.

1. Open the Eclipse preference window : _Window -> Preferences_
2. Navigate to the classpath variable panel : _Java -> Build Path -> Classpath Variables_
3. Add these 2 variables :
    * `JEKA_HOME` which point to _[Jeka Home]_, 
    * `JEKA_USER_HOME` which point to _[Jeka User Home]_.
    
## Note 
By default _[Jeka User Home]_ point to _[User Home]/.jeka_ but can be overridden by defining the environment 
variable `JEKA_USER_HOME`. 

# Jeka concepts

__Def Classes :__ Java source files located under _[PROJECT DIR]/jeka/def_. They are compiled on the flight by Jeka when invoked from the command line.

__Def Classpath :__ Classpath on which depend _def classes_ to get compiled and run. 
By default, it consists in _Jeka_ core classes but it can be augmented with any third party lib or _def classpath_ coming 
from other Jeka projects. 

__CommandSet Classes :__ Classes extending `JkCommandSet`. Their public no-arg methods can be invoked from the command line 
 if they belong to _def classpath_. Their public properties can be set from the command line as well. 
 Also, _plugins_ are bound to _commandSet classes_. Typically, _def classes_  include one _commandSet class_.

__Command :__ Java method member of _commandSet class_ invokable from command line. 
They must be instance method (not static), public, zero-args and returning void. 
Every method verifying these constraints within a _commandSet class_ or a _plugin_  is considered as a _command_.
 
__Options :__ This is a set of key-value used to inject parameters. Options can be mentioned 
as command line arguments, stored in specific files or hard coded in _commandSet classes_. Options can be injected 
in _commandSet class_ or _plugin_ instance fields.

__Plugins :__ Classes extending `JkPlugin` and named as _JkPluginXxxxx_ where_Xxxxx_ is the name of the plugin. In short, a plugin 
add dynamically commands and options to the running _commandSet class_.

# Basic automation project

First, let's create a simple automation project that read content from url and display it on the console. 

## Create a project

1. Create the root directory of your project (here 'sample1').
2. Open a terminal/console and cd to *sample1* directory. Jeka should be always executed from the root of the project.
3. Execute `jeka scaffold#run intellij#` under this directory (replace `intellij#` by `eclipse#` if you're using Eclipse).  
This will generate a project skeleton as follow :
```
sample1
   + jeka             
      + def             <-----  Java code that build your project goes here
         + Build.java   
      + output          <---- Genererated files are supposed to lie here  
   + sample1.iml    <----- Intellij metadata containing project dependencies (At least dev.jeka.core)
```
4. Import the project in your IDE. Everything should be Ok, in particular *Build.java* should compile and execute within your IDE.

```java
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkInit;

class Commands extends JkCommandSet {

    public static void main(String[] args) {
        Commands commands = JkInit.instanceOf(Commands.class, args);
        commands.clean();
    }

}
```

## Add a command

Add the following method to the `Commands` class.

```java
import dev.jeka.core.api.utils.JkUtilsIO;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkInit;

import java.net.MalformedURLException;
import java.net.URL;

class Commands extends JkCommandSet {

    public void displayGoogle() throws MalformedURLException {
        String content = JkUtilsIO.read(new URL("https://www.google.com/"));
        System.out.println(content);
    }

    public static void main(String[] args) throws Exception {
        JkInit.instanceOf(Commands.class, args).displayGoogle();
    }

}

```
Open a  console/:terminal in _sample1_ directory and execute `jeka displayGoogle`. You should see the Google source displayed.

Execute `jeka help` and the output should mention your new method.

```
...
From class Commands :
  Methods :
    displayGoogle : No description available.

From class JkCommandSet :
  Methods :
    clean : Cleans the output directory.
    help : Displays all available methods and options defined for this commandSet class.
...
```

Any public instance method with no-args and returning `void` fits to be a _command_. You can call several _commands_ in a single row.

You can also launch/debug command directly from your IDE, using the *main* method. Note that for so, you must instantiate 
your _commandSet class_ using `JkInit.instanceOf`.

## Self document your method

Add the following annotation to the _command_.

```java
@JkDoc("Fetch Google page and display its source on the console.")
public void displayGoogle() throws MalformedURLException {
    String content = JkUtilsIO.read(new URL("https://www.google.com/"));
    System.out.println(content);
}
```

Execute `jeka help` and the output should mention documentation.
```
From class Build :
  Methods :
    displayGoogle : Fetch Google page and display its source on the console.
```

## Add an option (parameter)

May you like to see Google page source but you probably want to apply this method to any other url.

To make it configurable, just declare the url in a public field so its value can be injected from command line.

```java
class Commands extends JkCommandSet {

    @JkDoc("The url to display content.")   // Optional self documentation
    public String url = "https://www.google.com/";

    @JkDoc("Fetch Google page and display its source on the console.")
    public void displayContent() throws MalformedURLException {
        String content = JkUtilsIO.read(new URL(url));
        System.out.println(content);
    }

    public static void main(String[] args) throws Exception {
        JkInit.instanceOf(Commands.class, args).displayContent();
    }

}
```

Execute `jeka displayContent -url=https://github.com/github` and you should see the Github page source displayed.

If you execute `jeka help` you should see the url option mentioned.

```
...
From class Commands :
  Methods :
    displayContent : Fetch Google page and display its source on the console.
  Options :
    -url (String, default : https://www.google.com/) : The url to display content.
...
```

## Use 3rd party libs in your commandSet class

You can mention inline the external libraries you need to compile and execute your _command class_. For exemple, you main need *Apache HttpClient* library to perform some non basic HTTP tasks.

1. Annotate your class with modules or jar files you want to use.

```java
@JkDefClasspath("org.apache.httpcomponents:httpclient:jar:4.5.8")  // Can import files from Maven repos
@JkDefClasspath("../local_libs/my-utility.jar")   // or simply located locally
class Commands extends JkCommandSet {
   ...
}
```

2. Execute `jeka intellij#iml` or `jeka eclipse#files` to add properly the dependencies to your IDE (You may need to refresh it).

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

## Import a commandSet class from another Jeka project

Imagine that you want to want to reuse *displayContent* method from project _sample1_ in another project. Let's create a new _sample2_ project located in a sibling folder than _sample1_.

1. Execute `mkdir sample2` then `cd sample2` followed by `jeka scaffold#run intellij#` (or `jeka scaffold#run eclipse#`)
2. Rename sample2 _commandSet class_ 'Sample2Commands` to avoid name collision. Be careful to rename its filename as well unless Jeka will fail.
3. Add a field of type `JkCommandSet` annotated with `JkImportProject` and the relative path of _sample1_ as value.
 
```java
class Sample2Commands extends JkCommandSet {

    @JkDefClasspathProject("../sample1")
    private JkCommandSet sample1Commands;

    public void hello() throws MalformedURLException {
        System.out.println("Hello World");
    }
    
}
```
4. Execute `jeka intellij#iml` (or `jeka eclipse#files`) to add _sample1_ dependencies to your IDE. Now _Sampl2Commands_ can refer to the _commandSet class_ of _sample1_.

5. Replace _JkCommandSet_ Type by the _Commands_ type from _sample1_ and use it in method implementation.

```java
class Sample2Commands extends JkCommandSet {

    @JkDefClasspathProject("../sample1")
    private Commands sample1Commands;  // This class comes from sample1

    public void printUrlContent() throws MalformedURLException {
        System.out.println("Content of " + sample1Commands.url);
        sample1Commands.displayContent();
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

You can set directly the value of the url on the command line as option values are injected on all imported commands recursively.

`jeka printUrlContent -url=https://github.com/github` displays :

```
Content of https://fr.wikipedia.org
<!DOCTYPE html>
<html class="client-nojs" lang="en" dir="ltr">
<head>
<meta charset="UTF-8"/>
<title>Wikipedia, the free encyclopedia</title>
...
```

## Restrictions

Except that _commandSet classes_ must have the same name than their filename, there is not known restriction about what you can do with _commandSet classes_ or _def classes_. 
You can define as many classes as you want into def directory. Organise them within Java packages or not. 

# Work with plugins

Each _commandSet class_ instance acts as a registry for plugins. In turn, plugins can interact each other through this registry.

## Write your first plugin

Let's implement similar commandSet as previously but using plugins :

* Create a new project 'sample-plugins' as we did for 'sample1". 
* Remove all classes that has been created in 'jeka/def' : we don't need it for now.
* Create a the following class inside 'jeka/def' : it could lie in a package or not.

```
@JkDoc("Provide command and option to display url content on the console")
public class JkPluginWebReader extends JkPlugin {

    @JkDoc("The url to display content.")
    public String url = "https://www.google.com/";

    protected JkPluginWebReader(JkCommandSet commands) {
        super(commands);
    }

    @JkDoc("Display source cotent of the url option on the console.")
    public void displayContent() throws MalformedURLException {
        String content = JkUtilsIO.read(new URL(url));
        System.out.println(content);
    }

}
```

* Execute `jeka help` in the console at the root of the project. At the end of the output your plugin should be
 mentioned : 
 ```
 Available plugins in classpath : eclipse, eclipsePath, git, intellij, jacoco, java, pgp, pom, repo, scaffold, sonar, war, webReader.
```

Jeka has discovered automatically your plugin called 'webReader'. For this your plugin class must follow 3 requirements :
* be `public`
* extend `JkPlugin`
* having a name starting with 'JkPlugin' : plugin names are inferred from their class name

Now you can execute `jeka webReader#help` to display which options and commands are available on this plugin.

To execute `displayContent` command : `jeka webReader#displayContent -webReader#url=https://twitter.com`.

You don't need to have a _commandSet class_ defined in _jeka/def_. By default Jeka uses `dev.jeka.core.tool.JkCommandSet`.

## What did happened behind the scene ?

Mentioning `webReader#` on the command line has instantiated _JkPluginWebReader_ class, attaching it to the current _commandSet class_ instance.

Mentioning `webReader#displayContent -webReader#url=https://twitter.com`  has injected the url value and invoke 
`JkPluginWebReader#displayContent` method on the instance plugin. 
This mechanism is similar to options/commands existing on _commandSet class_.

## Configure plugins within the commandSet class

You may need to configure default options on plugins and invoke them from the _commandSet classes_ without specifying 
everything from the command line. 

For such create a _commandSet class_ within the same project and declare the plugin as below. In the _commandSet class_ constructor 
you can redefine plugin state as its default field values. 

```
public class MyCommands extends JkCommandSet {

    private final JkPluginWebReader webReaderPlugin = getPlugin(JkPluginWebReader.class);

    MyCommands() {
        webReaderPlugin.url = "https://twitter.com";  // define a new default url
    }

    public void show() throws MalformedURLException {
        webReaderPlugin.displayContent();
    }

}
```

Now, if you execute `jeka webReader#help` you'll notice that the default `url` value is now the one defined above.

You can also use `jeka show` as a shorthand for `jeka webReader#displayContent`.

Note : You can also configure plugin by overriding `JkCommandSet@setup` methods. In this case 
the values set here will override the ones provided by command line.

## Configure/Enhance a plugin from another one

It's quite common to have a plugin that acts as an enhancer of another one. For example _Jacoco_ plugin enhance _Java_ 
 plugin by adding test coverage when tests are run. 
 
Of course this kind of mechanism is possible because _Java_ plugin test feature has been designed to be extendable 
 but the idea is that a plugin can access or load any other plugins from its owning _commandSet class_. 
 
Let's modify the webReader plugin in order it can allow the url be modified from the outside.

```
public class JkPluginWebReader extends JkPlugin {

    @JkDoc("The url to display content.")
    public String url = "https://www.google.com/";

    private UnaryOperator<URL> urlTransformer = url -> url;

    protected JkPluginWebReader(JkCommandSet commandSet) {
        super(commandSet);
    }

    @JkDoc("Fetch Google page and display its source on the console.")
    public void displayContent() throws MalformedURLException {
        URL effectiveUrl = urlTransformer.apply(new URL(url));
        System.out.println("Reading content from " + effectiveUrl);
        String content = JkUtilsIO.read(effectiveUrl);
        System.out.println(content);
    }

    public void setUrlTransformer(UnaryOperator<URL> urlTransformer) {
        this.urlTransformer = urlTransformer;
    }

}
```

Now let's create a plugin HttpsIzer in _jeka/def_ that forces the webReader plugin to use _https_ protocol.

```
public class JkPluginHttpsIzer extends JkPlugin {

    protected JkPluginHttpsIzer(JkCommandSet commandSet) {
        super(commands);
    }

    @Override
    protected void activate() {
        boolean webReaderPresent = this.getCommandSet().getPlugins().hasLoaded(JkPluginWebReader.class);
        if (!webReaderPresent) {
            return;
        }
        UnaryOperator<URL> urlTransformer = url -> {
            try {
                return new URL("https", url.getHost(), url.getPort(), url.getFile());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        };
        JkPluginWebReader webReader = this.getCommandSet().getPlugins().get(JkPluginWebReader.class);
        webReader.setUrlTransformer(urlTransformer);
    }
}
```

Now, if you execute `jeka webReader#displayContent -webReader#url=http://jeka.dev httpsIzer#` console will display :
```
Reading content from https://jeka.dev
...
```

**Explanation:** Adding `httpIzer#` instantiates and binds httpIzer plugin to its owning _commandSet class_ instance. 
During this process, `JkPlugin#activate` method is invoked. By default this method does nothing but as we have 
overrode it in `JkPluginHttpsIzer`, it modifies the `JkPluginWebReader` plugin bound to the owning _commandSet_ instance.


## Reuse a plugin across projects

You can pack your plugin in a jar, publish it on a repository and reuse it any Jeka project. The prerequisite is to know 
about building a Java project, which is covered in the next section.

### Develop

For those just create a regular Java project and add Jeka as ***PROVIDED*** dependency. 
```
// Use same Jeka version both for building and compiling
project.addDependencies(JkDependencySet.of()
        .andFile(JkLocator.getJekaJarPath(), PROVIDED));
```

Code your plugin in _src/main/java_ as you will do for any regular Java project. Of course you an use 
any third party lib as you want but keep in mind that the more deps you have the more dependency issues 
you may face with other plugins.

## Pack

Execute `Java java#pack java#publish` to build your plugin project and deploy it to your default repository.


## Reuse

You have to import the jar containing the plugin you want to use.
As for any other jar, you can declare it in a `@JkDefClasspath` annotation.

```
@JkDefClasspath("org.myorg:myjekaplugin:0.1")
class MyBuid extends JkCommandSet {
}
```

If you don't have declared it in a _commandSet class_ `@JkDefClasspath` annotation you can still invoke it from 
the command line : `jeka @org.myorg:myjekaplugin:0.1 myPlugin#doSomething`.


# Build a Java project

Now let's start more complicated tasks as building a Java project. It involves compilation, testing, packaging, dependency resolution, releasing, ...
There's many option to handle it in Jeka :

* Use low level API (similar to ANT tasks)
* Use high level _JkJavaProject_ API
* Use Jeka Java Plugin

The one you choose is a matter of taste, flexibility, verbosity, re-usability and integration with existing tools.

## Build Java project using low-level API

This approach is quite similar to what developers do when they use ANT. It consists in writing explicit tasks to compile, make resources,
tests and produce jars using Jeka low-level API.

```Java
import dev.jeka.core.api.depmanagement.*;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.file.JkPathTreeSet;
import dev.jeka.core.api.file.JkResourceProcessor;
import dev.jeka.core.api.java.*;
import dev.jeka.core.api.java.testing.JkJavaTestClasses;
import dev.jeka.core.api.java.testing.JkUnit;
import dev.jeka.core.api.java.testing.JkUnit.JunitReportDetail;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkDefClasspath;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@JkDefClasspath("org.apache.httpcomponents:httpclient:jar:4.5.6")
public class AntStyleBuild extends JkCommandSet {

    Path src = getBaseDir().resolve("src/main/javaPlugin");
    Path buildDir = getBaseDir().resolve("build/output");
    Path classDir = getOutputDir().resolve("classes");
    Path jarFile = getOutputDir().resolve("jar/" + getBaseTree().getRoot().getFileName() + ".jar");
    JkClasspath classpath;
    Path reportDir = buildDir.resolve("junitRreport");

    public void doDefault() {
        clean();
        run();
    }

    @Override
    protected void setup() {
       JkResolveResult depResolution = JkDependencyResolver.ofParent(JkRepo.ofMavenCentral()).resolve(JkDependencySet.of()
               .and("org.hibernate:hibernate-entitymanager:jar:5.4.2.Final")
               .and("junit:junit:4.11", JkJavaDepScopes.TEST)
       );
       classpath = JkClasspath.of(getBaseTree().andMatching(true,"libs/**/*.jar").getFiles())
            .and(depResolution.getFiles());
    }

    public void compile() {
        JkJavaCompiler.ofJdk().compile(JkJavaCompileSpec.of()
                .setOutputDir(classDir)
                .setClasspath(classpath)
                .setSourceAndTargetVersion(JkJavaVersion.V8)
                .addSources(src));
        Map<String, String> varReplacement = new HashMap<>();
        varReplacement.put("${server.ip}", "123.211.11.0");
        JkResourceProcessor.ofParent(JkPathTreeSet.of(src)).addInterpolator("**/*.properties", varReplacement)
                .generateTo(classDir, Charset.forName("UTF-8"));
        JkPathTree.of(src).andMatching(false, "**/*.javaPlugin").copyTo(classDir);
    }

    public void jar() {
        compile();
        JkManifest.ofEmpty().addMainClass("RunClass").writeToStandardLocation(classDir);
        JkPathTree.of(classDir).zipTo(jarFile);
    }

    public void javadoc() {
        JkJavadocProcessor.of(JkPathTreeSet.of(src), buildDir.resolve("javadoc")).process();
    }

    public void run() {
        jar();
        JkJavaProcess.of().withWorkingDir(jarFile.getParent())
            .andClasspath(classpath)
            .runJarSync(jarFile);
    }

    public void cleanBuild() {
        clean();
        jar();
    }

    public void junit() {
        jar();
        JkUnit.of().withForking()
        .withReportDir(reportDir)
        .withReport(JunitReportDetail.FULL)
        .run(JkJavaTestClasses.of(
                classpath.andPrepending(jarFile),
                JkPathTree.of(classDir).andMatching(true, "**/*Test.class", "*Test.class") ));
    }

}
```

## Build Java project using high level API

Defining all tasks needed to build a project can be verbose and tedious. Therefore Jeka provides a higher level API
to build Java project. This API mainly consist in a `JkJavaProject` class defining the project structure, dependencies, 
artifacts it produces and how they are produced.

Basically, A Java project build definition can stand in one single instance of `JkJavaProject`. 
This class implements `JkArtifactProducer` which defines methods to produce artifacts as jar files, javadoc and any 
other files you want to produces from the project build.

To avoid bloating `JkJavaProject` class, methods are splitted between `JkJavaProject` itself and 
`JkJavaProjectMaker` classes. The first host methods relative to the project 'static' structure 
(Name, version, layout, dependencies, Java version, default compilation options, manifest) and *maker* hosts methods 
to produced/published artifacts (jar, source-jars, javadoc, ...).

The outstanding philosophy is that the *raison d'être* of a Java project is to produce and publish artifacts (artifact = file produced by a project build). 
The *maker* holds the artifacts to produce and how to produce them.
By default, the production of the main artifact implies unit testing.

The following example shows how to build 2 projects, one depending on the other. If you are only interested of building 
a standalone project, you can concentrate on the Foo project.



```Java
        // --------------- Foo Project build -------------------------------------------------

        JkVersionProvider versionProvider = JkVersionProvider.of()
                .and("com.google.guava:guava", "21.0")
                .and("junit:junit", "4.12");

        JkJavaProject fooProject = JkJavaProject.ofMavenLayout(this.getBaseDir().resolve("foo"));
        fooProject.setDependencies(JkDependencySet.of()
                .and("junit:junit", JkJavaDepScopes.TEST)
                .and("com.google.guava:guava")
                .withVersionProvider(versionProvider)
        );
        fooProject.getMaker().addJavadocArtifact();  // Generate Javadoc by default

        // --------------- Bar Project build -------------------------------------------------

        JkJavaProject barProject = JkJavaProject.ofMavenLayout(this.getBaseDir().resolve("bar"));
        fooProject.setDependencies(JkDependencySet.of()
                .and("junit:junit", JkJavaDepScopes.TEST)
                .and("com.sun.jersey:jersey-server:1.19.4")
                .and("com.google.guava:guava")
                .and(fooProject)  // Bar project depends of Foo
                .withVersionProvider(versionProvider)
        );
        barProject.getMaker().defineMainArtifactAsFatJar(true); // Produced jar will embed dependencies
        barProject.getMaker().clean().makeAllArtifacts();  // Creates Bar jar along Foo jar (if not already built)
```

## Build Java project using Jeka Java plugin.

The java plugin consists in holding a `JkJavaProject` instance with predefined methods that you can directly call from the command line.
It can also alter other loaded plugin instances in order they take in account of Java nature of the project.

Let's create a new project from scratch to illustrate it :

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

Below is the content of the generated build class. Guava and Junit are present only fo demo purpose. You can remove it safely and add 
any dependency you need.

```Java
import dev.jeka.core.api.depmanagement.JkDependencySet;
import dev.jeka.core.api.java.project.JkJavaProject;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.JkCommandSet;
import dev.jeka.core.tool.JkPluginJava;

import static dev.jeka.core.api.depmanagement.JkJavaDepScopes.*;

class Build extends JkCommandSet {

    final JkPluginJava java = getPlugin(JkPluginJava.class);

    /*
     * Configures plugins to be bound to this commandSet class. When this method is called, option
     * fields have already been injected from command line.
     */
    @Override
    protected void setup() {
        java.getProject()
            .getDependencyManagement()
                .addDependencies(JkDependencySet.of()
                    .and("com.google.guava:guava:21.0")
                    .and("junit:junit:4.13", TEST));
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

If you want to create jar along javadoc and sources without testing : 
just execute `jeka clean java#pack -java#test`.

Explanation '-' prefix means that you want to set an option value. For example `-java#pack.sources=false` means that 
`JkPluginJava.pack.sources` will be injected the `false` value.

You can also set it by default in the build class constructor :

```Java
    protected Build() {
        java.tests.skip = true;
    }
```

## Explore functions and options provided out-of-thebox

Execute `jeka help` to display all what you can do from the command line for the current project. As told on the help screen,
you can execute `jeka aGivenPluginName#help` to display help on a specific plugin. 
The list of available plugins on the Jeka classpath is displayed in help screen.


