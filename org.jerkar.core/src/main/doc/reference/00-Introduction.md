## Introduction
----

This document stands for reference guide and provides details about Jerkar behaviour. If you are new to Jerkar you may find other places to learn about Jerkar :  

* To have an overall vision of Jerkar, please have a [__tour__](http://jerkar.github.io/tour.html).

* To get a step-by-step learning guide, please visit [__tutorial__](http://jerkar.github.io/documentation/latest/getting-started.html).

If you are looking for how exactly Jerkar behaves or you want to get a pretty exhaustive list of Jerkar features, you are in the right document.

However, a document can not replace a source code or API for exhaustion, so please please consult [__javadoc__](http://jerkar.github.io/javadoc/latest/index.html) or browse [__source code__](https://github.com/jerkar/jerkar/tree/master/org.jerkar.core) to get deeper knowledge. 

Also, you're welcome to raise an issue in Git-hub for requesting an improvement on documentation.

### Lexical

These terms are used all over the document, this lexical disambiguates their meanings.

<strong>Build Definition Files :</strong> These are files that define the build for a given project. In Jerkar, those files are Java sources (.java files) located under _[PROJECT DIR]/build/def_ directory. 
A Jerkar project may contain 0 or many build definition files as a definition file stands for both the main build class and _utilities_ classes.
  
<strong>Build Class :</strong> This is the class containing the methods executed by Jerkar when a project is built. 
The build class may be a class coming the compilation of the build definition files or a _standard_ class coming from Jerkar runtime.   
 
<strong>Jerkar Options :</strong> This is a set of key-value used to inject parameters in builds. The options can be mentioned as command line arguments or stored in specific files.
The section _Build Configuration_ details about Jerkar options.

