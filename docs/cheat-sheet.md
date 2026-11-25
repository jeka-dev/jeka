# Cheat Sheet

## Useful Commands

| Command                 | Description                                                    |
|-------------------------|----------------------------------------------------------------|
| `--doc`                 | Displays available methods, options, and KBeans.               |
| `intellij: sync -f`     | Generates `.iml` file for IntelliJ IDEA.                       |
| `intellij: initProject` | Initializes IntelliJ IDEA Project.                             |
| `eclipse: sync`         | Same purpose as above to generate metadata files for Eclipse.  |
| `project: scaffold`     | Generates files to create a basic JeKa project from scratch.   |
| `project: pack`         | Build JARs and other optional artifacts from a project.        |
| `base: scaffold`        | Creates files for a base workspace without a project.          |

## Standard Properties

| Property Name         | Default Value                         | Description                                                                                                                                                                            |
|-----------------------|---------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| jeka.version          |                                       | The JeKa version to use. It will be fetched from the repository specified first in the `jeka.repos.download` property. Use `jeka.version=.` to force the use of the locally installed version. |
| jeka.distrib.location |                                       | The exact location (directory) to get the JeKa distribution. If set, `jeka.version` will be ignored.                                                      |
| jeka.java.version     |                                       | The version of the JDK used to run JeKa and compile code located in `jeka-src`. Should be 21 or higher.                                                                                                        |
| jeka.java.distrib     | `temurin`                             | The distribution of JDK to fetch when `jeka.java.version` is mentioned.                                                                                        |
| jeka.repos.download   | `mavenCentral`                        | Comma-separated string of repositories to fetch Maven dependencies. More details [here](reference/properties.md#repositories).                                                         |
| jeka.repos.publish    |                                       | Comma-separated string of repositories to publish Maven artifacts. More details [here](reference/properties.md#repositories).                                                          |
| jeka.program.build    | See [here](tutorials/source-runnable-apps.md#jeka-projects) | Command line to execute to build the application when execution files are absent.                                                                                                          |
| jeka.kbean.default    |                                       | Name or class name of the KBean to use as default (when none is specified).                                                                                                            |
| jeka.test.skip        |                                       | Skip tests when building projects or codebases.                                                                                                                                        |
| jeka.platform.os      | *linux*, *windows* or *mac*           | Provides the OS of the running machine.                                                                                                                                                |
| jeka.platform.arch    | *x86 64-bit*, *AArch64 64-bit*        | Provides the architecture of the running machine.                                                                                                                                      |
| maven.classifier.os   | *linux*, *windows* or *osx*           | Provides the OS of the running machine, using maven nomenclature.                                                                                                                      |
| maven.classifier.arch | *aarch_64*, *x86_64* or *x86*         | Provides the architecture of the running machine, using maven nomenclature.                                                                                                            |


## JDK Selection Rules
1. If `JEKA_JDK_HOME` is set, use this JDK.
2. If `jeka.java.version` is specified:
    - Use JDK from `jeka.jdk.[version]` if available.
    - Otherwise, fetch JDK from local cache or download it.
3. If no version is specified:
    - Use JDK from `JAVA_HOME` if set.
    - Otherwise, fetch JDK 21 from local cache or download it.

## How JeKa Reads Properties
JeKa reads properties in this order:

- Command-line arguments (as `-Dmy.prop=xxx`).
- Environment variables (`my.prop` or `MY_PROP`). The bash launcher only recognizes the exact `my.prop` name for the properties it reads itself (`jeka.version`, `jeka.java.version`, `jeka.java.distrib`, `jeka.jdk.X`, ...).
- The `jeka.properties` file in the base directory.
- The `jeka.properties` files in parent directories, up to the first directory without one.
- The `global.properties` file in `[JEKA_USER_HOME]`.