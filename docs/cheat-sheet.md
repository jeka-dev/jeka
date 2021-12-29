## Useful commands 

_Jeka_ comes with predefined methods coming either from `JkClass` or built-in plugins. 

* `jeka` : Displays on console methods and options invokable from command line, along plugins available in the classpath.
* `jeka [pugin-name]#help` : Displays on consoles all methods and option invokable for the specified plugin (e.g. `jeka scaffold#help`).
* `jeka intellij#iml` : Generates iml file for Intellij. It is generated according the dependencies declared for this project.
* `jeka intellij#iml -JKC=` : If the above fails cause your def classes do not compile, using `-JKC=` avoids def compilation phase.
* `jeka eclipse#files` : Same purpose as above to generate metadata files for Eclipse.
* `jeka scaffold#run` : Generates files for creating a basic Jeka project from scratch.
* `jeka scaffold#wrapper` : Generates wrapper files (jekaw/jekaw.bat and bootstrap jar)
* `jeka scaffold#run java#` : Generate files for creating a Jeka project for building a JVM language project

## Useful standard options

You can add these options to you command line.

* `-kb=[KBeanName]` : By default, Jeka instantiates the first _KBean_ found under _def_ directory to execute methods on. 
  You can force to instantiate a specific class by passing its long or short name. 
  If the class is already in classpath, then no _def_ compilation occurs.
  Using simply `-JKC=` is equivalent to `-JKC=JkClass` which is the base class bundled in Jeka.
* `-lri` : Displays runtime info. This will display on console meaningfull information about current Jeka version, Java version, base directory, download repository, classpath, ...
* `-lsu` : Shows logs about jeka setup (compilation of def classes, plugin loading, ...).These informations are not logged by default.
* `-ls=BRACE` : Alters console output by delimiting tasks with braces and mentioning the processing time for each.
* `-ls=DEBUG` : Alters console output by showing the class name and line number from where the log has been emitted.
* `-lv` : Alters console output by displaying trace logs (emitted by `JkLog#trace`)
* `-dcf` : Force compilation of _def_ classes, even if they are marked as up-to-date.

## How to change the JDK that run Jeka

To determine the JDK to run upon, _jeka_ looks in priority order at :7

* _JEKA_JDK_ environment variable ([_JEKA_JDK_]/bin/java must point on _java_ executable)
* _JAVA_HOME_ environment variable 

If none of these variables are present, _jeka_ will run upon the _java_ executable accessible from your _PATH_ environment.

## How to change the repository Jeka uses to fetch dependencies 

By default, _jeka_ fetch dependencies from maven central (https://repo.maven.apache.org/maven2).

You can select another default repository by setting the `jeka.repos.download.url` options. 
We recommend storing this value in your [USER DIR]/.jeka/options.properties file to be reused across projects.

For more details, see `JkRepoFromOptions` javadoc.
