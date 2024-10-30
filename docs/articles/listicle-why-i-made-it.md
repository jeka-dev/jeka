# 5 Reasons I Created JeKa

JeKa, is a build tool for Java, with unique features.

### 1. Run Java Code Directly
When automating tasks for Java projects, you’ve likely faced the challenge of executing code easily. 
Traditional options are either to write complex build-tool plugins, rely on extra languages like Python or XML, 
or struggle with Java compilation and JRE configurations.

*So why not make it simple?*

With JeKa, you can execute Java code directly—whether it’s a single file or a larger multi-file project, 
with dependencies or without. JeKa allows Java source code to be run straight from your IDE or the command line 
without needing a pre-installed JDK/JRE. 

This removes obstacles of using Java as a script-language.

### 2. Create Builds and Pipelines with Java
Automating builds and deployments can quickly become complicated, 
especially when switching between multiple languages or configurations to set up a reliable CI/CD pipeline. 
With JeKa, I wanted to harness the full power of Java for addressing build/delivery complexity.

By including a comprehensive build library and reusable components, JeKa makes it easy to model projects 
and create full CI/CD pipelines directly in Java. 
This approach doesn’t just streamline the build process—it eliminates the need for additional scripting languages or external tools.

### 3. Portability: Run Everywhere

Traditional build tools often rely on the presence of a specific JDK 
or other third-party tools on the host machine. 
This dependency can make builds less portable, as some requirements are either implicit 
or only accessible within specific container images.

JeKa approaches this differently by fetching all necessary third-party tools, 
including the correct JDK version, as part of the build itself. 
This way, tool dependencies and their versions become a defined part of the build process, 
making builds more portable, consistent, and reproducible across different environments.

### 4. Distribute Applications as Source

Distributing a Java application traditionally involves creating JAR files, 
packaging them, publishing to a binary repository like Maven Central,
and then providing detailed instructions on the required JDK version.

*But why not simplify this process by just publishing the source code directly on a public Git repository?*

With JeKa, you can run an entire Java application directly from its source code. 
On the first run, the application is packaged into a JAR and cached, 
allowing it to be executed efficiently without the need for precompiled binaries.

This makes it incredibly convenient to develop Java tools that clients can start using right away, simply by mentioning a Git url.

### 5. Build Projects with Minimal Configuration or Coding

When starting a Java project, we should not be forced to configure anything except library dependencies 
needed for coding. Information as group/name, versioning and other can be specified afterward, if needed.

JeKa is designed with sensible defaults, reducing the need for configuration to minimal. 

For most common scenarios, you can get started with nearly zero setup, 
allowing you to focus on the essentials rather than wrestling with extensive configurations.

### 5. Simplify Java for Newcomers

At its core, JeKa is about making Java more approachable.
By reducing the need for additional tools and enabling direct code execution, 
JeKa transforms Java into a more flexible and accessible language. 
This simplicity benefits both seasoned developers and newcomers, making it easier for everyone to leverage Java’s capabilities.
