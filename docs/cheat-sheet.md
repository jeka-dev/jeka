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
| jeka.distrib.location |                                     | The exact location (directory) to get the JeKa distribution. If set, both `jeka.version` and `jeka.distrib.repo` will be ignored.                                                      |
| jeka.java.version     |                                     | The version of the JDK used to run Jeka, and compile code located in *jeka-src*                                                                                                        |
| jeka.java.distrib     | temurin                             | The distribution of JDK to fetch when `jeka.java.version` is mentioned. Should be 21 or higher                                                                                         |
| jeka.repos.download   | local, mavenCentral                 | Comma separated string of repositories to fetch Maven dependencies. More details [here](reference-guide/execution-engine-properties/#repositories)                                     |
| jeka.repos.publish    |                                     | Comma separated string of repository to publish Maven artifacts. More details [here](reference-guide/execution-engine-properties/#repositories)                                        |
| jeka.program.build    | project: pack -Djeka.test.skip=true | Cmd line to execute to build project when exec files are absents                                                                                                                       |
| jeka.kbean.default    |                                     | Name or class name of the KBean to use as default (when none is specified)                                                                                                             |
| jeka.test.skip        |                                     | Skip tests when building projects or code bases.                                                                                                                                       |
| jeka.platform.os      | *linux*, *windows* or *mac*         | Provides the os of the running machine                                                                                                                                                 |
|

## JDK Selection Rules
1. If `JEKA_JDK_HOME` is set, use this JDK
2. If `jeka.java.version` is specified:
    - Use JDK from `jeka.jdk.[version]` if available
    - Otherwise fetch JDK from local cache or download it
3. If no version is specified:
    - Use JDK from `JAVA_HOME` if set
    - Otherwise fetch JDK 21 from local cache or download it

## How JeKa Reads Properties
JeKa reads properties in this order:

- Command-line arguments (as `-Dmy.prop=xxx`).
- Environment variables (`my.prop`or `MY_PROP`).
- The *jeka.properties* file in the base dir.
- The *jeka.properties* file in parent directories, until it's not found.
- The *global.properties* file in [JEKA_USER_HOME].