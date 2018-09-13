## Introduction
----

This document stands for reference guide and provides details about Jerkar behaviour. If you are looking for 
how exactly Jerkar behaves or you want to get a pretty exhaustive list of Jerkar features, you are in the right place.

However, a document can not replace a source code or API for exhaustion. So you are encouraged to navigate into 
source code or Javadoc to get more details.  

The following terms are used all over the document :

__[PROJECT DIR]__ : refers to the root folder of the project to build (the one where you would put pom.xml or build.xml file if you were using ANT or Maven).

__[JERKAR HOME]__ : refers to the folder where is intalled Jerkar. You should find _jerkar.bat_ and _jerkar_ shell scripts directly under this folder.

__[JERKAR USER HOME]__ : refers to the folder where Jerkar stores caches, binary repository and global user configuration.

<strong>Build Classes :</strong> Java source code containing build instructions. These files are edited by the users and are located under _[PROJECT DIR]/build/def_ directory.  
This term can also be use to designate their compiled counterparts (.class files). 

<strong>Build classpath :</strong> Classpath on which depends _build classes_ to get compiled and executed. It consists
in _Jerkar_ core classes but can be augmented with any third party lib or build classes located in another project.
  
<strong>Options :</strong> This is a set of key-value used to inject parameters. Options can be mentioned as command line arguments, stored in specific files or be hard coded in build classes.


