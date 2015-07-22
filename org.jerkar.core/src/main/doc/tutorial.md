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
<br/>
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
	<strong>Eclipse users : </strong> You can execute <code>jerkar scaffold eclipse#generateFiles</code> altogether to generate <small>.project</small> and <small>.classpath</small> files.
</p>

As Jerkar builds itself, you can have a look at [how Jerkar project is structured](https://github.com/jerkar/jerkar/tree/master/org.jerkar.core).

## Build your project

Launch the `org.jerkar.tool.Main` class in your IDE or type `jerkar` in the command line (with the root of your project as working directory).


<br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/><br/>



