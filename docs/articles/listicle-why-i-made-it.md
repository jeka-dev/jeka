# 6 Reasons why I Made JeKa

JeKa is a unique tool for building or executing Java. It was designed with features to simplify and enhance Java development.

## 1. Run Java Code Directly
As a Java developer, I often need to write automated tasks for my projects, such as test data generators, configuration generators, or DevOps orchestration.

Usually, Java developers write plugins/tasks for their build tool, or use scripts in languages like *Bash*, *Groovy*, or *Python*. These solutions are not ideal, as they may require specific (and sometimes cryptic) syntax, unique debugging methods, or portability issues.

Ideally, Java developers should be able to write Java code in a project directory, and run it directly from the IDE or command line, without concerns about compilation, dependencies, or JDK setup.

JeKa provides a structure, to run Java source code seamlessly from your IDE or the command line—no JDK required—making Java a first-class choice for scripting, from simple tasks to complex needs. Additionaly it provides script facilities for direct method execution.

## 2. Use Java to build Java
Traditional Java build tools are written in *Java* but expose an external DSL API (XML, Kotlin, etc.) to configure them. This approach isolates public from internal API but makes tools more tedious to extend and harder to debug.

I would expect a build tool that I can configure or extend using simple stupid Java. I would like to tacle build/delivery complexity the same 
way I do for regular code. Meaning design, implement, re-factorize, test, share in jar files, run in debugger,... Java code.

By including a comprehensive Java library for building projects and a lightweight layer of reusable components, Jeka can be used as a pure Java build tool,
with straightfoward configuration, extension and troubleshooting.


## 3. High Portability
Traditional build tools often rely on a specific JDK or third-party tools on the host machine, making builds less portable due to implicit requirements or dependencies tied to specific container images.

I believe projects should be able to build on any platform—Windows, Linux, or macOS—without needing to install extra software or set up environment variables. Portability shouldn’t make troubleshooting and debugging harder, so it shouldn’t requires running on containers.

JeKa addresses these concerns by fetching all necessary third-party tools, including the correct JDK and JeKa version, as part of the build process. This ensures that tool dependencies and their versions are explicitly defined, enhancing portability, consistency, and reproducibility across environments, from Windows workstations to cloud CI/CD. 
For instance, you won't need to install GraalVM or Node.js to build native images or JavaScript applications.

## 4. Minimalist Configuration
Traditional build tools often require a minimum configuration, such as project name, version, and plugin declarations, which can feel excessive for getting started.

I expect zero setup (beyond specifying library dependencies) to start coding, testing, and packaging a new Java project. Information like group/name, versioning, and other details can be inferred with default values and specified later if needed.

JeKa is designed with sensible defaults, minimizing the need for configuration. For most common scenarios, you can get started with nearly zero setup and no build code.

## 5. Distribute Applications as Source

Typically, distributing a Java application involves creating JAR files and OS-specific scripts, packaging the files into an archive, publishing them to a binary repository, and providing detailed instructions on how to download and run the application... Not that simple!

I Would like clients to be able to run my Java application with a single command that points to its Git repository, allowing me to simply push or tag the repository to release new versions.

With JeKa, you can run an entire Java application directly from its source code. On the first run, the application is built into a JAR (or native executable) and cached, enabling efficient execution without the need for precompiled binaries.

This approach makes it incredibly convenient to develop Java tools that clients can start using right away, simply by referencing a Git URL.


## 6. Simplify Java for Newcomers

Traditionally, Java developers rely on complex build tools that use XML or Kotlin configurations to build applications. This setup often has a steep learning curve, which can be daunting for newcomers and makes Java seem overly complicated. Additionally, installing or changing the Java version usually involves configuring JDKs and adjusting environment variables.

Ideally, beginners should be able to focus on writing, testing, and running code without being hindered by build tools. Also, switching Java versions should be as simple as a single line of configuration.

JeKa addresses these challenges by minimizing required configuration, offering common functionality out-of-the-box, and creating a streamlined learning experience.


