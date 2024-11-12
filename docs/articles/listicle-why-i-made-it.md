# 5 Reasons why I Made JeKa

As a Java developer, I've always appreciated Java static typing and simplicity
but often find surrounding tooling unnecessarily complex and inconvenient. Here’s why I developed JeKa as an alternative:

## 1. Run Java Code Directly

Automating tasks—like generating test data or deploying binaries—often requires writing complex extensions for build tools or using scripts in languages like Bash or Groovy, adding setup and debugging challenges.

I wanted the simplicity of writing Java code that leverages the Java ecosystem, runnable from either the IDE or command line, allowing for easy debugging and the benefits of object-oriented design.

With JeKa, you can write and execute Java method directly from the IDE or command line, and add third-party libraries by specifying Maven dependencies directly within the source code.

## 2. Build Java with Java

Most Java build tools rely on configuration languages like XML or Kotlin, which can complicate debugging and extending for developers.

I believe configuration is best done in Java (or with properties for simpler cases), maintaining consistency
between development and tooling configuration/extension.

With JeKa, configuration and extensions are written directly in Java, making build tasks as navigable,
debuggable and reusable as regular code.

## 3. Minimal Configuration

Setting up a Java build often means defining project names, versions, and plugin details, even for small projects.

Ideally, we should be able to specify zero build configuration to compile, test, package or dockerize an application.

With Jeka, you can get started immediately, specifying only library dependencies and filling in details later as needed.

## 4. High Portability

With traditional build tools, builds often rely on specific JDK versions or third-party software installed on the build machine.

I wanted a tool that minimizes these dependencies so builds run seamlessly from any environment, without requiring a pre-installed JDK on the build machine and making Java version changes easy.

JeKa achieves this by automatically fetching any necessary tools or JDK versions, ensuring a consistent experience everywhere.


## 5. Source-Based Distribution

Distributing Java applications is often complex: it involves creating JAR files, OS-specific launch scripts, installation/run documentation, infrastructure for hosting binaries, and a process for pushing updates.

Ideally, we should be able to push code to a public Git repository, allowing users to run it with a single command.

Thanks to its high portability, JeKa enables any Java application to be built and run with a single command on most machines, efficiently caching binaries after the initial run for faster execution.


