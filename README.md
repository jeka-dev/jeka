![Logo of Jerkar](http://jerkar.github.io/img/logo.png)

<strong>Jerkar</strong> is a complete **Java** build system ala _Ant_, _Maven_, _Gradle_ or _Buildr_ but using **pure Java** to describe builds : no XML, no foreign language.
Breaking a common belief, it makes proof that Java is perfectly suitable in this domain.

See [official web site](http://jerkar.github.io) for more description.

# How to build Jerkar
Jerkar is made of following projects :
* core : complete Jerkar project but without embedding following plugins
* plugins-jacoco : a plugin to perform test coverage
* plugins-sonar : a plugin to perform sonar analysis
* distrib-all : the core distrib augmented with the above plugins

Jerkar builds itself. To get Jerkar full distrib built from the Java sources only, the simpler is to import these 4 projects in Eclipse, then :
* Create a Java Application run configuration (Run Configuration ... -> Java Application -> new)
    * Make sure that the Runtime JRE is a JDK (6 or above)
    * Choose `org.jerkar.distrib-all` as project
    * Choose `org.jerkar.tool.Main` as Main class
* Run it : It will launch a multi-project build. You will find result for the full distrib in *org.jerkar.distrib-all/build/output* directory 


# Status

Both documentation and code are pretty close to completion for a first release (expecting 2015 Q4) but the product is currently quite usable.
I mainly need help for further testing and providing some feedback.

# Code guidelines

In a nutshell : try to mimick the current style :-)
* Make a class public only when really needed. If a class is public, it should be prefixed with `Jk` (The goal is to not polute auto-completion in Eclipse).
* Favor immutable objects.
* Embrace a fluent style API (so stay away from JavaBean coding conventions).
* Don't use 3rd party dependencies (Use or enrich JkUtilsXxxxx classes for commons). 
* Make your code JDK6 complient
