# Getting Started
----

## How to install Jerkar

1. unzip the [distribution archive](http://jerkar.github.io/binaries/jerkar-distrib.zip) to the directory you want to install Jerkar : let's call it `[Jerkar install dir]`
2. make sure that either a valid *JDK* (6 or above) is on your _PATH_ environment variable or that a _JAVA_HOME_ variable is pointing on
3. add `[Jerkar install dir]` to your _PATH_ environment variable
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
## How scaffold a Jerkar project
1. create a directory named as _groupName.projectName_. You can use only _projectName_ if your project has no group
2. under this directory, execute `jerkar scaffold`. This generates the project structures.
3. If you are an Eclipse user, you can execute `jerkar eclipse#generateFiles` to generate `.project` and `.classpath` files.
<br/>

## How to setup Jerkar on existing Java project
1. add the `Jerkar install dir]/org.jerkar.core-fat.jar` lib to your project build-path on your IDE and attach the source code (`[Jerkar install dir]/lib-sources`). This jar includes Jerkar core along plugins classes.
2. create a `build/def` folder at the base of your project and make it a source folder in your IDE. In Jerkar, all build related stuff (build definition, local 3rd party libs, produced artifacts,...) lies under `[Jerkar install dir]/build` directory
3. write the build definition class extending JkJavaBuild in this directory (in whatever package)
4. launch the `org.jerkar.Main` class in your IDE or type `jerkar` in the command line (with the root of your project as working directory)

If your project respect conventions, does not need managed dependencies and don't do any specific things, you can even skip 2) and 3).




