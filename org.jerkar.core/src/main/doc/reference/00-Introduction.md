## Introduction
----

This document stands for reference guide and provides details about Jerkar behaviour. If you are new to Jerkar you may find other places to learn about Jerkar :  

* To have an overall vision of Jerkar, please have a [__tour__](http://jerkar.github.io/tour.html).

* To get a quick starting guide guide, please visit [__Getting Started__](http://jerkar.github.io/documentation/latest/getting_started.html).

If you are looking for how exactly Jerkar behaves or you want to get a pretty exhaustive list of Jerkar features, you are in the right place.

However, a document can not replace a source code or API for exhaustion, so please consult [__javadoc__](http://jerkar.github.io/javadoc/latest/index.html) or browse [__source code__](https://github.com/jerkar/jerkar/tree/master/org.jerkar.core) to get deeper knowledge. 

Also, you're welcome to raise an issue in Git-hub for requesting an improvement on documentation.

### Lexical

These terms are used all over the document, this lexical disambiguates their meanings.

__[PROJECT DIR]__ : refers to the root folder of the project to build (the one where you would put pom.xml or build.xml file using ANT or Maven).

__[JERKAR HOME]__ : refers to the folder where is intalled Jerkar. You should find _jerkar.bat_ and _jerkar_ shell files directly under this folder.

<strong>Build Classes :</strong> These are files that define build for a given project. In Jerkar, those files are Java sources (.java files) located under _[PROJECT DIR]/build/def_ directory and extending `org.jerkar.tool.JkBuild`. 
This term can also be use to designate the compiled build class (.class files) as this class is generated transparently by Jerkar.

In general, there is a single build class by project, but it can have none (if your project embrace conventions strictly) or many if project developers estimates that make sense.   
_[PROJECT DIR]/build/def_ directory may contain other utilities classes and files consumed by build classes.
  
<strong>Options :</strong> This is a set of key-value used to inject parameters in builds. The options can be mentioned as command line arguments or stored in specific files.
See section _Build Configuration_ for more details about options.

