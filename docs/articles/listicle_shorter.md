# 6 Reasons why I Made JeKa

As a Java developer, I've always appreciated Java static typing and simplicity 
but often find surrounding tooling unnecessarily complex. Here’s why I developed JeKa as an alternative:

## 1. Run Java Code Directly
Automating tasks—like generating test data or deploying binaries—often means writing plugins for build tools or scripting in languages like Bash or Groovy. 
This adds extra setup and debugging hassle. With JeKa, you can write and run Java code directly from your IDE or command line. 
This lets Java handle scripting tasks naturally, without extra languages or setups.

## 2. Build Java with Java
Most build tools for Java rely on configuration languages like XML or Kotlin, complicating debugging and extensions. 
JeKa lets you configure and extend directly in Java, making build tasks feel like regular Java code. 
This simplifies both troubleshooting and customization.

## 3. High Portability
Traditional build tools often depend on specific JDK versions or third-party software, making builds less portable. 
I wanted a tool that could run consistently across Windows, Linux, and macOS without extra setup. 
JeKa achieves this by fetching any necessary tools or JDK versions automatically, ensuring the same experience everywhere.

## 4. Minimal Configuration
Setting up a Java build often means defining project names, versions, and plugin details, even for small projects. 
I designed JeKa to require minimal configuration: with sensible defaults, 
you can get started immediately, specifying only library dependencies and filling in details later as needed.

## 5. Source-Based Distribution
Distributing Java applications traditionally involves creating JAR files, scripts, documentation (e.g., “how-to-run” guides), 
and hosting binaries—hardly a straightforward process.
JeKa lets you run an app directly from its source code by pointing to its Git repository, 
building it automatically on the first run.
Simply tagging the repository is all it takes to create a new release.

## 6. Make Java Easier for Newcomers
Build tools for Java often use complex XML or Kotlin configurations, which can be overwhelming for beginners. 
Additionally, switching JDK versions involves adjusting configurations and environment variables. 
JeKa reduces these hurdles, letting new developers focus on writing and running code, with an easier setup for switching Java versions.

JeKa aims to simplify Java tooling by focusing on portability, ease of use, and minimal setup—making Java projects more accessible and straightforward.