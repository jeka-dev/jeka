## Useful commands 

_Jeka_ has predefined methods that comes either from `JkClass` or built-in plugins. 

| Command              | Description                                                                                                                         |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `#help`              | Displays methods and options invokable from command line, along with plugins available in the classpath.                            |
| `[kbean-name]#help`  | Displays methods and options invokable for the specified plugin (e.g. `jeka scaffold#help`).                                        |
| `intellij#iml`       | Generates iml file for Intellij. This is generated according to the dependen Standrdcies declared for _Jeka_ project.               |
| `intellij#iml -dci`  | If the `jeka intellij#iml` fails due to compilation error on def classes, `-dci` forces ignore compileation error on *def classes*. |
| `eclipse#files`      | Same purpose as above to generate metadata files for Eclipse.                                                                       |
| `scaffold#run`       | Generates files to create a basic _Jeka_ project from scratch.                                                                      |
| `scaffold#wrapper`   | Generates wrapper files (jekaw/jekaw.bat and bootstrap jar).                                                                        |
| `scaffold#run java#` | Generates files to create a _Jeka_ project to build a JVM language project.                                                         |

## Useful standard options

You can add the below options to the command line.

| Option            | Description                                                                                                                                                                         |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-kb=[KBeanName]` | By default, _Jeka_ instantiates the first _KBean_ found under def directory to execute methods. It can be forced to instantiate a specific class by passing its long or short name. |
| `-lri`            | Displays runtime info which will be displayed meaningfully on the console about current _Jeka_ version, Java version, base directory, download repository, classpath, ...           |
| `-lsu`            | Shows logs about _Jeka_ setup (compilation of def classes, plugin loading, ...). This information is not logged by default.                                                         |
| `-ls=BRACE`       | Alters console output by delimiting tasks with braces and mentioning the processing time for each task.                                                                             |
| `-ls=DEBUG`       | Alters console output by showing the class name and line number where the log has been emitted.                                                                                     |
| `-lv`             | Alters console output by displaying trace logs (emitted by `JkLog#trace`).                                                                                                          |
| `-cw`             | Clean _.work_ directory, forcing compilation of def classes, even if it is marked as being up-to-date.                                                                              |

## Standard Properties

| Property Name         | Default Value                  | Description                                                                                                                                        |
|-----------------------|--------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| jeka.version          |                                | The Jeka version to use. The version will be fetched from the repo mentioned in `jeka.distrib.repo` property                                       |
| jeka.distrib.repo     | https://repo1.maven.org/maven2 | The repo where to fetch JeKa versions. Use https://oss.sonatype.org/content/repositories/snapshots for snapshot versions                           |
| jeka.distrib.location |                                | The exact location (file dir or url) to get the JeKa distribution. If set, both `jeka.version` and `jeka.distrib.repo` will be ignored.            |
| jeka.java.version     |                                | The version of the JDK used to compile and run Java code                                                                                           |
| jeka.java.distrib     | temurin                        | The distribution of JDK to fetch when `jeka.java.version` is mentioned                                                                             |
| jeka.repos.download   | local, mavenCentral            | Comma separated string of repositories to fetch Maven dependencies. More details [here](reference-guide/execution-engine-properties/#repositories) |
| jeka.repos.publish    |                                | Comma separated string of repository to publish Maven artifacts. More details [here](reference-guide/execution-engine-properties/#repositories)    |

## Rules for selecting JDK 

- if JEKA_JDK_HOME env var is specified, select it
- if `jeka.java.version` property is specified 
    - if a `jeka.jdk.[version]` property is specified, select it. 
    - else, look in cache or download the proper JDK
- else
    - if *JAVA_HOME* env var is specified, select it
    - else, look in cache and download default version (21)

## Rules for reading Properties

This is the place, in order of precedence where JeKa reads properties

- The command-line arguments formatted as "-Dmy.prop=xxx"
- Environment variables formatted as 'my.prop' 
- [BASE_DIR]/jeka.properties file. Where [BASE_DIR] is the root of the project to build/run.
- look recursively in parent [BASE_DIR]/../jeka.properties. Stop at first folder ancestor not having a *jeka.properties* file
- [JEKA_USER_HOME]/global.properties