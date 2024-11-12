# 5 Reasons why I Made JeKa

As a Java developer, I've always appreciated Java static typing and simplicity
but often find surrounding tooling unnecessarily complex and inconvenient. Here’s why I developed JeKa as an alternative:

## 1. Run Java Code Directly

Automating tasks—like generating test data or deploying binaries—often requires writing complex extensions for build tools or using scripts in languages like Bash or Groovy, adding setup and debugging challenges.

I wanted the simplicity of writing Java code that can run from either the IDE or the command line, making it easy to debug, run in CI/CD, and leverage existing Java design skills for handling complex tasks. I also needed scripts that could rely on third-party libraries without manual dependency management.

With JeKa, we can write and execute Java methods directly from the IDE or command line, and add third-party libraries by specifying Maven dependencies directly within the source code.

## 2. Configure Builds with Java

Most Java build tools rely on configuration languages like XML or Kotlin, which add complexity and cognitive load, obscure the internal model, and make it more difficult to extend.

I believe a build tool should simply be a thin component model around a Java library that is configurable and extendable with plain Java code or simple properties for basic cases.

Jeka is designed with these ideas in mind, making build code as navigable, debuggable, and reusable as regular code.

## 3. Write Minimalist Configuration (or not at all)

Setting up a Java build often means defining project names, versions, and plugin details, even for small projects.

Ideally, we should be able to specify zero build configuration to accomplish standard tasks such as compiling, testing, creating regular or uber JARs, compiling to native, or creating Docker images—specifying only library dependencies and filling in additional details as needed.

Jeka relies heavily on conventions to minimize the configuration required to the bare minimum.

## 4. High Portability

Traditional build tool generaly rely on JDK or third-party software installed on the build machine.

Ideally, the build tool should be responsible for fetching the required tooling (such as the JDK) based on the build configuration or defaults. This makes the build more portable and eliminates the need for managing different Java versions.

JeKa achieves this by automatically fetching all necessary tools, including itself, ensuring a consistent experience across all environments.

## 5. Source-Based Distribution

Distributing Java applications is not straightforward: it involves creating JAR files, OS-specific launch scripts, installation/run documentation, infrastructure for hosting binaries, and a process for pushing updates.

A much simplker process would consist in pushing application code to a public Git repository, and let user execute it on their machine with a single command.

Thanks to its high portability, JeKa enables any Java application to be built and run with a single command on most machines, efficiently caching binaries after the initial run for faster execution.


