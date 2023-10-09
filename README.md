![Build Status](https://github.com/jerkar/jeka/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/jeka-core)](https://search.maven.org/search?q=g:%22dev.jeka%22%20AND%20a:%22jeka-core%22) 
[![Gitter](https://badges.gitter.im/jeka-tool/community.svg)](https://gitter.im/jeka-tool/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Twitter Follow](https://img.shields.io/twitter/follow/JekaBuildTool.svg?style=social)](https://twitter.com/JekaBuildTool)  


#  Why JeKa ?

<img src="./docs/images/knight-color-logo.svg" width="100" align="left" hspace="15"  />

Basically, <strong>JeKa</strong> is an automation tool that allows users to execute **Java / Kotlin** source code directly
from the command line.

It comes with a variety of plugins and utilities to make common **devOps** tasks easy to implement, 
such as **building projects** with various technologies, creating **pipelines** and performing **quality checks**.

JeKa aims at bridging **dev** and **ops** by allowing the entire process to be implemented in a single language,
for say : **Java** or **Kotlin**. This includes tasks like *development*, *building*, *creating pipelines*, *testing*, 
*deploying containers* and *provisioning platforms*.

Generally, devOps tasks are implemented using scripts or configuration files with specific template languages around, 
resulting in a proliferation of languages and technologies that increase cognitive load 
and may discourage developers from fully committing to devOps tasks or implementing them poorly.

Bringing **Java or Kotlin** to the **devOps side** can not only lead to better developer engagement but also more careful 
and **robust** implementation due to the **statically typed** nature of these languages. 
This is especially true when working with well-designed, expressive APIs. 
One other good thing is that Jeka works only with familiar Java structure making tasks implementation manageable
by ops with basic Java knowledge.

Nevertheless, JeKa is very flexible and allows users to pick only the parts they are interested in by integrating 
with any tool providing a command-line interface (such as Maven, Gradle, Skaffold, Helm, Terraform, etc.).

# What Can You Do with JeKa ?

## Implement Pipelines

Here, by "implementing pipelines" we mean implementing the specific steps that the CI/CD pipeline will execute 
to move the application from source code to delivery. 
These steps typically include building, testing, publishing, and deploying the application.

This is quite straightforward with JeKa: you only have to code public methods in Java or Kotlin and you can invoke them simply from the command line without worrying about compilation (JeKa will take care of it behind the scenes).

You can also use any library in your code by declaring its Maven coordinates in an annotation, 
JeKa will fetch and resolve dependencies for you.

Method execution can be parameterized using property files, OS environment variables, or command line arguments. 
The parameters accept various types such as string, numbers, file paths, booleans, enumerations, and composite objects.

JeKa offers many utilities out-of-the-box for dealing with common devOps tasks such as handling files/file-trees/zips, 
working with Git, interacting with command-line tools, launching Java programs, running tests, and managing interactions with Maven repositories.

Pipeline code, like any Java code, can be shared on a Maven repository. 
When shared, pipelines can be invoked simply by mentioning their Maven coordinates without the need for it to be 
present on the local drive.

By adding annotations, pipeline code can provide context and explanations for its methods and fields, 
which will be visible when calling the help command.

See [Pipeline step to create a Github release](dev.jeka.master/jeka/def/github/Github.java)


## Build Projects
JVM projects building includes tasks such as compiling, unit and integration testing, performing quality 
checks, packaging artifacts (such as jar, sources, and container images), and publishing them. JeKa provides a powerful 
and flexible build tool out-of-the-box to achieve these tasks.

If you're not a fan of traditional build tools for JVM projects, JeKa's build tool is definitely worth checking out. 
It is quite concise and flexible. It currently supports out-of-the-box projects using technologies such as Java, Kotlin-JVM, 
Spring-Boot, Sonarqube, Jacoco, Node.js, and Protocol Buffer.

For other technologies, it is possible to directly use their Java API or command-line interface to integrate them into 
your builds. This is quite straightforward thanks to JeKa's simple design and rich utilities. 

See [examples of projects built with JeKa](https://github.com/jeka-dev/working-examples).

## Define Infrastructure as Code

For containerized applications, *packaging/deploying* generally means to deploy the application in a Kubernetes cluster. 
Kubernetes provides a Java client library that allows to define an application deployment 
using *Infrastructure as Code* (IaC), and install/uninstall it.

You can use this library directly in JeKa to define Kubernetes manifests using plain Java objects and manage the 
installation and uninstallation of the application, similar to how *Helm* operates.

Some cloud platforms such as Azure or AWS propose solutions to implement Infrastructure as Code using Java. 
They provide tutorials on how to use these solutions with Maven, but it's possible to use Jeka instead if you prefer to avoid using Maven.

Alternatively, you can use a tool such as *Pulumi* and integrate it into a Jeka pipeline using its command line interface.

See [Project deployed on Kubernetes](https://github.com/jeka-dev/working-examples/tree/master/kubernetes)

# What makes JeKa User Friendly ?

Thanks to its wrapper and the [Plugin for IntelliJ](https://github.com/jerkar/jeka-ide-intellij), you don't need to install anything on your machine to run Jeka. You only need a JDK 8 or higher.

JeKa is extremely lightweight, the full distribution size is around 1 MB including source code. The whole tool is contained in a single jar of approximately 600 KB and has zero dependencies.

Jeka does its best to not obfuscate what is happening behind the scene, making it easy to discover and troubleshoot issues encountered during execution.


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

# External Plugins

JeKa comes with plugins out-of-the-box. These plugins cover most of the common needs a Java developer has when building 
a project. This includes plugins for IDE metadata generation (IntelliJ, Eclipse), 
dependency management, Git, Java project building, testing, GPG signing, binary repositories, 
Maven interaction, scaffolding, and JEE Web ARchives.

Nevertheless, JeKa is extensible and other plugins exist outside the main distribution. Some of them are located in the
same monorepo, which means that you don't need to specify their version as JeKa will automatically pick the right version for you.

### Plugins Hosted in JeKa Monorepo


* [Springboot Plugin](plugins/dev.jeka.plugins.springboot)
* [Sonarqube Plugin](plugins/dev.jeka.plugins.sonarqube)
* [Jacoco Plugin](plugins/dev.jeka.plugins.jacoco)
* [NodeJs Plugin](plugins/dev.jeka.plugins.nodejs)
* [Protobuf Plugin](plugins/dev.jeka.plugins.protobuf)

### Known Plugins Hosted outside this repo

* [OpenApi Plugin](https://github.com/jeka-dev/openapi-plugin)

# News

* Out-of-the-box support for Kotlin-JVM. See [sample using springboot-kotlin-nodejs toghether](https://github.com/jeka-dev/working-examples/tree/master/springboot-kotlin-reactjs)
* Version 0.10.0 is out ! This is major product improvement.

# Roadmap/Ideas

We hope the 0.10.xx series to be the last prior 1.0.0.
0.10.xx series is a considerable improvement from 0.9.xx.
We expect our users to give feedbacks to finalise the product.

* Provides a concrete example of a complete pipeline delivering to a Kubernetes cluster.
* Stabilize the API based on user feedback. The API is functional now, but it can be improved with user input.
* Improve [plugin for Intellij](https://github.com/jerkar/jeka-ide-intellij)
* Improve Kotlin integration
* Provide a plugin for Android

Please visit [release note](https://github.com/jerkar/jeka/blob/master/release-note.md) and [issues](issues) for roadmap.

# Community

<a class="btn btn-link btn-neutral" href="https://projects.ow2.org/view/jeka">
              <img src="https://jeka.dev/images/ow2.svg" alt="Image" height="60" width="60"></a>
              
This project is supported by OW2 consortium.

You can ask question using regular using [this repository issues](https://github.com/jerkar/jerkar/issues).

You can also use direct emailing for questions and support : djeangdev@yahoo.fr

A twitter account also exist : https://twitter.com/djeang_dev

# Versioning 

JeKa follows [Semantic Versioning 2.0](https://semver.org/spec/v2.0.0.html).

# Developer Notes

## How is organized this repo ?

This repository is organized as a _monorepo_. It contains The JeKa core project along plugins and samples for 
automation testing.

* dev.jeka.core : Complete JeKa tool
* plugins : JeKa plugins released along JeKa core (Springboot, NodeJs, Jacoco and Sonarqube)
* samples : Sample projects serving for examples and automation testing
* dev.jeka.master : The master build for building all together.

JeKa builds itself. To build JeKa full distribution from sources, the simpler is to use your IDE.

## Build JeKa from IntelliJ

* Clone this repository into IntelliJ. Project is already configured (.iml and modules.xml are stored in git).
* If you have not installed JeKa plugin, add the `JEKA_CACHE_DIR` variable pointing on [USER_HOME]/.jeka/cache
* Make sure the project is configured with a JDK8 or higher.
* Run 'FULL BUILD' in Intellij _Run Configurations_ to perform a full build of core + plugins + complete test suite.
* Run 'FAST BUILD' in Intellij _Run Configurations_ to perform a fast build of the core without tests.

> For debugging the project, you may have to set up Intellij in order to workaround with an Intellij issue :
> Settings/Preferences | Build, Execution, Deployment | Debugger | Data Views | Kotlin | enable "Disable coroutine agent.
> [See here](https://stackoverflow.com/questions/68753383/how-to-fix-classnotfoundexception-kotlinx-coroutines-debug-agentpremain-in-debu)


## Build JeKa from command line

JeKa builds itself, but we need to compile the JeKa sources prior to execute it. 
Fot this, a small _Ant_ script bootstraps the build process by compiling JeKa first then launch 
the JeKa build.

At the repository root dir, execute : `ant -f .github\workflows\build.xml`.

To build the project including Sonarqube and test coverage  : `ant -f .github\workflows\build.xml -Dsonar.host.url=... `.  
Cause of Sonarqube scanner, this command has to be run with a JDK >= 11.


## How to edit documentation ?

Documentation is generated with [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/). Documentation sources are located (here)[docs].

You must install _Python_ and _Material for MkDocs_ on your computer (`pip install mkdocs-material`) prior to execute following command lines from the repo root directory :
- `mkdocs serve` : generate and serve the documentation on localhost:8000

The documentation is also supposed to be regenerated after each push/pull-request.


## How to Release ?

Release is done automatically by Github action on PUSH on *master*.
If the last commit message title contains a word like 'Release:XXX' (case matters ) then a tag XXX is created and 
the binaries will be published on Maven Central.
Otherwise, the binary wll be simply pushed on OSSRH snapshot.
<p align="center">
    <img src="docs/images/mascot.png" width='420' height='420' />
</p>

