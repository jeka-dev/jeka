[![Build Status](https://travis-ci.org/jerkar/jerkar.svg?branch=master)](https://travis-ci.org/jerkar/jerkar)
[![Maven Central](https://img.shields.io/maven-central/v/org.jerkar/core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.jerkar%22%20AND%20a:%22core%22) <br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<img src="http://jeka.dev/images/logo-head-big.png" width='350' height='420' align='middle'/>

<strong>Jerkar</strong> is a complete **Java build system** ala _Ant_, _Maven_, _Gradle_ or _Buildr_ using **pure Java** to automate your tasks. No XML or scripts : only rock solid Java code.

Enjoy all the engineering power you are comfortable with (Java code, IDE, 3rd party libs,  Maven repository, ...) to define, 
design, run, debug, reuse your automated tasks/builds. Exactly as you would do with regular code.

Also, __Jerkar conventions and plugin mechanism are so powerful__ that it can perform pretty exotic tasks without needing a single line of code/configuration. 

For example `jerkar java#pack jacoco# sonar#run -sonar#host.url=http://myserver/sonar`
performs a complete build running unit tests under Jacoco coverage tools and performs Sonar analysis on a Java project free 
of any build-code / configuration / script. 

# News
 
Master version now relies on JDK 8 while 0.6 and prior are relying on JDK 6.
In order to leverage new features bring by these versions, **Jerkar is deeply reworked**. <br/>

To avoid bloating API with legacy stuff, Jerkar 0.7 won't be compatible with Jerkar 0.6 (you can still run current builds using embedded mode). 
We expect from this move a much more clean, polished and intuitive product.

Documentation is now entirely hosted in this repository to better sync. Great progress has been done recently.
Please visit [latest documentation](org.jerkar.core/src/main/doc).

Note that you can still access Jerkar 0.6 documentation from [official web-site](http://project.jerkar.org/).

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

# Status

Last release contains all features a self respecting build tool must have : from compilation to publication features.
Currently Jerkar is undergoing a deep reworking in order to leverage of Java8 features. API is getting more polished 
but cannot be considered as stable for now.
