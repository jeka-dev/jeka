# Contributing to JeKa

Welcome, and thank you for your interest in contributing to JeKa! Whether fixing bugs, suggesting features, or improving documentation, your input is invaluable.

JeKa thrives on community collaboration. This guide will help you get started and make an impact. Let’s shape the future of Java tooling together!

Use [this discussion channel](https://github.com/orgs/jeka-dev/discussions/categories/contributing) to help us to make contributing easier.

## Repository Organization

This repository is a _monorepo_ containing JeKa's core, plugins, automation samples, and [general documentation](https://jeka-dev.github.io/jeka/).

### Documentation

Documentation is built with [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/). Source files are in the [docs folder](docs).  
To render it locally:
1. Install _Python_ and _Material for MkDocs_: `pip install mkdocs-material`
2. Run from the repo root:  
   ```shell
   mkdocs serve
   ```
   The documentation will be served at `http://localhost:8000`.

Documentation is regenerated after each push or pull request.

### Modules

- **core**: Core JeKa code, including bundled KBeans (project, git, docker, etc.).
- **plugins**: Plugins released with JeKa but not bundled (e.g., SpringBoot, Jacoco).
- **samples**: Example projects for illustration and integration testing.

See [internal design](https://jeka-dev.github.io/jeka/under-the-hood/) of  **core**.

## How to build

1. Clone this repository and open it in IntelliJ (project files are pre-configured).
2. Use the pre-defined IntelliJ run configurations:
  - Run **CORE BUILD** for the core only.
  - Run **FULL BUILD** for the core, plugins, and test suite.

The JeKa distribution is generated in `core/jeka-output/distrib`. 

Add this path to your `PATH` variable to use the local build.

> IntelliJ Debugging: Disable the coroutine agent under **Settings > Debugger > Data Views > Kotlin** to avoid IDE issues. [More details](https://stackoverflow.com/questions/68753383/how-to-fix-classnotfoundexception-kotlinx-coroutines-debug-agentpremain-in-debu).

## Coding Guidelines

Adopt the existing style:
- Keep classes `public` only when necessary, prefixed with `Jk` to maintain IDE clarity.
- Prefer fluent APIs.
- Avoid third-party dependencies unless critical.
- Ensure compatibility with JDK 21.

Contributions range from improving JeKa itself to creating plugins for better integration with popular tools.

## Building JeKa from Command Line

JeKa builds itself. An _Ant_ script is first used to compile JeKa and start the process.
```shell
ant -f .github\workflows\build.xml
```

For full builds with SonarQube analysis:  
```shell
ant -f .github\workflows\build.xml -Dsonar.host.url=...
```

## Releasing

Create a release via the [GitHub release mechanism](https://github.com/jeka-dev/jeka/releases). This automatically tags, builds, and publishes to Maven Central.

<p align="center">
    <img src="docs/images/mascot.png" width="420" height="420" />
</p>

## Sonarqube Analysis

Sonarqube analysis is available [here](https://sonarcloud.io/project/overview?id=dev.jeka.jeka-core)
