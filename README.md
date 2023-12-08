![Build Status](https://github.com/jerkar/jeka/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/jeka-core)](https://search.maven.org/search?q=g:%22dev.jeka%22%20AND%20a:%22jeka-core%22)
[![Twitter Follow](https://img.shields.io/twitter/follow/JekaBuildTool.svg?style=social)](https://twitter.com/JekaBuildTool)  


#  What is JeKa ?

<img src="./docs/images/knight-color-logo.svg" width="100" align="left" hspace="15"  />

Essentially, <strong>JeKa</strong> is an automation tool that enables users to execute Java / Kotlin source code 
directly from the command line.

It comes bundled with a library and a component model that cater to the requirements of building Java projects.

These elements can be utilized independently or in combination, tailored to specific needs from basic scripts to sophisticated software build and delivery pipelines.

These objectives can be achieved programmatically or by configuring properties for the reusable components.

Thanks to the component model, plugins are available to seamlessly integrate technologies such as  *Spring-Boot*, *OpenApi*, 
*NodeJs*, *Protobuf*, *SonarQube* and more.

# Why JeKa ?

## Short Answer

JeKa provides an alternative to mainstream Java build tools, for individuals frustrated with technologies they perceive 
as old, heavy, rigid or bloated.

It introduces a breath of fresh air in this domain by offering a light and versatile solution for 
building projects and automate tasks using Java language.

## But also ...

JeKa fills the gap between **dev** and **ops** by allowing the entire process to be implemented in a single language,
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
with any tool providing a command-line interface (such as Maven, Gradle, Kubectl, Helm, Terraform, etc.).

# Examples

- [Build for a Java Library](samples/dev.jeka.samples.basic/jeka/def/dev/jeka/core/samples/demo/JkProjectApiSimple.java)
- [Build for a Spring-Boot application](https://github.com/jeka-dev/working-examples/blob/master/springboot-api/jeka/def/Build.java)
- [Other Examples of projects built with JeKa](https://github.com/jeka-dev/working-examples).

# See in Action

[<img src="https://i9.ytimg.com/vi_webp/rUmhMhYRdr0/mq1.webp?sqp=CNTZzqsG-oaymwEmCMACELQB8quKqQMa8AEB-AH-CYAC0AWKAgwIABABGFogWihaMA8=&rs=AOn4CLC2q5cFAi5TKU3hTIHMiGsnx__6NA" width="50%">](https://www.youtube.com/embed/rUmhMhYRdr0?si=CYPAu21LdY-40xCy "Jeka in Action")


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


# Roadmap/Ideas

We hope the 0.10.xx series to be the last prior 1.0.0.
0.10.xx series is a considerable improvement from 0.9.xx.
We expect our users to give feedbacks to finalise the product.

* Stabilize the API based on user feedback. The API is functional now, but it can be improved with user input.
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


### How to release ?

Just use the [github release mechanism](https://github.com/jeka-dev/jeka/releases).
Creating a release implies creating a tag. This will trigger a build and a publication on Maven Central.

<p align="center">
    <img src="docs/images/mascot.png" width='420' height='420' />
</p>

