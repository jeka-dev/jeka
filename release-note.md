# 0.9.14.RELEASE
* Fix distribution zip archive
* Enhance Windows jeka.bat script   

# 0.9.13.RELEASE
* Modify Scaffold API. Use functional programming for late invoking. 

# 0.9.12.RELEASE
* Improve error message on Windows when JDK is not found
* Add a Java scaffold template for creating Jeka plugins

# 0.9.11.RELEASE
* Fix dependency issue when importing module via command-line '@'
* Fix running explicit Build class
* Minor console output improvement

# 0.9.10.RELEASE
* Deep rework of dependency management API. Removal of 'scope' context.
* Fix plugin version compatibility checker
* Fix duplicate setup on nested projects
* Add automated extra command line arguments via jeka/cmd.properties file 
* More consistent compilation handling
* Minor fixes on JkPluginPom
 
# 0.9.4.RELEASE
* Rename JkCommandSet to JkClass
* Improve log messages on plugin compatibility break
* Rename plugin lifecycle methods to `beforeSetup` and `afterSetup`.
* Improved log setup
* Add BRACE and INDENT logger style : set Default logger style to INDENT
* New log options (LRI, LB, LSU)

# 0.9.1.RELEASE
* Buf Fix on scaffolding Java project
* Typo fixes

# 0.9.0.RELEASE
* Add a simple facade over JkJavaProject to address common case conveniently
* Renaming JkJavaProjectJarProduction to JkJavaProjectConstruction
* Use same version comparator as Maven
* Clean Gpg Plugin
* Plugin version compatibility check Experimental
* Can make public field / property in JkCommandSet/JkPlugin not appear has option, using @JkDoc(hide = true)


# 0.9.0.M11
* Bug fixes related to JDK11+
* Bug Fixes related to GPG defaults

# 0.9.0.M4
* Improve JkManifest
* Various bux fixes

# 0.9.0.M3
* Bug-fix on JkDependencySet#add

# 0.9.0.M2
* API polish
* Minor bug fixes

# 0.9.0.M1
* Completely renewed API, now based on *parent chaining* pattern.
* Full support of JUnit5

# 0.8.20
* .iml generation : avoid module entry duplicates
* Add JkClassloader#findClassesMatchingAnnotations

# 0.8.19
* IntelliJ plugin : possible to add extra module dependencies in generated iml
* IntelliJ plugin : possible to skip jeka.jar dependency in generated iml

# 0.8.18
* Api Polish : `JkCommands` renamed to `JkCommandSet`
* Api Polish : `JkImportProject` renamed to `JkDefImport`
* Api Polish : `JkImport` renamed to `JkDefClasspath`
* Api Polish : `JkImportRepo` renamed to `JkDefRepo`
* Api Polish : In configuration files, `repo.run.url` renamed to `repo.def.url`
* Plugins can declare compatibility constrains on Jeka version

# 0.8.17 
* Better console output for `JkCommandSet` initialization
* Fix JkInterpolator#and

# 0.8.16
* Fix source attachment in .iml generation
* Fix classloader issues on Java 9+
* Setup slave commands from a master commands class.
* API polish

# 0.8.15
* Compare JkVersion according semantic version
* generate wrapper pointing on another location (for multi-projects)
* API polish on JkCommands
* Fix JkPluginWar when no static resource directory is present

# 0.8.14
* Fix JDK version recognition for version 9 and greater
* Let generate Eclipse/Intellij metadata without using JkPluginJava
* Fix warning on standard option usage
* Use private field with public setter to inject options in command classes
* Fix JkPathMatcher#or
* Fix JkLog.Event ClassCastException when run in external classloader
* Let write def classes in Kotlin (experimental)

# 0.8.13
* Generate jekaw *nix script with execution permissions
* Remove JkLog#execute method
* Log warning when an command line option does not match any field
* Fix make build failure on deps resolution failure by default

# 0.8.12
*  Eclipse plugin : Generate specific 'attributes' and 'accessrules' to each dependency.
*  Fix JkDependencySet#and(JkDependencySet) 
*  Make build failure on deps resolution failure by default
*  Move def compiled classes to _jeka/.work_ 

# 0.8.11
* Move to Ivy 2.5.0
* Fix `JkPathTree` bugs related to zip files.
* Preserve manifest of the main jar when creating fat jars.
* Fix small bugs in `JkManifest` 
* Fix small bugs for Eclipse .classpath file generation
* Classpath scanning for finding 'main' classes with Java 9+

