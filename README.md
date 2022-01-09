![Build Status](https://github.com/jerkar/jeka/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/jeka-core?versionSuffix=.RELEASE)](https://search.maven.org/search?q=g:%22dev.jeka%22%20AND%20a:%22jeka-core%22) 
[![Gitter](https://badges.gitter.im/jeka-tool/community.svg)](https://gitter.im/jeka-tool/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Twitter Follow](https://img.shields.io/twitter/follow/JekaBuildTool.svg?style=social)](https://twitter.com/JekaBuildTool)  
<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<img src="http://jeka.dev/images/large-social-logo.png" align='middle'/>

# What is Jeka ?

<strong>Jeka</strong> (formerly Jerkar) is a complete **Java build system** ala _Ant_, _Maven_ or _Gradle_ using Java as its main language instead of using XML or Groovy/Kotlin DSLs.

Build/task definitions are expressed with plain *Java* classes to leverage IDE power and Java ecosystem seamlessly.

Build scripts can be coded, modeled, run, debugged and reused as regular code.

Jeka offers an execution engine, a build API and a powerful plugin architecture to make your automation tasks a breeze. 

# Based on simple ideas

- Run Java methods from both IDE and command line indifferently.
- Simply use Java libraries for building Java projects programmatically.

<br/>
<sub>This is an example for building a simple Java Project.</sub>

```java
class Build extends JkBean {

    private JkProject project;
    
    Build() {
        project = JkProject.of().simpleFacade()
            .setBaseDir(".")
            .includeJavadocAndSources(true, true)
            .useSimpleLayout()  // sources and resources in ./src, tests and test resources in ./tests
            .mixResourcesAndSources()
            .configureCompileDeps(deps -> deps
                .and("com.google.guava:guava:31.0.1-jre")
                .and("com.fasterxml.jackson.core:jackson-core:2.13.0")
            )
            .configureTestDeps(deps -> deps
                .and("org.junit.jupiter:junit-jupiter-engine:5.8.2")
            )
            .setPublishedModuleId("my.org:my-module")
            .setPublishedVersion(JkGitProcess.of().getVersionFromTag());
    }

    public void cleanPack() {
        clean();
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

    private final SpringbootJkBean springboot = getBean(SpringbootJkBean.class);

    public boolean runIT = true;
    
    Build() {
        springboot.setSpringbootVersion("2.2.6.RELEASE");
        springboot.projectBean().configure(this::configure);
    }
    
    private void configure(JkProject project) {
        project.simpleFacade()
                .configureCompileDeps(deps -> deps
                        .and("org.springframework.boot:spring-boot-starter-web")
                        .and("org.projectlombok:lombok:1.18.20")
                )
                .configureRuntimeDeps(deps -> deps
                        .minus("org.projectlombok:lombok")
                )
                .configureTestDeps(deps -> deps
                        .and("org.springframework.boot:spring-boot-starter-test")
                        .withLocalExclusions("org.junit.vintage:junit-vintage-engine")
                )
                .addTestExcludeFilterSuffixedBy("IT", !runIT);
    }

    public void cleanPack() {
        clean();
        springboot.projectBean().pack();
    }

}
```
</details>

Explore Jeka possibilities from command line `jekaw -h`.</sub>

# User friendly
Thanks to wrapper and [Jeka Plugin for Intellij](https://github.com/jerkar/jeka-ide-intellij), you don't need to install anything on your machine. 
You only need a JDK 8 or higher.

Getting started in 5 minutes : [Here](https://jerkar.github.io/jeka/tutorials/gui-getting-started/)

> Current version of Jeka is no more compatible with Jeka Plugin for Intellij. Stick with Jeka version _0.9.15.RELEASE_ 
> if you still want to use the IDE plugin. 
> 
> This plugin will be upgraded later to use the latest Jeka version. 


# Get Jeka

* Snapshots : https://oss.sonatype.org/content/repositories/snapshots/dev/jeka/jeka-core/
* Releases : https://repo1.maven.org/maven2/dev/jeka/jeka-core/

The distribution is the file named jeka-core-x.x.x-distrib.zip. 

# How to use Jeka ?

Visit following pages according your expectation :

* [Reference Guide](https://jerkar.github.io/jeka)
* [Working examples](https://github.com/jerkar/working-examples)
* [Getting Started (Needs Intellij Plugin)](dev.jeka.core/src/main/doc/Getting%20Started%20-%20IDE.md) 

# External plugins

Jeka comes with plugins out of the box. These plugins covers the most common points a Java developer need to address 
when building a project. This includes plugins for IDE metadata generation (IntelliJ, Eclipse), dependency management, git, , java project building, testing, PGP signing, binary repositories, Maven interaction, scaffolding, sonarQube and web archives.

Nevertheless, Jeka is extensible and other plugins exist outside the main distib among :
* [Springboot Plugin](plugins/dev.jeka.plugins.springboot)
* [Sonarqube Plugin](plugins/dev.jeka.plugins.sonarqube)
* [Jacoco Plugin](plugins/dev.jeka.plugins.jacoco)
* [Protobuf Plugin](https://github.com/jerkar/protobuf-plugin)

# Community

<a class="btn btn-link btn-neutral" href="https://projects.ow2.org/view/jeka">
              <img src="https://jeka.dev/images/ow2.svg" alt="Image" height="60" width="60"></a>
              
This project is supported by OW2 consortium.

You can ask question using regular using [this repository issues](https://github.com/jerkar/jerkar/issues).

You can also use direct emailing for questions and support : djeangdev@yahoo.fr

A twitter account also exist : https://twitter.com/djeang_dev

# News

* Completely reworked execution engine. `JkClass` and `JkPlugin` have been merged in the unified `JkBean` concept.
* Enhanced performance with faster startup and support for Java 11 & 17.
* Reworked command-line syntax.
* Improved help and documentation.
* Experimental support for Kotlin, both for writting build code and build Kotlin projects.
* Jeka project is now organized in a mono-repo leading in a better integrated/tested components.


# Roadmap/Ideas

* Improve landing page and provide tutorials based on Intellij plugin for easy/fast starting.
* Stabilise api from user feedbacks. API is quite workable now but may be improved.
* Enhance existing graphical [plugin for Intellij](https://github.com/jerkar/jeka-ide-intellij)
* Provide a plugin for Android
* Provides a graphical plugin for better integration with Eclipse

Please visit [release note](https://github.com/jerkar/jeka/blob/master/release-note.md) and [issues](issues) for roadmap.


# How to build Jeka ?

Jeka is made of following projects :
* dev.jeka.core : complete Jeka project
* dev.jeka.samples : A sample project with several build classes to illustrate how Jeka can be used in different ways
* dev.jeka.depender-samples : A sample project depending on the above sample project to illustrate multi-project builds. 
These sample projects are also used to run some black-box tests

Jeka builds itself. To build Jeka full distrib from sources, the simpler is to use your IDE.

Once distrib created, add the distrib folder to your PATH environment variable.

## Build Jeka from IntelliJ

* Clone this repository into IntelliJ. Project is already configured (.iml and modules.xml are stored in git).
* Add the `JEKA_USER_HOME` variable pointing on [USER_HOME]/.jeka
* Make sure the project is configured with a JDK8 or higher.
* Run 'FULL BUILD' in Intellij _Run Configurations_ to perform a full build of core + plugins + complete test suite.
* Run 'CoreBuild - skip tests' in Intellij _Run Configurations_ to perform a fast build of the core without tests.


## Build Jeka from Eclipse (outdated)

* Clone this repository in Eclipse. Project is already configured ( *.project* and *.classpath* are stored in git).
* Add the `JEKA_USER_HOME` classpath variable pointing on [USER_HOME]/.jeka 
* Make sure the project is configured to compile using a JDK8 or higher and not a JRE.
* Run `dev.jeka.core.CoreBuildAndIT` class main method. This class is located in *jeka/def* folder. 
* This creates the full distrib in *dev.jeka.core/jeka/output/distrib* folder and run full Integration test suite.



For debuging the project, you have to setup Intellij in order to workaround witj an Intellij issue :
Settings/Preferences | Build, Execution, Deployment | Debugger | Data Views | Kotlin | enable "Disable coroutine agent.
[See here](https://stackoverflow.com/questions/68753383/how-to-fix-classnotfoundexception-kotlinx-coroutines-debug-agentpremain-in-debu)

## How to edit documentation

Documentation is generated with [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/). Documentation sources are located (here)[docs].

You must install _Python_ and _Material for MkDocs_ on your computer (`pip install mkdocs-material`) prior to execute following command lines from this repo root directory :
- `mkdocs serve` : generate and serve the documentation on localhost:8000
- `mkdocs gh-de

The documentation is also supposed to be regenerated after each push/pull-request.


## How to Release

Release is done automatically by Github action on PUSH on *master*.
If the last commit message title contains a word like 'Release:XXX' (case matters ) then a tag XXX is created and 
the binaries will be published on Maven Central.
Otherwise, the binary wll be simply pushed on OSSRH snapshot.

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<img src="docs/images/mascot.png" width='420' height='420' align='center'/>

