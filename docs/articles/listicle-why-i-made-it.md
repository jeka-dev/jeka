# 6 Reasons why I Made JeKa

JeKa is a unique tool for building or executing Java. It was designed with features to simplify and enhance Java development.

## 1. Run Java Code Directly
As a Java developer, I often need to write automated tasks for my projects, such as test data generators, configuration generators, or DevOps orchestration.

Usually, Java developers write plugins/tasks for their build tool, or use scripts in languages like *Bash*, *Groovy*, or *Python*. These solutions are not ideal, as they may require specific (and sometimes cryptic) syntax, unique debugging methods, or additional runtime environments on the host machine.

Ideally, Java developers should be able to write Java code in a project directory, and run it directly from the IDE or command line, without concerns about compilation, dependencies, or JDK setup.

JeKa provides a structure to run Java source code seamlessly from your IDE or the command line—no JDK required—making Java a first-class choice for scripting, from simple tasks to complex needs.

## 2. Build Java with Java
Traditional Java build tools are written in *Java* but expose an external DSL API (XML, Kotlin, etc.) to configure them. This approach isolates public from internal API but makes tools more tedious to extend and harder to debug.

In contrast, JeKa includes a comprehensive Java library for building projects, and a thin layer of reusable components that can be manipulated directly from Java code. This makes JeKa straightforward to extend, debug and troubleshoot with, as there is no extra layer between user code and the processing code.

The entire build and delivery logic can be modeled and implemented in Java, organized as needed to manage complexity and re-use logic across projects effectively.

## 3. Portability without Containers
Traditional build tools, often rely on the presence of a specific JDK or other third-party tools on the host machine. These dependencies make builds less portable, as some requirements are either implicit or accessible only within specific container images.

JeKa approaches this differently by fetching all necessary third-party tools, including the correct JDK and JeKa version, as part of the build itself. This way, tool dependencies and their versions become a defined part of the build process, making builds more portable, consistent, and reproducible across different environments, from Windows workstations to cloud CI/CD. 


## 4. Minimalist Configuration
When starting a Java project, there should be zero setup required beyond specifying library dependencies for coding. Information like group/name, versioning, and other settings can be specified later, if needed.

JeKa is designed with sensible defaults, reducing the need for configuration to a strict minimum. For most common scenarios, you can get started with nearly zero setup and zero build code.

## 5. Distribute Applications as Source
Distributing a Java application traditionally involves:
- Creating JAR files and OS-specific scripts.
- Packaging files in an archive.
- Publishing to a binary repository.
- Providing detailed instructions on how to download and run.

*Why not just publish the source code on a public Git repository?*

With JeKa, you can run an entire Java application directly from its source code. On the first run, the application is built into a JAR and cached, allowing it to be executed efficiently without the need for precompiled binaries.

This makes it incredibly convenient to develop Java tools that clients can start using right away, simply by pointing to a Git URL.

## 6. Simplify Java for Newcomers
Traditionally, Java developers rely on complex build tools that use XML or Kotlin configurations for building projects. This setup often has a steep learning curve, which can be daunting for newcomers and make Java seem overly complicated.

It would be ideal if Java developers could build, run, or publish applications with minimal effort.

JeKa provides these features effortlessly, allowing users to focus on writing effective code while gradually learning the specific configurations as needed.