# 0.8.10 
* Display JkException stacktrace when failing in verbose mode
* Display Exception stacktrace when `JkPluginIntellij#allIml` fails
* Add `@JkCompileOption` to inject compile options to def class compiler
* Add wildcard usage in `@JkImport`

# 0.8.9
* Fix NPE when invoking `JkLog.startTask` on a new Thread
* Handle duplicate dependency declaration on a `JkDependencySet`
* Fix issue when generating fat jar along original jar 

# 0.8.8
* Fix classloading error with wrapper
* Fix classloading error on when using jdk9+
* Fix dependency scope issue taking too large scopes

# 0.8.7

* Fix download repo NPE introduced in 0.8.6
* Fix Java version naming in Eclipse .classpath generation
* Handle Intellij Platform Plugin in iml generation  

# 0.8.6

* Fix wrapper failure at downloading
* More intuitive behavior for JkPluginRepo 
* Improve Windows and Linux script : JEKA_HOME environment variable no more required to be set
* Windows : fix problem with project paths containing spaces
 
# 0.8.5

* Wrapper fully implemented(multi-project, configurable location)
* Dependency resolver can find groups, modules and versions available on remote repositories
* Getting started doc improved
* Small bug fixes and improvements

# 0.8.4

* Partial jekaw wrapper (working on single projects)

# 0.8.3

* Jeka can now run on JDK version greater or equal to 8 (tested until Jdk 12).
* JkPathTree polish and bug fix related to zip archive.

# 0.8.2

* Reworked mechanism for embedding 3rd party jar within jeka-core.jar
* Improved general performances and lower resource consumption
* Bug fixes

# 0.8.1

* Minor adaptation for better plugin integration
* Minor bug fixes

# 0.8.0

* Change from org.jerkar to dev.jeka namespace.
* Package classes has all been renamed (no backward compatibility with 0.7.x)
* Bug fixes

# 0.7.0

* Move from jdk6 to jdk8
* Documentation hosted in Jerkar repository for better sync.
* Deeply reworked plugin mechanism
* Completely new build API based on Java8 lambdas
* Completely new file API based on Java7 nio
* Removed legacy API (no backward compatibility with Jerkar 0.6.x)

# 0.6.0

* Deep dependency resolution reworking : entirely based on tree leading in a greater accuracy/control.
* Fix issue #60 related to artifacts with classifier.
* Handle Eclipse project to project dependencies while generating .classpath (issue #61)
* Generate Intellij modules.xml files
* Take scope into account when generating Intellij iml files
* Many bug fixes

# 0.5.0

* Add `showDependencies` on `JkBuildDependencySupport` that display project dependency tree.
* Add `JkDependencyNode` to explore and reason about dependency trees.
* Cleaner log output
* Fix bug related to artifact resolution failure in local repositories
* Better Eclipse support
* Possibility to use any JSR199 compiler (as ECJ) instead of Java
* API polish
* Minor bug fixes
 

# 0.4.6

* Resolved classpath follows dependency declaration order
* Fat jars now exclude signature files by default
* Possibility to add arbitrary files in jar produced by JkJavaPacker
* Add method on JkJavaBuildPlugin to alter source code generation 
* Bug fixes

# 0.4.5

* Easier repository/credential management
* Enhanced scaffolding
* Export build class method description to XML file (tooling purpose)
* API polish
* Minor bug fixes

# 0.4.0

* Add IntelliJ IDEA plugin for generating iml files (module descriptor).
* "Wrapper" mode (embedding Jerkar jar in the project itself) is achieved by placing jar files in build/boot and not in /build/libs/build anymore.
* Display running Jerkar version in console. 
* Cross-compile. The JDK to compile is chosen according declared source version.
 
# 0.3.2

* Add JkPom class to reason about Maven POM files (retrieving dependencies, dependency management, version, ...)
* Add method on InternalResolver for fetching directly artifacts, regardless dependency management.
* Polish on Repository API

# 0.3.1

* Fix Eclipse .classpath generation (doublon entries for local jar dependencies, exported project dependencies)
* Improve dependency handling between java sub-projects
* Fix auto found main class.
* Add methods on JkDependencies.Builder
 
# 0.3.0

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
 
# 0.2.7

First official release.
