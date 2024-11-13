# 5 Reasons why I Made JeKa

As a Java developer, I've always appreciated Java static typing and simplicity
but often find surrounding tooling unnecessarily complex and inconvenient. Here’s why I developed JeKa as an alternative:

## 1. Run Java Code Directly

Automating tasks—like generating test data or deploying binaries—often requires writing complex extensions for build tools or using extra languages like Bash or Groovy, adding setup, cognitive load, and debugging challenges.

I wanted the simplicity of writing Java code that can run from either the IDE or the command line, making it easy to debug, run in CI/CD, and leverage existing Java design skills for handling complex tasks. I also needed scripts that could rely on third-party libraries without manual dependency management.

With JeKa, we can write and execute Java methods directly from the IDE or command line, and add third-party libraries by specifying Maven dependencies directly within the source code.

## 2. Configure Builds with Java

Most Java build tools rely on configuration languages like XML or Kotlin, which increase complexity when extending the tool or troubleshooting issues. This also sends a discouraging message to newcomers, who feel they need to learn an extra language or deal with complex XML configurations just to work with Java.

I’d expect basic build configuration to be handled with simple properties, while more specific or complex requirements could be addressed with structured Java code that’s easy to refactor, share, and debug.

In addition to its ability to execute Java code, JeKa offers a set of classes and a flexible component model designed to handle simple to complex build tasks. These tasks can be seamlessly configured and executed as standard Java code.

## 3. Write Minimalist Configuration (or not at all)

Setting up a Java build often means defining project names, versions, and plugin details, even for small projects.

Ideally, we should be able to specify zero build configuration to accomplish standard tasks such as compiling, testing, creating regular or uber JARs, compiling to native, or creating Docker images—specifying only library dependencies and filling in additional details as needed.

JeKa minimizes the required configuration to the bare minimum, using defaults and conventions whenever possible

## 4. High Portability

Traditional build tool generaly rely on JDK or third-party software installed on the build machine.

Ideally, the build tool should handle fetching the necessary tools (including the required JeKa and JDK versions) based on the build configuration or defaults. This approach enhances portability and removes the need to manage multiple Java versions.

JeKa achieves this by automatically fetching all necessary tools, including itself, ensuring a consistent experience across all environments.

## 5. Source-Based Distribution

Distributing Java applications is not straightforward: it involves creating JAR files, OS-specific launch scripts, installation/run documentation, infrastructure for hosting binaries, and a process for pushing updates.

A simpler approach would be to push the application code to a public Git repository and allow users to execute it efficiently on their machines with a single command, without additional JVM launch time.

Thanks to its high portability, JeKa enables any Java application to be built and run with a single command on most machines. It caches binaries after the initial run, ensuring direct execution for subsequent runs.

 ## Conclusion
 
While Java as a language has evolved towards greater simplicity and conciseness, the surrounding tooling has not kept pace, remaining complex and configuration-heavy.
Tools like **JeKa**, **Jbang**, and **bld** address this by offering lightweight, minimal-setup environments that streamline build and execution, making Java projects easier to start and manage with just a few commands.
