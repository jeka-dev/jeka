# Lexical

These terms are used in this document, this short lexical disambiguates their meanings.

__[JERKAR HOME]__ : refers to the folder where _Jerkar_ is intalled. You should find _jerkar.bat_ and _jerkar_ shell files directly under this folder.

__[JERKAR USER HOME]__ : refers to the folder where Jerkar stores caches, binary repository and global user configuration.

__[USER HOME]__ : User Home within the meaning of Windows or Unix.


# Install Jerkar

1. unzip the [distribution archive](http://jerkar.github.io/binaries/jerkar-distrib.zip) to the directory you want to install Jerkar (_[JERKAR HOME]_)
2. make sure that either a valid JDK is on your _PATH_ environment variable or that a _JAVA_HOME_ variable is pointing on
3. add _[JERKAR HOME]_ to your _PATH_ environment variable
4. execute `jerkar help` in the command line. You should get an output starting by : 

```
Usage:
jerkar (method | pluginName#method) [-optionName=<value>] [-pluginName#optionName=<value>] [-DsystemPropName=value]

Execute the specified methods defined in run class or plugins using the specified options and ofSystem properties.
Ex: jerkar clean java#pack -java#pack.sources=true -LogVerbose -other=xxx -DmyProp=Xxxx
...
```

# Use Jerkar with command line

## Create a project

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

# Use with intellij

## setup intellij

As for Eclipse, you must declare the two path variables (go settings -> Apparence & behavior -> Path Variables)
 * `JERKAR_HOME` which point to _[Jerkar Home]_, 
 * `JERKAR_REPO` which point to _[Jerkar User Home]/cache/repo_

## setup iml file

Execute `jerkar intellij#generateIml` from project root folder to generate an iml file 
according the Build.java file.

## run/debug within Intellij

You can go two ways :
- Just execute your Build class main method.
- Create a Run/Debug application configuration for class `org.jerkar.tool.Main` class.

**Important :** Make sure you choose __$MODULE_DIR$__ as the working directory for the Run/Debug configuration.


# Use with Eclipse

## Setup Eclipse 

To use Jerkar within Eclipse, you just have to set 2 classpath variables in Eclipse.

1. Open the Eclipse preference window : _Window -> Preferences_
2. Navigate to the classpath variable panel : _Java -> Build Path -> Classpath Variables_
3. Add these 2 variables :
    * `JERKAR_HOME` which point to _[Jerkar Home]_, 
    * `JERKAR_REPO` which point to _[Jerkar User Home]/cache/repo_.
    
Note : By default _[Jerkar User Home]_ point to _[User Home]/.jerkar_ but can be overridden by defining the environment 
variable `JERKAR_USER_HOME`. 
  
If you have any problem to figure out the last value, just execute `jerkar help -LH` from anywhere and it will start logging the relevant information :

```
 _______    	   _
(_______)         | |                
     _ _____  ____| |  _ _____  ____ 
 _  | | ___ |/ ___) |_/ |____ |/ ___)
| |_| | ____| |   |  _ (/ ___ | |    
 \___/|_____)_|   |_| \_)_____|_|
                                     The 100% Java build tool.

Java Home : C:\UserTemp\I19451\software\jdk1.6.0_24\jre
Java Version : 1.6.0_24, Sun Microsystems Inc.
Jerkar Home : C:\software\jerkar                               <-- This is the value for JERKAR_HOME
Jerkar User Home : C:\users\djeang\.jerkar
Jerkar Repository Cache : C:\users\djeang\.jerkar\cache\repo   <-- This is the value for JERKAR_REPO
...
```

## setup _.classpath_ file

Execute `jerkar eclipse#generateFiles` from project root folder to generate a _.classpath_ file 
according the `Build.java` file.

## run/debug within Eclipse

You can go two ways :
- Just execute your Build class main method.
- Configure a launcher on `org.jerkar.tool.Main` class that has your project root dir as working directory. This way you 
can specify which method to execute along options and system properties.

