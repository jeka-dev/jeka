# Getting Started
------------------

## Install Jerkar

1. unzip the [distribution archive](http://jerkar.github.io/binaries/jerkar-distrib.zip) to the directory you want to install Jerkar : let's call it _[Jerkar Home]_
2. make sure that either a valid *JDK* is on your _PATH_ environment variable or that a _JAVA_HOME_ variable is pointing on
3. add _[Jerkar Home]_ to your _PATH_ environment variable
4. execute `jerkar -LH help` in the command line. You should get an output starting by : 

<pre><code>
 _______           _
(_______)         | |
     _ _____  ____| |  _ _____  ____
 _  | | ___ |/ ___) |_/ |____ |/ ___)
| |_| | ____| |   |  _ (/ ___ | |
 \___/|_____)_|   |_| \_)_____|_|
                                     The 100% Java build tool.
Java Home : C:\Program Files (x86)\Java\jdk1.8.0_121\jre
Java Version : 1.8.0_121, Oracle Corporation
Jerkar Home : C:\software\jerkar                             
Jerkar User Home : C:\users\djeang\.jerkar
</code></pre>

Note : -LH option stands for "Log Headers". In this mode, Jerkar displays meta-information about 
the running build.

## Use Jerkar with command line

Let's see first how we can use Jerkar using command line.
You can `jerkar help` to display the the command-line interface help. 

### Create a project

1. Create a new directory named 'mygroup.myproject' as the root of your project.
2. Execute `jerkar scaffold#run` under this directory. 
This will generate a project skeleton with the following build class at _[PROJECT DIR]/build/def/Build.java_

<pre><code>import org.jerkar.api.depmanagement.JkDependencySet;
import org.jerkar.api.java.JkJavaVersion;
import org.jerkar.tool.JkInit;
import org.jerkar.tool.builtins.java.JkJavaProjectBuild;

import static org.jerkar.api.depmanagement.JkJavaDepScopes.*;

/**
 * @formatter:off
 */
class Build extends JkJavaProjectBuild {

    /*
     * Configures default values for option fields of this class. When this method is called, build option
     * fields have not been populated yet.
     */
    @Override
    protected void setupOptionDefaults() {
        java().projectVersion = "0.0.1-SNAPSHOT";
    }

    /*
     * Configures plugins to be bound to this build class. When this method is called, build option
     * fields have already been populated.
     */
    @Override
    protected void configurePlugins() {
        project()   // Configure project structure and dependencies using project() instance.
                .setVersionedModule("mygroup:myproject", java().projectVersion)
                .setSourceVersion(JkJavaVersion.V8)
                .setDependencies(dependencies());

        maker()    // Configure how project should be build using maker() instance.
                .setJuniter(maker().getJuniter().forked(true));
    }

    private JkDependencySet dependencies() {  // Example of dependencies.
        return JkDependencySet.of()
                .and("com.google.guava:guava:21.0")
                .and("junit:junit:4.11", TEST);
    }

    public static void main(String[] args) {
        JkInit.instanceOf(Build.class, args).doDefault();
    }

}</code></pre>

You can execute `jerkar java#info` to see your project setup.

### Build your project

1. Edit the Build.java source file above. For example, you can add compile dependencies.
2. Just execute `jerkar` under the project base directory. This will compile, run test and package your project in a jar file.
3. If you need to do more specific things, execute `jerkar java#help` to see which possibilities Java plugin offers.

### Explore functions

Execute `jerkar help` to display all what you can do from the command line for the current project.

## Use with Eclipse

### Setup Eclipse 

To use Jerkar within Eclipse, you just have to set 2 variables in Order Eclipse find Jerkar binaries and your local 
repository hosting dependencies.

1. Open the Eclipse preference window : _Window -> Preferences_
2. Navigate to the classpath variable panel : _Java -> Build Path -> Classpath Variables_
3. Add these 2 variables :
    * `JERKAR_HOME` which point to _[Jerkar Home]_, 
    * `JERKAR_REPO` which point to _[Jerkar User Home]_/.jerkar/cache/repo_.
    
Note : By default _[Jerkar User Home]_ point to _[User Home]/.jerkar_ but can be overridden by defining the environment 
variable `JERKAR_USER_HOME`. 
  
  
If you have any problem to figure out the last value, just execute `jerkar -LH` from anywhere and it will start logging the relevant information :

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

### setup _.classpath_ file

Execute `jerkar eclipse#generateFiles` from project root folder to generate a _.classpath_ file 
according the Build.java file.

### run/debug within Eclipse

You can go two ways :
- Just execute your Build class main method.
- Configure a launcher on `org.jerkar.tool.Main` class that has your project root dir as working directory.


## Use with intellij

### setup intellij

As for Eclipse, you must declare the two path variable (go settings -> Apparence & behavior -> Path Variables)
 * `JERKAR_HOME` which point to _[Jerkar Home]_, 
 * `JERKAR_REPO` which point to _[Jerkar User Home]_/.jerkar/cache/repo_

### setup iml file

Execute `jerkar intellij#generateIml` from project root folder to generate an iml file 
according the Build.java file.

### run/debug within Intellij

You can go two ways :
- Just execute your Build class main method.
- Create a Run/Debug application configuration for class `org.jerkar.tool.Main` class.

**Important :** Make sure you choose __$MODULE_DIR$__ as the working directory for the Run/Debug configuration.
