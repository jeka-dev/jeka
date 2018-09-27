# Library Part

As said in intro, Jerkar contains a library for all regulars things you need to build/publish projects and especially Java projects.

The library part embeds third party jar as _Ivy_ or _BouncyCastle_ but these dependencies are hidden and loaded in 
a specific class loader. These 3rd party APIs are not visible/accessible to client code so one can use another 
version of these APIs without conflict : you can consider Jerkar as a *zero-dependency library*.


* files : file trees, filter, zip, path sequence
* system : launching external process, log, info on Jerkar
* cryptography : PGP signer
* dependency management
  * dependency resolution : dependency manager, module dependencies, project dependencies
  * publication : publishing on repositories
* java
  * main : compilation, javadoc, resource processor, manifest, packager, classloader, classpath, launching 
  * junit : launching, report
  * project : project structure to build
* tooling : eclipse integration, intellij integration, Maven integration/migration, Pom parsing
* support
 







