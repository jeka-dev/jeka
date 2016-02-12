## 0.4.0-SNAPSHOT

* Add IntelliJ IDEA plugin for generating iml files (module descriptor).
* "Wrapper" mode (embedding Jerkar jar in the project itself) is achieved by placing jar files in build/boot and not in /build/libs/build anymore.
* Display running Jerkar version in console. 
* Cross-compile. The JDK to compile is choosen according declared source version.
 
## 0.3.2

* Add JkPom class to reason about Maven POM files (retrieving dependencies, dependency management, version, ...)
* Add method on InternalResolver for fetching directly artifacts, regardless dependency management.
* Polish on Repository API

## 0.3.1

* Fix Eclipse .classpath generation (doublon entries for local jar dependencies, exported project dependencies)
* Improve dependency handling between java sub-projects
* Fix auto found main class.
* Add methods on JkDependencies.Builder
 
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