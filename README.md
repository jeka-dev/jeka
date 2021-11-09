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
- Use standard plugins to implement/custom common tasks.

<br/>
<sub>This is an example of a build class for a simple Springboot project.</sub>

```java
@JkDefClasspath("dev.jeka:springboot-plugin:3.0.0.RC7")
class Build extends JkClass {

    private final JkPluginSpringboot springboot = getPlugin(JkPluginSpringboot.class);

    public boolean runIT = true;

    @Override
    protected void setup() {
        springboot.setSpringbootVersion("2.2.6.RELEASE");
        springboot.javaPlugin().getProject().simpleFacade()
            .setCompileDependencies(deps -> deps
                .and("org.springframework.boot:spring-boot-starter-web")
                .and("org.projectlombok:lombok:1.18.20")
            )
            .setRuntimeDependencies(deps -> deps
                .minus("org.projectlombok:lombok")
            )
            .setTestDependencies(deps -> deps
                .and("org.springframework.boot:spring-boot-starter-test")
                    .withLocalExclusions("org.junit.vintage:junit-vintage-engine")
            )
            .addTestExcludeFilterSuffixedBy("IT", !runIT);
    }

    public void cleanPack() {
        clean();
        springboot.projectPlugin().pack();
    }

}
```
<sub>To build the project, execute ´cleanPack´ from your IDE or execute the following command line.</sub>
```
/home/me/myproject>./jekaw cleanPack -runIT=false
```

