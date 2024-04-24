![Build Status](https://github.com/jerkar/jeka/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/jeka-core)](https://search.maven.org/search?q=g:%22dev.jeka%22%20AND%20a:%22jeka-core%22)
[![Twitter Follow](https://img.shields.io/twitter/follow/JekaBuildTool.svg?style=social)](https://twitter.com/JekaBuildTool)  

> [!IMPORTANT]
> JeKa 0.11.0 has been released!!!
> 
> This release brings major and drastic changes compared to previous JeKa versions.
> 
> The content of this page reflects the new version and does not apply to 0.10.x. 
> 
> The existing IDE plugin for IntelliJ still works with Jeka 0.10.x but is no longer compatible with the new version. 
> A new plugin for Jeka 0.11.x is under development.
> 
> The official documentation provides information for both versions and a migration guide.

<img src="./docs/images/knight-color-logo.svg" width="100" align="right" hspace="15"  />

##  What is JeKa ?

<strong>Jeka</strong> is a Java build tool that allows users to build, script or run directly Java/Kotlin applications.

Its key features include:

-  **JDKs Management:** Automatically downloads and selects the appropriate JDKs. No need to have Java already installed on the host machine.


-  **Direct App Execution:** Run applications directly from their Git repository, providing a means to distribute Java applications as source.


-  **Portable Builds:** JeKa downloads everything needed for a build (JDKs, NodeJs, OpenAPI tooling, etc. including JeKa itself!)


-  **Easily Extendable:** JeKa comes with a simple plugin mechanism for easy extension.


-  **Customizable:** JeKa can be configured with concise **property files** for generic cases, or by **Java code** for specific needs.


## Use-cases

- **Use Java for scripting** 

  JeKa makes it ridiculously easy to write and execute scripts using the Java language.
  Write your automated tasks directly in Java and execute them from anywhere, without any setup.


- **Build projects - Create pipelines** 
  
  Build projects and seamlessly combine scripts to create comprehensive CI/CD pipelines that can run anywhere, 
  from Windows desktop to cloud CI/CD environments.
  
  JeKa can also complement other build tools such as *Maven* or *Gradle* to fulfill their missing features.


- **Deliver applications as sources** 

  JeKa can execute entire Java applications of any kind and size directly from their Git repository. 
  
  Simply commit or tag your repository to release your application.


- **Make Java fun for newcomers**

  Say goodbye to learning legacy build tools and grappling with JDKs when starting with Java.

  Write directly Java code that can be build and executed from anywhere with minimal or zero setup.


- **Centralize build logic**: 

  Define in one place the build and delivery logic that projects with similar technologies can share and extend.
  

## Examples

- [Execute the 'cow says' cmd-line program directly from Github](https://github.com/jeka-dev/demo-cowsay)
- [Execute a calculator GUI app directly from Github](https://github.com/djeang/Calculator-jeka)


- [Build a Springboot-Agular application, including Sonarqube analysis and Docker E2E testing](https://github.com/jeka-dev/demo-project-springboot-angular)
- [JeKa used along Maven to build and run Quarkus native app](https://github.com/jeka-dev/demo-maven-jeka-quarkus)


- [Build a Java Library](samples/dev.jeka.samples.basic/jeka/def/dev/jeka/core/samples/demo/JkProjectApiSimple.java)
- [Other Examples of projects built with JeKa](https://github.com/jeka-dev/working-examples).

## Getting Started

Visit following pages according your expectation :

* [Reference Guide](https://jeka-dev.github.io/jeka/)
* [Working examples](https://github.com/jeka-dev/working-examples)

## Plugins

### Bundled Plugins

JeKa comes with plugins out-of-the-box. These plugins cover most of the common needs for building a project. 
This includes IDE integration (IntelliJ Eclipse), 
dependency management, compilation, testing, Git, GPG signing, Scaffolding, Maven publication, Docker image maker, ...

You can have a description of these plugins by executing command `jeka --help`.


### External Plugins

External plugins require to be explicitly imported.

The following plugins are hosted in same monorepo then JeKa, and so are released in conjunction 
which means that we don't need to specify their version when importing.

* [Springboot Plugin](plugins/dev.jeka.plugins.springboot)
* [Sonarqube Plugin](plugins/dev.jeka.plugins.sonarqube)
* [Jacoco Plugin](plugins/dev.jeka.plugins.jacoco)
* [NodeJs Plugin](plugins/dev.jeka.plugins.nodejs)
* [Protobuf Plugin](plugins/dev.jeka.plugins.protobuf)
* [Nexus Plugin](plugins/dev.jeka.plugins.nexus)

The following plugins are maintained in their own repos.

* [OpenApi Plugin](https://github.com/jeka-dev/openapi-plugin)


# Community

<a class="btn btn-link btn-neutral" href="https://projects.ow2.org/view/jeka">
              <img src="https://jeka.dev/images/ow2.svg" alt="Image" height="60" width="60"></a>
              
This project is supported by OW2 consortium.

You can ask question using regular using [this repository issues](https://github.com/jerkar/jerkar/issues).

You can also use direct emailing for questions and support : djeangdev@yahoo.fr

A twitter account also exist : https://twitter.com/djeang_dev

## Versioning 

JeKa follows [Semantic Versioning 2.0](https://semver.org/spec/v2.0.0.html).

## Developer Notes

### How is Organized this Repo ?

This repository is organized as a _monorepo_. It contains The JeKa core project along plugins and samples for 
automation testing.

* dev.jeka.core : Complete JeKa tool
* plugins : JeKa plugins released along JeKa core (Springboot, NodeJs, Jacoco and Sonarqube)
* samples : Sample projects serving for examples and automation testing
* dev.jeka.master : The master build for building all together.

JeKa builds itself. To build JeKa full distribution from sources, the simpler is to use your IDE.

### Build JeKa from IntelliJ

* Clone this repository into IntelliJ. Project is already configured (.iml and modules.xml are stored in git).
* If you have not installed JeKa plugin, add the `JEKA_CACHE_DIR` variable pointing on [USER_HOME]/.jeka/cache
* Make sure the project is configured with a JDK8 or higher.
* Run 'FULL BUILD' in Intellij _Run Configurations_ to perform a full build of core + plugins + complete test suite.
* Run 'FAST BUILD' in Intellij _Run Configurations_ to perform a fast build of the core without tests.
* Set environment variable `JEKA_HOME` = *[JeKa project location]/dev.jeka.core/jeka/output/distrib* and add it to `PATH` environment.

> For debugging the project, you may have to set up Intellij in order to workaround with an Intellij issue :
> Settings/Preferences | Build, Execution, Deployment | Debugger | Data Views | Kotlin | enable "Disable coroutine agent.
> [See here](https://stackoverflow.com/questions/68753383/how-to-fix-classnotfoundexception-kotlinx-coroutines-debug-agentpremain-in-debu)


### Build JeKa from Command Line

JeKa builds itself, but we need to compile the JeKa sources prior to execute it. 
For this, a small _Ant_ script bootstraps the build process by compiling JeKa first then launch 
the JeKa build.

At the repository root dir, execute : `ant -f .github\workflows\build.xml`.

To build the project including Sonarqube and test coverage  : `ant -f .github\workflows\build.xml -Dsonar.host.url=... `.  
Cause of Sonarqube scanner, this command has to be run with a JDK >= 11.


## How to Edit Documentation ?

Documentation is generated with [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/). Documentation sources are located (here)[docs].

You must install _Python_ and _Material for MkDocs_ on your computer 
(`pip install mkdocs-material`) prior to execute following command lines from the repo root directory :
```shell
mkdocs serve
```
This generates and serves the documentation on localhost:8000

The documentation is also supposed to be regenerated after each push/pull-request.


## How to Release ?

Just use the [github release mechanism](https://github.com/jeka-dev/jeka/releases).
Creating a release implies creating a tag. This will trigger a build and a publication on Maven Central.

<p align="center">
    <img src="docs/images/mascot.png" width='420' height='420' />
</p>


