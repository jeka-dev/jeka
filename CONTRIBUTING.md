# Contributing

## How this repo is organised ?

This repository is organized as a _monorepo_. It contains The JeKa core project along plugins and samples for
automation testing plus [General documentation](https://jeka-dev.github.io/jeka/).

### JeKa Documentation

[General documentation](https://jeka-dev.github.io/jeka/) is generated using [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/) and sources lies in [doc folder](docs).

To render the documentation locally, you need to install _Python_ and _Material for MkDocs_ on your machine
(`pip install mkdocs-material`) prior to execute following command lines from the repo root directory :
```shell
mkdocs serve
```
This generates and serves the documentation on localhost:8000

The documentation is also supposed to be regenerated after each push/pull-request.

### JeKa modules

Jeka modules host the code, tests, build definition and local documentation for JeKa and its plugins.

- dev.jeka.core: contains code, test and build definition for the Jeka product, including all KBeans bundled 
  in JeKa base distribution (project, git, docker, maven,...).
- plugins: contains plugins for JeKa that are released along main distribution but are not included in (Springboot, Jacoco, Sonarqube, nodejs,...)
- samples: Contains some application sample used either for illustration or integration testing.
- master: Contains the code that build/test/publish everything together.

## Start working

The simplest solution to work on JeKa, is to clone this repository within intelliJ.

* Clone this repository into IntelliJ. Project is already configured (.iml and modules.xml are stored in git).
* Run 'CORE BUILD' in Intellij _Run Configurations_ to perform a build on core only.
* Run 'FULL BUILD' in Intellij _Run Configurations_ to perform a full build of core + plugins + complete test suite.

The *JeKa* distribution in generated in *dev.jeka.core/jeka-output/distrib* folder

Add *[JeKa project location]/dev.jeka.core/jeka/output/distrib/bin* to `PATH` environment variable, so 
you'll be able to use the JeKa distribution built locally in your terminal.

> For debugging the project, you may have to set up Intellij in order to workaround with an Intellij issue :
> Settings/Preferences | Build, Execution, Deployment | Debugger | Data Views | Kotlin | enable "Disable coroutine agent.
> [See here](https://stackoverflow.com/questions/68753383/how-to-fix-classnotfoundexception-kotlinx-coroutines-debug-agentpremain-in-debu)

If you don't know where to start, [ask our contributors](https://github.com/djeang).

### Working with samples

Samples may need some external dependencies that are not still present in your bin repo. 
If it is the case, just execute at the root folder of the specific sample module :
```shell
jeka intellij: iml
```
This will regenerate the iml file while downloading the missing dependencies.

## Coding Rules

If you want to help Jeka, follow one rule: copy our current style. 

Here's what you need to know:
* Only make a class public if you need to. If a class is public, use `Jk` at the start of the name. This keeps the auto-complete in IDEs clear when using Jeka.
* Use a fluent-style API.
* Don't use other third-party things (You can use or add to JkUtilsXxxxx classes for shared things).
* Jeka must work with JDK8.

Anybody can help Jeka. It's a new project with lots of ways to help. 
You can add to or debug the Jeka project. You can also create plugins to make Jeka work better with your favorite tools. It's not hard and even a little code can help a lot.

## Build JeKa from Command Line

JeKa builds itself, but we need to compile the JeKa sources prior to execute it.
For this, a small _Ant_ script bootstraps the build process by compiling JeKa first then launch
the JeKa build.

At the repository root dir, execute : `ant -f .github\workflows\build.xml`.

To build the project including Sonarqube and test coverage  : `ant -f .github\workflows\build.xml -Dsonar.host.url=... `.  
Cause of Sonarqube scanner, this command has to be run with a JDK >= 11.

## How to Release ?

Just use the [github release mechanism](https://github.com/jeka-dev/jeka/releases).
Creating a release implies creating a tag. This will trigger a build and a publication on Maven Central.

<p align="center">
    <img src="docs/images/mascot.png" width='420' height='420' />
</p>


