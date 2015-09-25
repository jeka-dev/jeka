# Tutorial
------------------

## Install Jerkar

1. unzip the [distribution archive](http://jerkar.github.io/binaries/jerkar-distrib.zip) to the directory you want to install Jerkar : let's call it _[Jerkar Home]_
2. make sure that either a valid *JDK* (6 or above) is on your _PATH_ environment variable or that a _JAVA_HOME_ variable is pointing on
3. add _[Jerkar Home]_ to your _PATH_ environment variable
4. execute `jerkar` in the command line. You should get an output starting by : 

<pre><code>
 _______           _
(_______)         | |
     _ _____  ____| |  _ _____  ____
 _  | | ___ |/ ___) |_/ |____ |/ ___)
| |_| | ____| |   |  _ (/ ___ | |
 \___/|_____)_|   |_| \_)_____|_|
                                     The 100% Java build tool.
</code></pre>

## Setup Eclipse

To reference Jerkar libraries and project dependencies in the Eclipse, it's better to use _classpath variables_ as the project will be more portable. 
Moreover, when Jerkar generates _.classpath_ file, variables are used in place of complete path, so you need anyway to set these variables once for all :

1. Open the Eclipse preference window : _Window -> Preferences_
2. Navigate to the classpath variable panel : _Java -> Build Path -> Classpath Variables_
3. Add these 2 variables :
    * `JERKAR_HOME` which point to the same location than _[Jerkar Home]_ 
    * `JERKAR_REPO` which point to _[User Home]_/.jerkar/cache/repo
  
If you have any problem to figure out the last value, just execute `jerkar` from anywhere and it will start logging the relevant information :

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

## Create a Java Jerkar project

When starting with a new project you have the choice of creating the project by hand or use scaffolding.

### Create a project by hand
1. Create a new java project in your IDE : You can follow convention by naming your project as _groupName.projectName_.
2. Add the _[Jerkar Home]/org.jerkar.core-fat.jar_ lib to your project build-path and attach the source code ( _[Jerkar Home]/lib-sources_ ). This jar includes Jerkar core along plugins classes.
3. Create a _build/def_ folder at the base of your project and make it a source folder in your IDE. In Jerkar, all build related stuff (build definition, local 3rd party libs, produced artifacts,...) lie in _build_ directory located at the root of your project
4. Write the build definition class extending `JkJavaBuild` in the _build/def_ folder (in whatever package).


<p class="alert alert-success">
If your project is <a href="../../tour.html#100conventional">100% conventional</a>, you can skip steps <strong>2, 3 and 4</strong>.
</p>

### Use scaffolding
1. Create a new java project in your IDE
2. Execute `jerkar scaffold` under the project base directory. This generates the project structures.

<p class="alert alert-success">
	<strong>Eclipse users : </strong> You can execute <code>jerkar eclipse#generateFiles</code> to generate both <small>.project</small> and <small>.classpath</small> files from the build definition.
</p>

As Jerkar builds itself, you can have a look at [how Jerkar project is structured](https://github.com/jerkar/jerkar/tree/master/org.jerkar.core).

## Build your project

You can build your project by using the command line or by launching it through your IDE. Using IDE provides the ability to debug your build in a friendly way, as you would debug regular classes.

### Launch in command line

Open a terminal, set the current directory to the root of your project and execute `jerkar`.
This will execute ´doDefault´ method of the default build class of your project. If your project has no build class it will execute `JkJavaBuild#doDefault()` provided with Jerkar library.

### Launch 

Launch the `org.jerkar.tool.Main` class in your IDE or type `jerkar` in the command line (with the root of your project as working directory).

## Play with Sample project

To provide an easy hand on experience, you can play with the **org.jerkar.build-samples** project host in [Jerkar Git Repository](https://github.com/jerkar/jerkar.git).

This project contains several build classes all located under *build/def* directory. 

All of these build classes are independent from each other, each one illustrates one way of doing thing in Jerkar. 
Some of these build class declares dependencies only for illustration purpose, not because the compiled classed actually need it.

<p class="alert alert-success">
	<strong>Prerequisite for Eclipse user : </strong> 
	Set the *Windows -> Preference ->  
</p>

1. import **org.jerkar.build-samples** project in your IDE
   -> Normally the IDE displays error mark cause the build path is not properly set up.
2. Eclipse users : execute <code>jerkar scaffold eclipse#generateFiles</code> at the root of the project
   -> This generates the eclipse .classpath file
  

   
 
 
 
 
 
 
 
 
 
 
 
 
 
 
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/> 
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/> 
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/> 
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/> 
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/> 
<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>



