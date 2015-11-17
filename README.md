![Logo of Jerkar](http://jerkar.github.io/img/logo.png)

<strong>Jerkar</strong> is a complete **build system** ala _Ant_, _Maven_, _Gradle_ or _Buildr_ but using **pure Java** to describe builds : no XML, no foreign language.
Breaking a common belief, it makes proof that Java is perfectly suitable in this domain.

See [official web site](http://jerkar.github.io) for more description.

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

## Build Jerkar from Intellij

Youu can achieve the same using **Intellij** as Intellij module definitions (.iml) are stored in git. `org.jerkar.distrib-all` contains the project definition (.idea folder).

# Status

First release has been delivered. It contains all features a self respecting build tool must have : from compilation to publication features.
Now we'll plan to deliver new releases at fast pace. You're welcome to push your expectation for next releases. 


# Using Jerkar

Jerkar is expected to have a very fast learning curve for Java developers. You can visit the following page in this order :

* http://jerkar.github.io/tell-me-more.html : introduction to Jerkar. Answer to the question : *What Jerkar is exactly ?*
* http://jerkar.github.io/tour.html : to give a concrete idea of how Jerkar is working
* http://jerkar.github.io/documentation/latest/getting_started.html : to get hand-on experience
* http://jerkar.github.io/documentation/latest/reference.html : to know the details about Jerkar behavior


# How to contribute ?

The most wanted skill on this project is technical writing. The documentation is pretty complete but the style should be improved.  
So if you fell ok to review/improve some documentation, welcome on board !

If you want contribute to code, 1 rule : try to mimick the current style :-).
More concretely :

* Make a class public only when really needed. If a class is public, it should be prefixed with `Jk` (The goal is to not polute auto-completion in IDE when Jerkar is on the build path).
* Favor immutable objects.
* Embrace a fluent style API (so stay away from JavaBean coding conventions).
* Don't use 3rd party dependencies (Use or enrich JkUtilsXxxxx classes for commons). 
* Make your code JDK6 complient (probably switch to JDK 7/8 soon).
