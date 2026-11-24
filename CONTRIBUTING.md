# Contributing to JeKa

Welcome, and thank you for your interest in contributing to JeKa! Whether fixing bugs, suggesting features, or improving documentation, your input is invaluable.

JeKa thrives on community collaboration. This guide will help you get started and make an impact. Let’s shape the future of Java tooling together!

Use [this discussion channel](https://github.com/orgs/jeka-dev/discussions/categories/contributing) to help us to make contributing easier.

## Repository Organization

This repository is a _monorepo_ containing JeKa's core, plugins, automation samples, and [general documentation](https://jeka-dev.github.io/jeka/).

### Documentation

Documentation is built with [Material for MkDocs](https://squidfunk.github.io/mkdocs-material/). Source files are in the [docs folder](docs).  
To render it locally:
1. Install _Python_, then the required packages: `pip install mkdocs-material pymdown-extensions`
2. Run from the repo root:  
   ```shell
   mkdocs serve
   ```
   The documentation will be served at `http://localhost:8000`.

The published documentation is regenerated on each release.

### Modules

- **core**: Core JeKa code, including bundled KBeans (project, git, docker, etc.).
- **plugins**: Plugins released with JeKa but not bundled (e.g., SpringBoot, Jacoco).
- **samples**: Example projects for illustration and integration testing.

See the [internal design](https://jeka-dev.github.io/jeka/under-the-hood/) of **core**.

## Building JeKa

JeKa builds itself. The bootstrap scripts let you build it from a fresh clone, without JeKa or any other build tool installed.

**Prerequisites:** Git, and JDK 21 or higher with `java` and `javac` in your `PATH`.

### Build from scratch

Run the bootstrap script from the repository root.

Linux / macOS:
```shell
git clone https://github.com/jeka-dev/jeka.git
cd jeka
./bootstrap.sh project: pack --clean --stacktrace
```

Windows (PowerShell):
```powershell
git clone https://github.com/jeka-dev/jeka.git
cd jeka
.\bootstrap.ps1 project: pack --clean --stacktrace
```

> If PowerShell refuses to run the script, use:
> `powershell -ExecutionPolicy Bypass -File .\bootstrap.ps1 project: pack --clean --stacktrace`

The script:
1. Compiles the JeKa sources into `.jeka-work/bootstrap`, using `javac` and the jars vendored in the repository.
2. Launches the freshly compiled JeKa with the arguments you passed.

Any JeKa arguments are accepted. From the root, commands apply to *core* and all plugins; use `-cb=core` to target *core* only:
```shell
./bootstrap.sh -cb=core project: test pack   # compile, test and package core only
```

### Use the local build

The build above generates the JeKa distribution in `core/jeka-output/distrib/bin`. Add this directory to your `PATH`:

```shell
export PATH="$PWD/core/jeka-output/distrib/bin:$PATH"     # Linux / macOS
```
```powershell
$env:PATH = "$PWD\core\jeka-output\distrib\bin;$env:PATH"  # Windows
```

You can now use the `jeka` command instead of the bootstrap script:

```shell
jeka -cb=core project: test      # run core tests
jeka project: test pack          # compile, test and package core and plugins
```

To run a single test class:
```shell
jeka -cb=core project: test "-D@project.test.includePatterns=.*PackageCyclicDependencyTest"
```

> If the local distribution is broken, re-run the bootstrap script to rebuild it from sources.

### Build with IntelliJ

Open the repository in IntelliJ (project files are pre-configured) and use one of the provided run configurations:

- **CORE BUILD**: builds and tests core only.
- **CORE BUILD skip test**: builds core only, without testing.
- **FULL BUILD**: builds and tests core and plugins, generates documentation, and publishes to the local Maven repository.
- **FULL BUILD skip test**: builds core and plugins and publishes to the local Maven repository, without testing.

> IntelliJ debugging: disable the coroutine agent under **Settings > Debugger > Data Views > Kotlin** to avoid IDE issues. [More details](https://stackoverflow.com/questions/68753383/how-to-fix-classnotfoundexception-kotlinx-coroutines-debug-agentpremain-in-debu).

### Continuous integration

Each push and pull request is built by [GitHub Actions](.github/workflows/push-master.yml), which starts from the same `bootstrap.sh` script.

## Coding Guidelines

Adopt the existing style:
- Keep classes `public` only when necessary, prefixed with `Jk` to maintain IDE clarity.
- Prefer fluent APIs.
- Avoid third-party dependencies unless critical.
- Ensure compatibility with JDK 21.

Contributions range from improving JeKa itself to creating plugins for better integration with popular tools.

## SonarQube Analysis

The SonarQube analysis, run by CI, is available [here](https://sonarcloud.io/project/overview?id=dev.jeka.jeka-core).

## Releasing

Create a release via the [GitHub release mechanism](https://github.com/jeka-dev/jeka/releases). This creates the tag and triggers the CI workflow, which builds and publishes the artifacts to Maven Central.

<p align="center">
    <img src="docs/images/mascot.png" width="420" height="420" />
</p>
