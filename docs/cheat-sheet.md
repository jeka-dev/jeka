## Useful commands

| Command                 | Description                                                    |
|-------------------------|----------------------------------------------------------------|
| `--doc`                 | Displays available methods, options and KBeans                 |
| `intellij: sync -f`     | Generates iml file for Intellij.                               |
| `intellij: initProject` | Initializes Intellij Project                                   |
| `eclipse: sync`         | Same purpose as above to generate metadata files for Eclipse.  |
| `project: scaffold`     | Generates files to create a basic _Jeka_ project from scratch. |
| `project: pack`         | Build jars and others optional artifacts from a project        |
| `base: scaffold`        | Creates files a base workspace without project.                |

## Standard Properties

| Property Name         | Default Value                       | Description                                                                                                                                                                            |
|-----------------------|-------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| jeka.version          |                                     | The Jeka version to use. It will be fetched from the repository specified in the `jeka.distrib.repo` property. Use `jeka.version=.` to force the use of the locally installed version. |
| jeka.distrib.repo     | https://repo1.maven.org/maven2      | The repo where to fetch JeKa versions. Use https://oss.sonatype.org/content/repositories/snapshots for snapshot versions                                                               |
| jeka.distrib.location |                                     | The exact location (file dir or url) to get the JeKa distribution. If set, both `jeka.version` and `jeka.distrib.repo` will be ignored.                                                |
| jeka.java.version     |                                     | The version of the JDK used to compile and run Java code                                                                                                                               |
| jeka.java.distrib     | temurin                             | The distribution of JDK to fetch when `jeka.java.version` is mentioned                                                                                                                 |
| jeka.repos.download   | local, mavenCentral                 | Comma separated string of repositories to fetch Maven dependencies. More details [here](reference-guide/execution-engine-properties/#repositories)                                     |
| jeka.repos.publish    |                                     | Comma separated string of repository to publish Maven artifacts. More details [here](reference-guide/execution-engine-properties/#repositories)                                        |
| jeka.program.build    | project: pack -Djeka.test.skip=true | Cmd line to execute to build project when exec files are absents                                                                                                                       |
| jeka.kbean.default    |                                     | Name or class name of the KBean to use as default (when none is specified)                                                                                                             |
| jeka.test.skip        |                                     | Skip tests when building projects or code bases.                                                                                                                                       |

## Rules for selecting JDK 

- if JEKA_JDK_HOME env var is specified, select it
- if `jeka.java.version` property is specified 
    - if a `jeka.jdk.[version]` property is specified, select it. 
    - else, look in cache or download the proper JDK
- else
    - if *JAVA_HOME* env var is specified, select it
    - else, look in cache and download default version (21)

## How JeKa Reads Properties
JeKa reads properties in this order:

- Command-line arguments (as `-Dmy.prop=xxx`).
- Environment variables (`my.prop`or `MY_PROP`).
- The *jeka.properties* file in the base dir.
- The *jeka.properties* file in parent directories, until it's not found.
- The *global.properties* file in [JEKA_USER_HOME].