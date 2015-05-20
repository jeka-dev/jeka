# Getting Started
------------------

## How to install Jerkar

1. unzip the [distribution archive](http://jerkar.github.io/binaries/jerkar-distrib.zip) to the directory you want to install Jerkar : let's call it `[Jerkar Home]`
2. make sure that either a valid *JDK* (6 or above) is on your _PATH_ environment variable or that a _JAVA_HOME_ variable is pointing on
3. add `[Jerkar Home]` to your _PATH_ environment variable
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
## How to scaffold a Jerkar project
1. create a directory named as _groupName.projectName_. You can use only _projectName_ if your project has no group
2. under this directory, execute `jerkar scaffold`. This generates the project structures.

<p class="alert alert-success">
	<strong>Eclipse users : </strong> You can execute <code>jerkar eclipse#generateFiles</code> to generate <small>.project</small> and <small>.classpath</small> files.
</p>

## How to setup Jerkar on existing Java project
1. add the `[Jerkar Home]/org.jerkar.core-fat.jar` lib to your project build-path on your IDE and attach the source code (`[Jerkar Home]/lib-sources`). This jar includes Jerkar core along plugins classes
2. create a `build/def` folder at the base of your project and make it a source folder in your IDE. In Jerkar, all build related stuff (build definition, local 3rd party libs, produced artifacts,...) lie in `build` directory located at the root of your project
3. write the build definition class extending `JkJavaBuild` in the `build/def` folder (in whatever package)
4. launch the `org.jerkar.Main` class in your IDE or type `jerkar` in the command line (with the root of your project as working directory)

As Jerkar builds itself, you can have a look at [how Jerkar project is structured](https://github.com/jerkar/jerkar/tree/master/org.jerkar.core).

<p class="alert alert-success">
If your project is <a href="../../tour.html#100conventional">100% conventional</a>, you can even skip steps <strong>2.</strong> and <strong>3.</strong>.
</p>



