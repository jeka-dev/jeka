[![Build Status](https://travis-ci.org/jerkar/jerkar.svg?branch=master)](https://travis-ci.org/jerkar/jerkar)
[![Maven Central](https://img.shields.io/maven-central/v/org.jerkar/core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.jerkar%22%20AND%20a:%22core%22) <br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<img src="http://jeka.dev/images/logo-whole-bg.jpg" width='420' height='420' align='middle'/>

# What is Jerkar

<strong>Jeka</strong> is a complete **Java build system** ala _Ant_, _Maven_, _Gradle_ or _Buildr_ using **pure Java** to automate your tasks. No XML or scripts : only rock solid Java code.

Enjoy all the engineering power you are comfortable with (Java code, IDE, 3rd party libs,  Maven repository, ...) to define, 
design, run, debug, reuse your automated tasks/builds. Exactly as you would do with regular code.

Also, __Jeka conventions and plugin mechanism are so powerful__ that it can perform pretty exotic tasks without needing a single line of code/configuration. 

For example `jerkar java#pack jacoco# sonar#run -sonar#host.url=http://myserver/sonar`
performs a complete build running unit tests under Jacoco coverage tools and performs Sonar analysis on a Java project free 
of any build-code / configuration / script. 

# News

__Jerkar__ is being rebranded as __Jeka__. While this rebranding is under process 'Jeka' and 'Jerkar' term may be used indifferently. 
 
Jeka now relies on JDK 8 while versions 0.6 and prior are relying on JDK 6.
In order to leverage new features bring by these versions, **Jeka has been deeply reworked**. <br/>
Versions from 0.7.0 are pretty stables.

To avoid bloating API with legacy stuff, Jeka won't be compatible with Jerkar 0.6 (you can still run current builds using embedded mode). 
We expect from this move a much more clean, polished and intuitive product.

Documentation is now entirely hosted in this repository to better sync. Great progress has been done recently.
Please visit [latest documentation](org.jerkar.core/src/main/doc).

# Get Jerkar

* Snapshots : https://oss.sonatype.org/content/repositories/snapshots/org/jerkar/core/
* Releases : https://repo1.maven.org/maven2/org/jerkar/core/

The distribution is the file named core-x.x.x-distrib.zip. 

# How to use Jerkar

Jerkar is designed to be easy to master for Java developers. In despite it offers rich functionalities, 
developers should be able to figure out how it works by knowing few concepts and navigate in source code.

That said, documentation is needed for a starting point.

Visit following pages according your expectation :
* [Getting Started](org.jerkar.core/src/main/doc/Getting%20Started.md)
* [Reference Guide](org.jerkar.core/src/main/doc/Reference%20Guide)
* [Frequently Asked Questions](org.jerkar.core/src/main/doc/FAQ.md)
* [Javadoc](https://jeka.dev/docs/javadoc)
* [Working examples](https://github.com/jerkar/working-examples)

# Community

You can ask question using regular using [this repository issues](https://github.com/jerkar/jerkar/issues).

You can also use direct emailing for questions and support : djeangdev@yahoo.fr

A twitter account also exist : https://twitter.com/djeang_dev

# How to build Jerkar

Jerkar is made of following projects :
* org.jerkar.core : complete Jerkar project
* org.jerkar.samples : A sample project with several build classes to illustrate how Jerkar can be used in different ways
* org.jerkar.samples-dependee : A sample project depending on the above sample project to illustrate multi-project builds. 
These sample projects are also used to run some black-box tests

Jerkar builds itself. To build Jerkar full distrib from sources, the simpler is to use your IDE.

Once distrib created, add the distrib folder to your PATH environment variable.

## Build Jerkar from Eclipse

* Clone this repository in Eclipse. Project is already configured ( *.project* and *.classpath* are stored in git). 
* Make sure the project is configured to compile using a JDK8 and not a JRE.
* Run `org.jerkar.CoreBuild` class main method. This class is located in *build/def* folder. 
* This creates the full distrib in *org.jerkar.core/build/output/distrib* folder

## Build Jerkar from IntelliJ

* Clone this repository into IntelliJ. Project is already configured (.iml and modules.xml are stored in git).
* Make sure the project is configured with a JDK8.
* Run `org.jerkar.CoreBuild` class main method. This class is located in *build/def* folder, inside *org.jerkar.core* project.
* This creates the full distrib in *org.jerkar.core/build/output/distrib* folder

