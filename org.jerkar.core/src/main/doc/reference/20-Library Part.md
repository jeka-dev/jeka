# Library Part

Jerkar contains a library for dealing with files, compilations, dependency management, tests, 
external process, crypto signatures, ... In a glance, all regulars things you need to build/publish projects and especially Java projects.

Even if bundled in the same jar, the library does not depend of the tool part, so you can use it in your own 
tooling, or in a basic main class.

As Jerkar tool is relatively lightweight comparing to its library, I see no compelling reason to split it in two jars.

The library part embeds third party products as _Ivy_ or _BouncyCastle_ but these dependencies are hidden and loaded in 
a specific class loader. These 3rd party APIs are not visible/accessible to clients so they can use their own 
version of these APIs without conflict.




