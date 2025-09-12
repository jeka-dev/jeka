![Build Status](https://github.com/jerkar/jeka/actions/workflows/push-master.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/dev.jeka/jeka-core)](https://search.maven.org/search?q=g:%22dev.jeka%22%20AND%20a:%22jeka-core%22)
[![Twitter Follow](https://img.shields.io/twitter/follow/JekaBuildTool.svg?style=social)](https://twitter.com/JekaBuildTool)  


<img src="./docs/images/logo-plain-gradient.svg" width="100" align="right" hspace="15"  />

_____
**⭐ Support the Project ⭐**

If you find this project useful, please consider giving it a ⭐ on GitHub! Your support helps the project grow and reach more developers. 😊
_____

# JeKa

**The Next-Gen Build Tool for Java & Co**

## Description

JeKa is a modern Java build tool designed for simplicity, combining ease of use with robust handling of complex scenarios.

It targets a generation of Java developers who prefer simple, Java-centric tools over complex XML-based 
or external DSL-based solutions for building their applications.

## Use Cases
- **Replace Maven or Gradle:** Use simple and powerful properties/Java instead of XML or complex DSL. No pre-installed JDK required.
- **Use Java for scripting:** Write scripts, devOps pipelines or complex applications in Java, runnable directly from source code.
- **Package-less Java:** Run applications directly from Git-hosted source code in JVM or native mode without traditional packaging or deployment.
- **Handle complex build scenarios gracefully:** Encapsulate build logic using intuitive Java mechanisms.
- **Make Java more attractive:** Learn Java effortlessly, without complex XML or intimidating heavy tools.

## Features
- **Zero-Config Builds:** Build Java projects with zero setup — no configuration or JDK installation required. 
- **Java-Based Configuration:** Customize builds with simple properties or fine-tune with plain Java code — no XML, No DSL.
- **Fully Portable builds:** Automatically downloads required JDK versions and tools — no pre-installed JDK or Jeka needed.
- **Cloud-Native Ready:** Effortless native compilation and Docker image creation — no setup or configuration needed.
- **Run Java/Kotlin Scripts:** Execute simple scripts or full applications directly from source code — no compilation and dep management needed.
- **Instant App Deployment:** Push application code to Git, and it's ready to run — no pipeline required.
- **Simple Extensions:** Integrate third-party tools or handle complex scenarios with minimal effort.
- **Super Lightweight:** Comes as a zero-dependency JAR of less than 2MB.
- **Multi-Module Project Support:** Enables configuration of multi-module projects with minimal configuration required.
- **Supported Technologies:** Java, Kotlin, Git, Docker, GraalVM, Spring-Boot, Node.js, OpenAPI, Jacoco, SonarQube, Protobuf, Maven, and more.


## Installation
Visit the [installation page](https://jeka-dev.github.io/jeka/installation/).

## Usage (Examples)

**Execute build methods**
- **Compile, test, and create JAR**: `jeka project: test pack`
- **Compile to native executable**: `jeka native: compile`
- **Create a JVM-based Docker image**: `jeka docker: build`
- **Create a native-based Docker image**: `jeka docker: buildNative`

**Execute Java applications**
- **Run a Java application directly from its Git repository**: `jeka -r <git url> -p [program args...]`
- **Example:** `jeka -r https://github.com/jeka-dev/demo-cowsay#0.0.6 -p Hello JeKa`

**Help**
- Display help on console: `jeka --help`
- Display docs on KBeans: `jeka --doc`

**Configure builds using properties**
```properties
jeka.classpath=dev.jeka:jacoco-plugin  dev.jeka:sonarqube-plugin

@project=on
@project.moduleId=my-org:my-lib
@project.gitVersioning.enable=true
@project.pack.jarType=SHADE

@jacoco=on
@sonarqube=on
@sonarqube.gate=true
```

**Create specific tasks with Java code**

```java
import dev.jeka.core.tool.JkInject;
import dev.jeka.core.tool.builtins.project.ProjectKBean;

@JkDep("commons-net:commons-net:3.11.1")
class Build extends KBean {

    @JkDoc("Deploy Spring-Boot application on remote server")
    public void deployFtp() {
        Path jar = load(ProjectKBean.class).project.artifactLocator.getMainArtifactPath();
        this.sendThroughFtp(jar);
        this.sendRestartCommand();
    }
  ...
```
Run directly this method from command line: `jeka build: deployFtp`.

Visit the [documentation](https://jeka-dev.github.io/jeka/), and explore the [examples](https://jeka-dev.github.io/jeka/examples/).


## External Plugins

External plugins must be explicitly imported and are hosted as JAR files on Maven Central.

The following plugins are part of JeKa’s monorepo and are released together, so their version does not need to be specified when importing:
- [Spring Boot Plugin](plugins/plugins.springboot)
- [SonarQube Plugin](plugins/plugins.sonarqube)
- [JaCoCo Plugin](plugins/plugins.jacoco)
- [Node.js Plugin](plugins/plugins.nodejs)
- [Kotlin Plugin](plugins/plugins.kotlin)
- [Protobuf Plugin](plugins/plugins.protobuf)
- [Nexus Plugin](plugins/plugins.nexus)
- [Central Portal Plugin](plugins/plugins.centralportal)

The following plugin is maintained in a separate repository:
- [OpenAPI Plugin](https://github.com/jeka-dev/openapi-plugin)

## Community

- **Contribute:** [Contribution Guide](https://github.com/jeka-dev/jeka/blob/master/CONTRIBUTING.md)
- **Issues:** [Report or track issues](https://github.com/jeka-dev/jeka/issues)
- **Discussions:** [Join discussions](https://github.com/orgs/jeka-dev/discussions)
- **Twitter:** [Follow us](https://github.com/jeka-dev/jeka)
- **Email Support:** <a href="mailto:support@jeka.dev">support@jeka.dev</a>

This project is supported by the OW2 Consortium.

<a class="btn btn-link btn-neutral" href="https://projects.ow2.org/view/jeka">
  <img src="docs/images/ow2.svg" alt="OW2 Logo" width="80" />
</a>

## Versioning

JeKa follows [Semantic Versioning 2.0](https://semver.org/spec/v2.0.0.html).

## Roadmap

- Improve documentation
- Enhance existing functionality based on user feedback.
- Develop a dedicated plugin for Kubernetes.
- Provide examples showcasing JeKa's use for provisioning cloud platforms via their SDKs.
 