# User friendly
Thanks to wrapper and [Jeka Plugin for Intellij](https://github.com/jerkar/jeka-ide-intellij), you don't need to install anything on your machine. 
You only need a JDK 8 or higher.

Getting started in 10 minutes : [Here](https://github.com/jerkar/jeka/blob/master/dev.jeka.core/src/main/doc/Getting%20Started%20-%20IDE.md)

# News 

* Jeka built-in plugins Jacoco and Sonar are removed from the main distribution in favor of up-to-date external plugins :
  * [Sonarqube Plugin](https://github.com/jerkar/sonarqube-plugin)
  * [Jacoco Plugin](https://github.com/jerkar/jacoco-plugin)

* Support for Windows has been improved

* Jeka 0.9.x serie is out. 0.9.x aims at providing intermediate API polishes and improvements
  prior to go to 1.0.0.alpha. It will also provide necessary features to interact with a first class Intellij plugin.
     
* Jeka has joined OW2 organisation on january 2020 : https://projects.ow2.org/view/jeka/

Last major additions :

* Reworked [dependency management](https://github.com/jerkar/jeka/blob/master/dev.jeka.core/src/main/doc/Reference%20Guide/2.3.%20Dependency%20management.md)
* Completely renewed API, now embracing widely *Parent Chaining*.
* Test engine now relies on Junit 5 (still compatible with Junit 3&4)
* Release of a [plugin for Intellij](https://github.com/jerkar/jeka-ide-intellij)
* Upgraded to Ivy 2.5.0
* Wrapper to run Jeka independently of the Jeka version installed on the host machine
* Jdk9+ compatibility
* Deploying on Maven central though a modern release process (version numbering based on Git instead of being hardcoded).
Jeka now uses these features to release itself.

# Roadmap/Ideas
 
* Improve landing page and provide tutorials based on Intellij plugin for easy1/fast starting. 
* Stabilise api from user feedbacks. API is quite workable now but may be improved.
* Enhance existing graphical [plugin for Intellij](https://github.com/jerkar/jeka-ide-intellij)
* Integrate Kotlin as a first citizen language for both building Kotlin projects and write Jeka command classes.
* Provide a plugin for Android
* Provides a graphical plugin for better integration with Eclipse

Please visit [release note](https://github.com/jerkar/jeka/blob/master/release-note.md) and [issues](issues) for roadmap.

# Get Jeka

* Snapshots : https://oss.sonatype.org/content/repositories/snapshots/dev/jeka/jeka-core/
* Releases : https://repo1.maven.org/maven2/dev/jeka/jeka-core/

The distribution is the file named jeka-core-x.x.x-distrib.zip. 

# How to use Jeka ?

Jeka is designed to be easy to master for Java developers. It is easy to figure out how it works by knowing few 
concepts and navigate in source code.

That said, documentation is needed for a starting point.

Visit following pages according your expectation :
* [Getting Started](dev.jeka.core/src/main/doc/Getting%20Started%20-%20IDE.md)
* [Reference Guide](dev.jeka.core/src/main/doc/Reference%20Guide)
* [Frequently Asked Questions](dev.jeka.core/src/main/doc/FAQ.md)
* [Javadoc](https://jeka.dev/docs/javadoc)
* [Working examples](https://github.com/jerkar/working-examples)

# External plugins

Jeka comes with plugins out of the box. These plugins covers the most common points a Java developer need to address 
when building a project. This includes plugins for IDE metadata generation (IntelliJ, Eclipse), dependency management, git, , java project building, testing, PGP signing, binary repositories, Maven interaction, scaffolding, sonarQube and web archives.

Nevertheless, Jeka is extensible and other plugins exist outside the main distib among :
* [Springboot Plugin](https://github.com/jerkar/springboot-plugin)
* [Protobuf Plugin](https://github.com/jerkar/protobuf-plugin)
* [Sonarqube Plugin](https://github.com/jerkar/sonarqube-plugin)
* [Jacoco Plugin](https://github.com/jerkar/jacoco-plugin)

# Community

<a class="btn btn-link btn-neutral" href="https://projects.ow2.org/view/jeka">
              <img src="https://jeka.dev/images/ow2.svg" alt="Image" height="60" width="60"></a>
              
This project is supported by OW2 consortium.
              
              

You can ask question using regular using [this repository issues](https://github.com/jerkar/jerkar/issues).

You can also use direct emailing for questions and support : djeangdev@yahoo.fr

A twitter account also exist : https://twitter.com/djeang_dev

# How to build Jeka ?

Jeka is made of following projects :
* dev.jeka.core : complete Jeka project
* dev.jeka.samples : A sample project with several build classes to illustrate how Jeka can be used in different ways
* dev.jeka.depender-samples : A sample project depending on the above sample project to illustrate multi-project builds. 
These sample projects are also used to run some black-box tests

Jeka builds itself. To build Jeka full distrib from sources, the simpler is to use your IDE.

Once distrib created, add the distrib folder to your PATH environment variable.

## Build Jeka from Eclipse

* Clone this repository in Eclipse. Project is already configured ( *.project* and *.classpath* are stored in git).
* Add the `JEKA_USER_HOME` classpath variable pointing on [USER_HOME]/.jeka 
* Make sure the project is configured to compile using a JDK8 or higher and not a JRE.
* Run `dev.jeka.core.CoreBuildAndIT` class main method. This class is located in *jeka/def* folder. 
* This creates the full distrib in *dev.jeka.core/jeka/output/distrib* folder and run full Integration test suite.

## Build Jeka from IntelliJ

Note that it exists a now an IntelliJ plugin to integrate Jeka to get rid of the following setup.

* Clone this repository into IntelliJ. Project is already configured (.iml and modules.xml are stored in git).
* Add the `JEKA_USER_HOME` variable pointing on [USER_HOME]/.jeka 
* Make sure the project is configured with a JDK8 or higher.
* Run `dev.jeka.core.CoreBuildAndIT` class main method. This class is located in *jeka/def* folder, inside *dev.jeka.core* module.
  Make sure to run it using `$MODULE_WORKING_DIR$` as working directory.
* This creates the full distrib in *dev.jeka.core/jeka/output/distrib* folder  and run full Integration test suite.

To build the project without running whole integration test suite, run `dev.jeka.core.CoreBuild` class main method.

For debuging the project, you have to setup Intellij in order to workaround witj an Intellij issue :
Settings/Preferences | Build, Execution, Deployment | Debugger | Data Views | Kotlin | enable "Disable coroutine agent.
[See here](https://stackoverflow.com/questions/68753383/how-to-fix-classnotfoundexception-kotlinx-coroutines-debug-agentpremain-in-debu)

## How to Release ?

Release is done automatically by Github action on PUSH on *master*.
If the last commit message title contains a word like 'Release:XXX' (case matters ) then a tag XXX is created and 
the binaries will be published on Maven Central.
Otherwise, the binary wll be simply pushed on OSSRH snapshot.

To really deploy to Maven central, a manual action it still needed to [close/release repository](https://oss.sonatype.org).

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<img src="http://jeka.dev/images/logo-whole-bg.jpg" width='420' height='420' align='center'/>

