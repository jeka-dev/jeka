![Build Status](https://github.com/jerkar/jeka/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/jeka-core)](https://search.maven.org/search?q=g:%22dev.jeka%22%20AND%20a:%22jeka-core%22) 
[![Gitter](https://badges.gitter.im/jeka-tool/community.svg)](https://gitter.im/jeka-tool/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Twitter Follow](https://img.shields.io/twitter/follow/JekaBuildTool.svg?style=social)](https://twitter.com/JekaBuildTool)  

#  Why JeKa

<img src="./docs/images/knight-color-logo.svg" align="left" width="120"/>

Basically, <strong>JeKa</strong> is an automation tool that allows users to execute **Java / Kotlin** source code directly
from the command line.

It comes with a variety of plugins and utilities to make common **devOps** tasks easy to implement, 
such as **building projects** with various technologies, creating **pipelines** and performing **quality checks**.

JeKa aims at bridging **dev** and **ops** by allowing the entire process to be implemented in a single language,
for say : **Java** or **Kotlin**. This includes tasks like *development*, *building*, *creating pipelines*, 
 *deploying containers*, *provisioning platforms*, and *testing*.

Generally, devOps tasks are implemented using scripts or specific template languages, resulting in a proliferation 
of languages and technologies that increase cognitive load and may discourage developers from fully committing to devOps tasks or implementing them poorly.

Bringing **Java or Kotlin** to the **devOps side** can not only lead to better developer engagement but also more careful 
and **robust** implementation due to the **statically typed** nature of these languages. 
This is especially true when working with well-designed, expressive APIs.

Nevertheless, Jeka is very flexible and allows users to pick only the parts they are interested in by integrating 
with any tool providing a command-line interface (such as Maven, Gradle, Skaffold, Helm, Terraform, etc.).

# What Can You Do with JeKa

## Create Pipelines
That is the primary intent of JeKa and it is very simple: you only have to code public methods in Java or Kotlin 
and you can invoke them simply from the command line without worrying about compilation 
(JeKa will take care of it behind the scenes).

You can use any library in your code by declaring its Maven coordinates in an annotation, JeKa will take care of fetching and resolving dependencies for you.

Method execution can be parameterized using instance fields or OS environment variables. The command-line interface allows injecting field values. 
The parameters accept various types such as string, mumbers, file path, booleans, enumerations, and composite objects.

JeKa offers many utilities out-of-the-box for dealing with common devOps tasks such as handling files/file-trees/zips, 
working with Git, interacting with command-line tools, launching Java programs, running tests, managing interactions 
with Maven repositories, and handling XML.

Pipeline methods can be annotated to provide information about their purpose or function when the help command is invoked.

## Build projects
Project building for JVM projects includes tasks such as compiling, unit and integration testing, 
performing quality checks, and packaging artifacts (such as jar, sources, and doc). 
JeKa provides a powerful and flexible build tool out-of-the-box to achieve these tasks.

If you're not a fan of traditional build tools for JVM projects, JeKa's build tool is definitely worth checking out. 
It is quite concise and flexible. It currently supports projects using technologies such as Java, Kotlin-JVM, 
Spring-Boot, Sonarqube, Jacoco, Node.js, and Protocol Buffer.

For other technologies, there are currently no plugins available for zero-effort integration, but it is still possible to use 
their Java API or command-line interface to integrate them into your builds. 
This is more straightforward than with traditional build tools, thanks to JeKa's simple design and rich utilities.

Alternatively, you can use your preferred build tool from a JeKa pipeline through its command-line interface.

## Publish Artifacts
Publishing artifacts is a crucial step for sharing and distributing tools or reusable components such as a Java library, documentation, or a container image.

JeKa offers seamless support for publishing to Maven/Ivy repositories and Nexus, and specifically to OSSRH for deploying artifacts on Maven Central.

For other types of repositories, you can use your preferred Java HTTP client or a client library.

For container images, it is possible to publish via the command-line interface or using Java libraries such as the Docker client for Java.
## Package and Deliver Containers

Packaging and deliver containers generally means to deploy the application in a Kubernetes cluster.
This implies build container images, publish it, creating a Kubernetes manifest and install it on the cluster.

Also, during development time, developers may need to deploy 








# User friendly
Thanks to wrapper and [JeKa Plugin for Intellij](https://github.com/jerkar/jeka-ide-intellij), you don't need to install anything on your machine. 
You only need a JDK 8 or higher.

JeKa is extremly lightweight, the full distribution size is around 1 MB including source code. 
The whole tool is contained in a single jar of aproximatly 600 KB and has zero dependency.

It's quite easy to discover what JeKa does behind the scene and troubleshot issues encountered during the build.


# Get JeKa

The simpler way to get JeKa is by using [IntelliJ plugin](https://github.com/jerkar/jeka-ide-intellij) that will 
handle distribution installation for you. Anyway you can get the binary distributions from the following places :

* Snapshots : https://oss.sonatype.org/content/repositories/snapshots/dev/jeka/jeka-core/
* Milestones and Release Candidates : https://github.com/orgs/jeka-dev/packages?repo_name=jeka
* Releases : https://repo1.maven.org/maven2/dev/jeka/jeka-core/

The distribution is the file named jeka-core-x.x.x-distrib.zip. 

# How to use JeKa ?

Visit following pages according your expectation :

* [Getting Started (Needs Intellij Plugin)](https://jeka-dev.github.io/jeka/tutorials/gui-getting-started/#getting-started-with-jeka)
* [Reference Guide](https://jeka-dev.github.io/jeka/)
* [Working examples](https://github.com/jeka-dev/working-examples)

# External plugins

JeKa comes with plugins out of the box. These plugins covers the most common points a Java developer need to address 
when building a project. This includes plugins for IDE metadata generation (IntelliJ, Eclipse), dependency management, git, , java project building, testing, PGP signing, binary repositories, Maven interaction, scaffolding, sonarQube and web archives.

Nevertheless, JeKa is extensible and other plugins exist outside the main distib among :
* [Springboot Plugin](plugins/dev.jeka.plugins.springboot)
* [Sonarqube Plugin](plugins/dev.jeka.plugins.sonarqube)
* [Jacoco Plugin](plugins/dev.jeka.plugins.jacoco)
* [NodeJs Plugin](plugins/dev.jeka.plugins.nodejs)
* [Protobuf Plugin](https://github.com/jerkar/protobuf-plugin)

# Community

<a class="btn btn-link btn-neutral" href="https://projects.ow2.org/view/jeka">
              <img src="https://jeka.dev/images/ow2.svg" alt="Image" height="60" width="60"></a>
              
This project is supported by OW2 consortium.

You can ask question using regular using [this repository issues](https://github.com/jerkar/jerkar/issues).

You can also use direct emailing for questions and support : djeangdev@yahoo.fr

A twitter account also exist : https://twitter.com/djeang_dev

# News

* Version 0.10.0 is out ! This is major product improvement.

# Versioning 

JeKa follows [semantic versioning 2.0](https://semver.org/spec/v2.0.0.html).

# Roadmap/Ideas

We hope the 0.10.xx series to be the last prior 1.0.0. 
0.10.xx series is a considerable improvement from 0.9.xx. 
We expect our users to give feedbacks to finalise the product.  

* Stabilise api from user feedbacks. API is quite workable now but may be improved from user inputs
* Enhance existing graphical [plugin for Intellij](https://github.com/jerkar/jeka-ide-intellij)
* Improve Kotlin integration
* Provide a plugin for Android

Please visit [release note](https://github.com/jerkar/jeka/blob/master/release-note.md) and [issues](issues) for roadmap.


# How to build JeKa ?

This repository is organized as a _mono repo_. It contains The JeKa core project along plugins and samples for 
automation testing.

* dev.jeka.core : Complete JeKa tool
* plugins : JeKa plugins released along JeKa core (Springboot, NodeJs, Jacoco and Sonarqube)
* samples : Sample projects serving for examples and automation testing
* dev.jeka.master : The master build for building all together.

JeKa builds itself. To build JeKa full distribution from sources, the simpler is to use your IDE.

Once distribution created, add the distrib folder to your PATH environment variable.

## Build JeKa from IntelliJ

* Clone this repository into IntelliJ. Project is already configured (.iml and modules.xml are stored in git).
* Add the `JEKA_CACHE_DIR` variable pointing on [USER_HOME]/.jeka/cache
* Make sure the project is configured with a JDK8 or higher.
* Run 'FULL BUILD' in Intellij _Run Configurations_ to perform a full build of core + plugins + complete test suite.
* Run 'FAST BUILD' in Intellij _Run Configurations_ to perform a fast build of the core without tests.

> For debugging the project, you have to set up Intellij in order to workaround with an Intellij issue :
> Settings/Preferences | Build, Execution, Deployment | Debugger | Data Views | Kotlin | enable "Disable coroutine agent.
> [See here](https://stackoverflow.com/questions/68753383/how-to-fix-classnotfoundexception-kotlinx-coroutines-debug-agentpremain-in-debu)


## Build JeKa from command line

JeKa builds itself, but we need to compile the JeKa sources prior to execute it. 
Fot this, a small _Ant_ script bootstraps the build process by compiling JeKa first then launch 
the JeKa build.

At the repository root dir, execute : `ant -f .github\workflows\build.xml`.

To build the project including Sonarqube and test coverage  : `ant -f .github\workflows\build.xml -Dsonar.host.url=... `.  
Cause of Sonalqube scanner, this command has to be run with a JDK >= 11.


## How to edit documentation

Documentation is generated with [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/). Documentation sources are located (here)[docs].

You must install _Python_ and _Material for MkDocs_ on your computer (`pip install mkdocs-material`) prior to execute following command lines from the repo root directory :
- `mkdocs serve` : generate and serve the documentation on localhost:8000

The documentation is also supposed to be regenerated after each push/pull-request.


## How to Release

Release is done automatically by Github action on PUSH on *master*.
If the last commit message title contains a word like 'Release:XXX' (case matters ) then a tag XXX is created and 
the binaries will be published on Maven Central.
Otherwise, the binary wll be simply pushed on OSSRH snapshot.
<p align="center">
    <img src="docs/images/mascot.png" width='420' height='420' />
</p>

