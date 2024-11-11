![Build Status](https://github.com/jerkar/jeka/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/jeka-core)](https://search.maven.org/search?q=g:%22dev.jeka%22%20AND%20a:%22jeka-core%22)
[![Twitter Follow](https://img.shields.io/twitter/follow/JekaBuildTool.svg?style=social)](https://twitter.com/JekaBuildTool)  


<img src="./docs/images/logo-plain-gradient.svg" width="100" align="right" hspace="15"  />

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

## Contribute 

Are you a developer, technical writer, or user interested in making an impact?
We invite you to join us in contributing code, documenting knowledge, or providing valuable feedback.
Your participation is crucial in creating something great.
At Jeka, every contribution counts, and every voice matters.
Collaborate with like-minded individuals and help shape the future of our community.

Let’s build, learn, and grow together. Join us and make a difference.

See [contribution page](CONTRIBUTING.md) for starting.


##  What is JeKa ?

*Read the [6 raisons why I created JeKa](docs/articles/listicle-why-i-made-it.md) article.*

**JeKa** is a Java build tool for building or executing Java applications and scripts, 
directly from source code.

Its key features include:

-  **Portable Builds:** JeKa automatically downloads the required JDKs and tools, this includes *JDKs, Graalvm, Maven deps, NodeJs, 
   OpenAPI, JeKa itself and more*. You won't need to setup your machine or a container images prior building. 

-  **Direct App Execution:** JeKa  run applications directly from their Git repository, providing a means to distribute Java applications as source.

-  **Customizable and Extendable:** JeKa can be configured with a concise **property file** for generic cases, or by **Java code** for specific needs.
   Also, JeKa provides a simple plugin mechanism that allows for easy extension.

-  **Kotlin Support:** Scripts or applications can also be implemented using the *Kotlin* language.

## Use-cases

- **Use Java for scripting** 

  JeKa makes it ridiculously easy to write and execute scripts using the Java language.
  Write your automated tasks directly in Java, using third-party dependencies or not, and execute them from anywhere, without any setup.

- **Build projects - Create delivery pipelines** 
  
  Build projects and seamlessly combine scripts to create comprehensive CI/CD pipelines that can run anywhere, 
  from IDE debugger to cloud CI/CD environments. 
  
  JeKa can also complement other build tools such as *Maven* or *Gradle* to fulfill their missing features.


- **Deliver applications as sources** 

  JeKa can execute entire Java applications of any kind and size directly from their Git repository. 
  
  Simply commit or tag repository to publish application.

 - **Centralize build logic**

   Define project build and CI/CD logic in one place, and reuse across all your organization.

- **Make Java fun for newcomers**

  Say goodbye to learning legacy build tools and grappling with JDKs when starting with Java.

  Write Java code and execute it directly from anywhere, with minimal or zero setup.



  

## Examples

### Run Java Applications directly from their Source Repository

- [Execute the 'cow says' cmd-line program directly from its Github repo](https://github.com/jeka-dev/demo-cowsay)
- [Execute a calculator GUI app directly from its Github repo](https://github.com/djeang/Calculator-jeka)

### Build and Test E2E
- [CLI application with a zero-conf build](https://github.com/jeka-dev/demo-base-application)
- [Java library published on Maven Central](https://github.com/djeang/vincer-dom)
- [Spring-Boot application with a build configured using properties only](https://github.com/jeka-dev/demo-springboot-simple)
- [Same but with a build configured with Java code](https://github.com/jeka-dev/demo-springboot-simple/tree/code-config)
- [Springboot-Agular application, including Sonarqube analysis and Docker E2E testing](https://github.com/jeka-dev/demo-project-springboot-angular)

### Reuse Build Logic
- [Build a Spring-Boot + ReactJs application with just 3 lines of configuration.](https://github.com/jeka-dev/demo-build-templates-consumer)


### Combine with existing Build Tools
- [Use JeKa along Maven to build and run Quarkus native app from anywhere](https://github.com/jeka-dev/demo-maven-jeka-quarkus)

### Native and Docker Images
- [Create Docker Images for Spring-Boot Native effortlessly](https://github.com/jeka-dev/demo-project-springboot-headless)
<br/>

[Other Examples of projects built with JeKa](https://github.com/jeka-dev/working-examples).

## Getting Started

Visit following pages according your expectation :

* [Reference Guide](https://jeka-dev.github.io/jeka/)
* [Working examples](https://github.com/jeka-dev/working-examples)

## KBeans

KBeans are JeKa components that expose functionnalities to the command line. They can also be conveniently 
activated and configured via properties, or via other KBeans.

Plugins generally contain one KBean, but they can contain zero or many.

### Bundled KBeans

JeKa is bundled with KBeans covering : IDE integration (IntelliJ Eclipse), Java/Kotlin Project building, Maven publication, Git integration, Docker and more. 

You can get a description of the functionalities provided by the KBeans by executing `jeka --doc` 

Additionnaly JeKa offers high and low level api for dealing with 
dependency management, compilation, testing, Git, GPG signing, Scaffolding, Maven publication, Docker image maker, ...

### External Plugins

External plugins require to be explicitly imported. They are hosted as jar file in Maven Central.

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

## Versioning

JeKa follows [Semantic Versioning 2.0](https://semver.org/spec/v2.0.0.html).

## Community

<a class="btn btn-link btn-neutral" href="https://projects.ow2.org/view/jeka">
              <img src="https://jeka.dev/images/ow2.svg" alt="Image" height="60" width="60"></a>
              
This project is supported by OW2 consortium.

Issues: https://github.com/jeka-dev/jeka/issues

Discussions: https://github.com/orgs/jeka-dev/discussions

Twitter: https://github.com/jeka-dev/jeka

Email support : support@jeka.dev


<img alt="mascot" src="docs/images/mascot.png" align="center" width="50%" height="50%" />








