## 0.3.1-SNAPSHOT

* Fix Eclipse .classpath generation (doublon entries for local jar dependencies)

## 0.3.0

* Add "@" import syntax to import build dependencies from the command line (Usefull for scaffolding projects).
* Improve handling of version definition in JkBuildDependencySupport template (add final effectiveVersion() method).
* Fix dependencies management when compiling/running build classes
* Fix Eclipse file generator about JRE container name.
* JkClassloader : add static method to auto-discover class having a main method  
* JkJavaBuild : provide support for populating manifest.
* JkJavaBuild : modify default scope mapping.
* Scaffolding is now alterable by plugins
* Move low level utility methods for Zip in JkUtilsZip class. 
* The Jerkar java process now launches Jerkar fat jar and not the normak jerkar jar + ext libs  
* Improved Javadoc
 
## 0.2.7

First official release.