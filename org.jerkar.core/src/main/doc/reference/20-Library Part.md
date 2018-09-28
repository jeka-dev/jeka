# Library Part

As said in intro, Jerkar contains a library for all regular things you need to build/publish projects and especially Java projects.

The library part embeds third party jar as _Ivy_ or _BouncyCastle_ but these dependencies are hidden and loaded in 
a specific class loader. These 3rd party APIs are not visible/accessible to client code so one can use another 
version of these APIs without conflict : you can consider Jerkar as a *zero-dependency library*.


* __Files :__ File trees, filters, zip, path sequence
* __System :__ Launching external process, Logging, Meta-info
* __Cryptography :__ PGP signer
* __Dependency management :__ Dependency management, publishing on repositories
* __Java :__ Compilation, javadoc, resource processor, manifest, packager, classloader, classpath, launching 
  * __Junit :__ Launching, report
  * __Project :__ Project structure to build
* __Tooling :__ Eclipse integration, intellij integration, Maven interaction
* __Support__
 







