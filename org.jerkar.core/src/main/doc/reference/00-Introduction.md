# Jerkar - Reference Guide
--------------------------

## Introduction
----

This document stands for reference guide and provides details about Jerkar behaviour. If you are looking for 
how exactly Jerkar behaves or you want to get a pretty exhaustive list of Jerkar features, you are in the right place.

However, a document can not replace a source code or API for exhaustion. So you are encouraged to navigate into 
source code or Javadoc to get more details.  

The following terms are used all over the document, this is their meaning :

__[PROJECT DIR]__ : refers to the root folder of the project to build (the one where you would put pom.xml or build.xml file using ANT or Maven).

__[JERKAR HOME]__ : refers to the folder where is intalled Jerkar. You should find _jerkar.bat_ and _jerkar_ shell files directly under this folder.

<strong>Build Classes :</strong> Java source code containing build statements. These files are located under _[PROJECT DIR]/build/def_ directory.  
This term can also be use to designate their compiled counterparts (.class files). Build classes have dependencies on 
Jerkar classes but can also depend on third party jar or build classes located in another project.

In general, there is a single build class by project, but it can have none (if your project embrace conventions strictly) or many if project developers estimates that make sense.   
_[PROJECT DIR]/build/def_ directory may contain other utilities classes and files consumed by build classes.
  
<strong>Options :</strong> This is a set of key-value used to inject parameters in builds. The options can be mentioned as command line arguments, stored in specific files or be hard coded in build classes.


