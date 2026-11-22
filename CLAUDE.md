# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Project Is

JeKa is a Java build tool that **builds itself**. The repo is a monorepo containing the core engine, bundled plugins, and sample/integration test projects. JeKa uses JeKa for its own build — there is no Gradle or Maven wrapper.

## Build Commands

JeKa must be bootstrapped from scratch via ANT (since it needs itself to build):

```shell
# Bootstrap and build core (from repo root)
ant -f bootstrap/build.xml
# Or with the bash script (pass any jeka arguments directly):
bash bootstrap.sh project: pack --clean --inspect --stacktrace

# After that, the local JeKa distrib is at core/jeka-output/distrib/bin
# Add it to PATH to use the local build:
export PATH="$PWD/core/jeka-output/distrib/bin:$PATH"
```

**IntelliJ run configurations** (pre-configured, recommended for development):
- **CORE BUILD** — builds core only: `project: test pack e2eTest --clean --stacktrace -v` (working dir: `core/`)
- **FULL BUILD** — builds core + all plugins + tests + docs: runs from repo root

**JeKa CLI commands once bootstrapped:**
```shell
# From the core/ directory
jeka project: test pack       # compile, test, and package
jeka project: pack            # compile and package only
jeka base: test               # run unit tests via BaseKBean
jeka project: test e2eTest    # run all tests including e2e

# From repo root (multi-module)
jeka project: test pack e2eTest base: test build: enrichMkDocs maven: publishLocal --clean
```

**Run a single test class:**
```shell
jeka project: test -Djeka.test.filter=MyTestClassName
```

**Documentation (MkDocs):**
```shell
pip install mkdocs-material
mkdocs serve   # serves at http://localhost:8000
```

## Repository Structure

```
core/           # JeKa engine and all bundled KBeans — the main artifact (dev.jeka:jeka-core)
  src/main/java/dev/jeka/core/
    api/        # Reusable public API (Jk-prefixed classes)
    tool/       # Engine: KBean framework, CLI parsing, classpath loading
  jeka-src/     # KBeans used to build core itself (dev subdirectory)
  jeka-boot/    # Pre-compiled bootstrap JARs
  libs/         # Vendored JARs: Ivy, Bouncycastle, Classgraph, Gson, Jansi, JUnit 5, etc.
plugins/        # Plugins released with JeKa but not bundled in core
  plugins.springboot/
  plugins.jacoco/
  plugins.sonarqube/
  plugins.kotlin/
  plugins.nodejs/
  plugins.protobuf/
  plugins.nexus/
  plugins.centralportal/
  plugins.gradle/
samples/        # Example projects used for illustration and integration testing
jeka-src/       # Root-level build KBeans: Build.java, Triage.java, MkDocsEnricher.java
jeka.properties # Root multi-module config; child-bases: core, plugins/*
docs/           # MkDocs source for the documentation site
```

## Architecture: Core Concepts

### KBean Framework (`core/src/main/java/dev/jeka/core/tool/`)

**KBean** is the fundamental unit. Any Java class extending `KBean` becomes a named command group. The class name minus the `KBean` suffix is the KBean's name (e.g., `ProjectKBean` → `project`).

- KBeans expose public `void` methods as CLI commands and public fields as configurable properties.
- Use `@JkDoc` to annotate methods/fields with descriptions shown in `--doc` output.
- KBeans must not be instantiated with `new`; use `load(SomeKBean.class)` from within another KBean.
- Initialization logic goes in `init()`, not the constructor (fields are not yet set in the constructor).

**JkRunbase** (`JkRunbase.java`) is the execution context per base directory. It holds the KBean registry, reads `jeka.properties`, and manages the classloader. Each directory with a `jeka.properties` or `jeka-src/` is a runbase.

**Built-in KBeans** (in `core/src/main/java/dev/jeka/core/tool/builtins/`):
- `ProjectKBean` — full Java project build (compile, test, pack, publish)
- `BaseKBean` — lightweight alternative to ProjectKBean for simpler projects/scripts
- `AppKBean` — run Java applications from source/Git
- `DockerKBean` — build Docker images
- `GitKBean` — Git versioning support
- `MavenKBean` — publish to Maven repositories
- `NativeKBean` — GraalVM native compilation
- `IntellijKBean` / `EclipseKBean` — IDE support generation
- `SetupKBean` — JeKa installation/upgrade

### Configuration (`jeka.properties`)

Properties follow the pattern `@beanname.property=value`. The `@` prefix marks a KBean name:
```properties
@project=on                          # activate ProjectKBean
@project.moduleId=com.example:my-lib # set a property
@project.gitVersioning.enable=true
jeka.classpath=dev.jeka:jacoco-plugin # add plugin JARs to classpath
```

Multi-module support via `_jeka.child-bases=core, plugins/*` in root `jeka.properties`.

### Public API (`core/src/main/java/dev/jeka/core/api/`)

All public API classes are prefixed with `Jk`. Key packages:
- `api/project/` — `JkProject`: central model for Java project build (compilation, testing, packaging, publishing)
- `api/depmanagement/` — dependency resolution via Ivy (`JkDependencySet`, `JkCoordinate`, `JkRepo`)
- `api/java/` — JVM toolchain (`JkJavaCompilerToolChain`, `JkJavaProcess`, `JkClassLoader`)
- `api/file/` — file utilities (`JkPathTree`, `JkPathFile`, `JkZipTree`)
- `api/system/` — logging (`JkLog`), process execution (`JkAbstractProcess`)
- `api/tooling/` — Docker, Git, Maven, IntelliJ integrations

### Naming and Style Conventions

- Public classes in `api/` are prefixed with `Jk` to stay discoverable in IDEs.
- Internal/engine classes in `tool/` are not prefixed.
- Fluent API style: methods return `this` or the builder type.
- Avoid third-party dependencies unless critical — the core JAR is intentionally tiny (<2MB).
- Target JDK 21; keep classes `public` only when needed.
- Plugin KBeans extending a framework should follow the naming pattern `XxxPlugin` or `XxxKBean`.

## Testing

- **Unit tests**: `core/src/test/java/dev/jeka/core/tool/` and `core/src/test/java/dev/jeka/core/api/`
- **Integration tests**: `core/src/test/java/dev/jeka/core/integrationtest/` — files named `*IT.java` (hit real Maven repos, requires network)
- **E2E tests**: `core/src/test/java/e2e/` — invoke the actual `jeka` CLI binary
- Slow/integration tests are gated by `JEKA_RUN_SLOW_TEST=true` env var in CI

The `PackageCyclicDependencyTest` enforces that the `api/` packages don't have cyclic dependencies — run it when restructuring packages.

## Releasing

Releases are created via GitHub Releases UI, which triggers the CI workflow to tag, build, and publish to Maven Central (via Central Portal). The local `ant` bootstrap + CI pipeline handles everything; there is no manual publish step.
