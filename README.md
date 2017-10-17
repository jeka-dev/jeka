[![Build Status](https://travis-ci.org/jerkar/jerkar.svg?branch=master)](https://travis-ci.org/jerkar/jerkar)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.jerkar/core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.jerkar/core) <br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
<img src="http://jerkar.github.io/img/logo/PNG-01.png" width='350' height='420' align='middle'/>

<strong>Jerkar</strong> is a complete **build system** ala _Ant_, _Maven_, _Gradle_ or _Buildr_ but using **pure Java** to describe builds : no XML, no foreign language.
Breaking a common belief, it makes proof that Java is perfectly suitable in this domain.

# News
 
 
Master version now relies on JDK 8 while 0.6 and prior are relying on JDK 6.
In order to leverage new features bring by these versions, **Jerkar is deeply reworked**. <br/>
Also, to avoid bloating API with legacy stuff, Jerkar 0.7 won't be compatible with Jerkar 0.6 (you can still run current builds using embedded mode). 
We expecpt from this move a much cleaner and polished API for a more intuitive product.

# How to use Jerkar

Jerkar is expected to have a very fast learning curve for Java developers. You can visit the following page in this order :

* http://jerkar.github.io/tell-me-more.html : introduction to Jerkar. Answer to the question : *What Jerkar is exactly ?*
* http://jerkar.github.io/tour.html : to give a concrete idea on how Jerkar is working
* http://jerkar.github.io/documentation/latest/getting_started.html : to get hand-on experience
* http://jerkar.github.io/documentation/latest/reference.html : to know the details about Jerkar behavior
* https://github.com/jerkar/jerkar-examples : Examples of project built with Jerkar.

# How to build Jerkar
Jerkar is made of following projects :
* core : complete Jerkar project but without embedding following plugins
* plugins-jacoco : a plugin to perform test coverage
* plugins-sonar : a plugin to perform sonar analysis
* distrib-all : the core distrib augmented with the above plugins

Jerkar builds itself. To get Jerkar full distrib built from the Java sources only, the simpler is to build it from your IDE.

## Build Jerkar from Eclipse

* Import the 4 projects described above in Eclipse (that already holds *.project* and *.classpath* files) 
* Create a Java Application run configuration (Run Configuration ... -> Java Application -> new)
    * Make sure that the Runtime JRE is a JDK (6 or above)
    * Choose `org.jerkar.distrib-all` as project
    * Choose `org.jerkar.tool.Main` as Main class
* Run it : It will launch a multi-project build. You will find result for the full distrib in *org.jerkar.distrib-all/build/output* directory 

## Build Jerkar from IntelliJ

You can achieve the same using **Intellij** as Intellij module definitions (.iml) are stored in git. You should get a single project containing 4 modules.
Execute 'DistribAllBuild' run config to build the full distrib in *org.jerkar.distrib-all/build/output* directory.

# Status

First releases has been delivered. It contains all features a self respecting build tool must have : from compilation to publication features.
Now we'll plan to deliver new releases at fast pace. You're welcome to push your expectation for next releases. 

# Want to contribute ?

Jerkar welcomes contributors. As a new project there's plenty of free rooms to start : You can extends/debug the jerkar project itself but you van also write addin/plugin for integrate better Jerkar with your favorite technology. Don't be intimidated, it's relatively easy and you can provide great added value just by writing very few code. As an example, look at the [Spring Boot addin](https://github.com/jerkar/spring-boot-addin). 
Also do not hesitate to [contact contributors](https://github.com/djeang) to discuss about what is best to start with.

## Coding rule
If you contribute to Jerkar Core project, there's only 1 rule : try to mimic the current style :-).
More concretely :

* Make a class public only when really needed. If a class is public, it should be prefixed with `Jk` (The goal is to not pollute auto-completion in IDE when Jerkar is on the build path).
* Favor immutable objects.
* Embrace a fluent style API (so stay away from JavaBean coding conventions).
* Don't use 3rd party dependencies (Use or enrich JkUtilsXxxxx classes for commons). 
* Jerkar 0.7.x relies on JDK 8
