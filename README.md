![Build Status](https://github.com/jerkar/jeka/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/jeka-core?versionSuffix=.RELEASE)](https://search.maven.org/search?q=g:%22dev.jeka%22%20AND%20a:%22jeka-core%22) 
[![Gitter](https://badges.gitter.im/jeka-tool/community.svg)](https://gitter.im/jeka-tool/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Twitter Follow](https://img.shields.io/twitter/follow/JekaBuildTool.svg?style=social)](https://twitter.com/JekaBuildTool)  

# What is Jeka ?

<img src="./docs/images/knight-color-logo.svg" align="left" width="180"/>

<strong>Jeka</strong> is a complete **Java build system** ala _Ant_, _Maven_ or _Gradle_ using Java as its main language instead of using XML or Groovy/Kotlin DSLs.

Build/task definitions are expressed with plain *Java* classes to leverage IDE power and Java ecosystem seamlessly.

Build scripts can be coded, modeled, run, debugged and reused as regular code.

Jeka offers an execution engine, a build API and a powerful plugin architecture to make your automation tasks a breeze. 

<br/>

# Based on simple ideas

- Run Java public methods from both IDE and command line indifferently.
- Simply use Java libraries for building Java projects programmatically.

<br/>
<sub>This is an example for building a simple Java Project.</sub>

```java
class Build extends JkBean {

    private JkProject project;
    
    Build() {
        project = JkProject.of().simpleFacade()
            .setBaseDir(".")
            .includeJavadocAndSources(true, true)  // produce javadoc and sources jars
            .configureCompileDependencies(deps -> deps
                .and("com.google.guava:guava:31.0.1-jre")
                .and("com.fasterxml.jackson.core:jackson-core:2.13.0")
            )
            .configureTestDependencies(deps -> deps
                .and("org.junit.jupiter:junit-jupiter-engine:5.8.2")
            )
            .setPublishedModuleId("my.org:my-module")
            .setPublishedVersion(JkGitProcess.of().getVersionFromTag());
    }

    public void cleanPack() {
        JkPathTree.of(getBaseDir().resolve("jeka//output")).deleteContent();
        project.pack();  // package all artifacts of the project (jar, source jar and javadoc)
    }

}
```
<sub>To build the project, execute ´cleanPack´ from your IDE or execute the following command line.</sub>
```
/home/me/myproject>./jekaw cleanPack
```

<details>
<summary>Example of Springboot project.</summary>

```java
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.tool.JkBean;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.plugins.springboot.SpringbootJkBean;

@JkInjectClasspath("dev.jeka:springboot-plugin")
class Build extends JkBean {

    private final SpringbootJkBean springbootBean = getBean(SpringbootJkBean.class);

    public boolean runIT = true;
    
    Build() {
        springbootBean.setSpringbootVersion("2.2.6.RELEASE");
        springbootBean.projectBean().configure(this::configure);
    }
    
    private void configure(JkProject project) {
        project.flatFacade()
                .configureCompileDependencies(deps -> deps
                        .and("org.springframework.boot:spring-boot-starter-web")
                        .and("org.projectlombok:lombok:1.18.20")
                )
                .configureRuntimeDependencies(deps -> deps
                        .minus("org.projectlombok:lombok")
                )
                .configureTestDependencies(deps -> deps
                        .and("org.springframework.boot:spring-boot-starter-test")
                            .withLocalExclusions("org.junit.vintage:junit-vintage-engine")
                )
                .addTestExcludeFilterSuffixedBy("IT", !runIT);
    }

    public void cleanPack() {
        springbootBean.projectBean().clean();
        springbootBean.projectBean().pack();
    }

}
```
</details>

Projects can be **also built without any build code** in a very concise way : 1 properties file for configuration and 
on flat file only for declaring dependencies.

<details>
<summary>Example building springboot project using Sonarqube + Jacoco test coverage</summary>

*local.properties*
```properties 
jeka.cmd._append=springboot# @dev.jeka:jacoco-plugin @dev.jeka:sonarqube-plugin @dev.jeka:springboot-plugin

jeka.cmd.build=project#clean project#pack
jeka.cmd.build_quality=:build sonarqube#run jacoco# sonarqube#logOutput=true -Dsonar.host.url=http://localhost:9000

jeka.java.version=17
springboot#springbootVersion=2.7.5

intellij#jekaModuleName=wrapper-common
```

*project-dependencies.txt*
```text
==== COMPILE ====
org.springframework.boot:spring-boot-starter-web
org.projectlombok:lombok:1.18.24

==== RUNTIME ====
org.postgresql:postgresql:42.3.7
- org.projectlombok:lombok

==== TEST ====
org.springframework.boot:spring-boot-starter-test
```
</details>

Explore Jeka possibilities from command line `jekaw -h`.</sub>

# User friendly
Thanks to wrapper and [Jeka Plugin for Intellij](https://github.com/jerkar/jeka-ide-intellij), you don't need to install anything on your machine. 
You only need a JDK 8 or higher.

Jeka is extremly lightweight, the full distribution size is around 1 MB including source code. 
The whole tool is contained in a single jar of aproximatly 600 KB and has zero dependency.

It's quite easy to discover what Jeka does behind the scene and troubleshot issues encountered during the build.

> Current version of Jeka is no more compatible with Jeka Plugin for Intellij. Stick with Jeka version _0.9.15.RELEASE_ 
> if you still want to use the IDE plugin. 
> 
> This plugin will be upgraded later to use the latest Jeka version. 


# Get Jeka

The simpler way to get Jeka is by using [IntelliJ plugin](https://github.com/jerkar/jeka-ide-intellij) that will 
handle distribution installation for you. Anyway you can get the binary distributions from the following places :

* Snapshots : https://oss.sonatype.org/content/repositories/snapshots/dev/jeka/jeka-core/
* Milestones and Release Candidates : https://github.com/orgs/jeka-dev/packages?repo_name=jeka
* Releases : https://repo1.maven.org/maven2/dev/jeka/jeka-core/

The distribution is the file named jeka-core-x.x.x-distrib.zip. 

# How to use Jeka ?

Visit following pages according your expectation :

* [Getting Started (Needs Intellij Plugin)](https://jeka-dev.github.io/jeka/tutorials/gui-getting-started/#getting-started-with-jeka)
* [Reference Guide](https://jeka-dev.github.io/jeka/)
* [Working examples](https://github.com/jeka-dev/working-examples)

# External plugins

Jeka comes with plugins out of the box. These plugins covers the most common points a Java developer need to address 
when building a project. This includes plugins for IDE metadata generation (IntelliJ, Eclipse), dependency management, git, , java project building, testing, PGP signing, binary repositories, Maven interaction, scaffolding, sonarQube and web archives.

Nevertheless, Jeka is extensible and other plugins exist outside the main distib among :
* [Springboot Plugin](plugins/dev.jeka.plugins.springboot)
* [Sonarqube Plugin](plugins/dev.jeka.plugins.sonarqube)
* [Jacoco Plugin](plugins/dev.jeka.plugins.jacoco)
* [NodeJs Plugin](plugins/dev.jeka.plugins.nodejs)
* [Protobuf Plugin](https://github.com/jerkar/protobuf-plugin)

# Community

<a class="btn btn-link btn-neutral" href="https://projects.ow2.org/view/jeka">
              <img src="https://jeka.dev/images/ow2.svg" alt="Image" height="60" width="60"></a>
              
This project is supported by OW2 consortium.

You can ask question using regular using [this repository issues](https://github.com/jerkar/jerkar/issues).

You can also use direct emailing for questions and support : djeangdev@yahoo.fr

A twitter account also exist : https://twitter.com/djeang_dev

# News

* Version 0.10.0 soon available. Stay tuned !

# Roadmap/Ideas

We hope the 0.10.xx series to be the last prior 1.0.0. 
0.10.xx series is a considerable improvement from 0.9.xx. 
We expect our users to give feedbacks to finalise the product.  

* Stabilise api from user feedbacks. API is quite workable now but may be improved from user inputs
* Enhance existing graphical [plugin for Intellij](https://github.com/jerkar/jeka-ide-intellij)
* Improve Kotlin integration
* Provide a plugin for Android

Please visit [release note](https://github.com/jerkar/jeka/blob/master/release-note.md) and [issues](issues) for roadmap.


# How to build Jeka ?

This repository is organized as a _mono repo_. It contains The Jeka core project along plugins and samples for 
automation testing.

* dev.jeka.core : Complete Jeka tool
* plugins : Jeka plugins released along Jeka core (Springboot, NodeJs, Jacoco and Sonarqube)
* samples : Sample projects serving for examples and automation testing
* dev.jeka.master : The master build for building all together.

Jeka builds itself. To build Jeka full distribution from sources, the simpler is to use your IDE.

Once distribution created, add the distrib folder to your PATH environment variable.

## Build Jeka from IntelliJ

* Clone this repository into IntelliJ. Project is already configured (.iml and modules.xml are stored in git).
* Add the `JEKA_CACHE_DIR` variable pointing on [USER_HOME]/.jeka/cache
* Make sure the project is configured with a JDK8 or higher.
* Run 'FULL BUILD' in Intellij _Run Configurations_ to perform a full build of core + plugins + complete test suite.
* Run 'FAST BUILD' in Intellij _Run Configurations_ to perform a fast build of the core without tests.

> For debugging the project, you have to set up Intellij in order to workaround with an Intellij issue :
> Settings/Preferences | Build, Execution, Deployment | Debugger | Data Views | Kotlin | enable "Disable coroutine agent.
> [See here](https://stackoverflow.com/questions/68753383/how-to-fix-classnotfoundexception-kotlinx-coroutines-debug-agentpremain-in-debu)


## Build Jeka from command line

Jeka builds itself, but we need to compile the Jeka sources prior to execute it. 
Fot this, a small _Ant_ script bootstraps the build process by compiling Jeka first then launch 
the Jeka build.

At the repository root dir, execute : `ant -f .github\workflows\build.xml`.

To build the project including Sonarqube and test coverage  : `ant -f .github\workflows\build.xml -Dsonar.host.url=... `.  
Cause of Sonalqube scanner, this command has to be run with a JDK >= 11.


## How to edit documentation

Documentation is generated with [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/). Documentation sources are located (here)[docs].

You must install _Python_ and _Material for MkDocs_ on your computer (`pip install mkdocs-material`) prior to execute following command lines from the repo root directory :
- `mkdocs serve` : generate and serve the documentation on localhost:8000

The documentation is also supposed to be regenerated after each push/pull-request.


## How to Release

Release is done automatically by Github action on PUSH on *master*.
If the last commit message title contains a word like 'Release:XXX' (case matters ) then a tag XXX is created and 
the binaries will be published on Maven Central.
Otherwise, the binary wll be simply pushed on OSSRH snapshot.
<p align="center">
    <img src="docs/images/mascot.png" width='420' height='420' />
</p>

