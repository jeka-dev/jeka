# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

## What is JeKa

JeKa is a Java build tool where build logic is written in plain Java/Kotlin (no XML, no DSLs). It bootstraps itself from source: the `jeka` CLI compiles `jeka-src/*.java` on the fly, resolves declared dependencies, and runs the requested actions. The core JAR ships with zero external dependencies.

## Build Commands

JeKa builds itself. The primary way to build is via IntelliJ run configurations, which invoke `dev.jeka.core.tool.Main` directly:

| Config                 | Command args                                                                                                                           | Scope                    |
|------------------------|----------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| `CORE BUILD`           | `project: test pack e2eTest --clean --stacktrace -v`                                                                                   | Core module only         |
| `CORE BUILD skip test` | `project: pack e2eTest --clean --stacktrace -v`                                                                                        | Core without tests       |
| `FULL BUILD`           | `project: test pack e2eTest base: test build: enrichMkDocs maven: publishLocal --clean-work --duration --clean --stacktrace --inspect` | Core + plugins + docs    |
| `FULL BUILD skip test` | Same without `test`                                                                                                                    | Core + plugins, no tests |

From the command line (requires `jeka` on PATH):
```bash
# From core/ directory
jeka project: --doc                             # Show help on project KBean
jeka project: test                              # Run tests
jeka project: test test.includePatterns=ac.me.MyTestClass  # Single test class
jeka project: pack                              # Build JAR
jeka project: test pack e2eTest --clean         # Full core build

# From root directory
jeka build: enrichMkDocs                        # Regenerate docs from @JkDoc annotations
jeka maven: publishLocal                        # Publish to local Maven repo
```

Bootstrap (no `jeka` on PATH yet):
```bash
ant -f .github/workflows/build.xml              # Compile bootstrap + core
```

## Architecture

### Module Structure

```
/core          – Main JeKa engine + reusable build APIs
/plugins/*     – Optional plugins (springboot, sonarqube, jacoco, kotlin, nodejs, protobuf, nexus, centralportal, gradle)
/samples/*     – Example projects demonstrating patterns
/jeka-src/     – Root-level build KBeans (Build, Triage, SdkmanPublisher)
/docs/         – MkDocs source (Material theme)
```

Each module has its own `jeka.properties` and `jeka-src/` directory. The root `jeka.properties` enables multi-module support via `_jeka.child-bases=core, plugins/*`.

### Core Package Split

**`dev.jeka.core.tool`** – Execution engine: parses `jeka-src`, compiles on the fly, injects KBeans, and dispatches CLI actions.

Key classes:
- `Main` – CLI entry point
- `Engine` / `Engines` – Orchestrates parsing → compilation → injection → execution
- `KBean` – Base class for all build definitions
- `JkRunbase` – Per-project execution context
- `RunbaseGraph` – Multi-module dependency graph
- `SourceParser` – Extracts `@JkDep`, `@JkInject` etc. from source without full compilation
- `InitClassesResolver` – Builds the classpath from declared deps

**`dev.jeka.core.api`** – Reusable build APIs, independent of the KBean model:
- `project/JkProject` – Core project model (compilation, testing, packaging)
- `depmanagement/` – Dependency resolution against Maven repos
- `java/` – `JkJavaCompiler`, classpath, process management
- `file/JkPathTree` – Glob-based file tree operations
- `testing/JkTestProcessor` – JUnit Platform integration
- `tooling/` – Git, IntelliJ, Docker, GraalVM helpers
- `system/JkLog` – Logging/process output

**`dev.jeka.core.tool.builtins`** – Built-in KBeans:
- `project/ProjectKBean` – Wraps `JkProject`; the main Java project builder
- `base/BaseKBean` – Compiles and runs `jeka-src` code
- `app/AppKBean` – Runs Java apps directly from Git repos

### KBean Model

KBeans are the fundamental build-configuration unit. Users subclass `KBean` in `jeka-src/`:

```java
public class MyBuild extends KBean {

    @JkDoc("Description shown in help")
    public String myOption = "default";     // public fields = CLI/property configurable

    @JkInject
    private ProjectKBean project;            // inject other KBeans

    @Override
    protected void init() { /* customise after injection */ }

    @JkDoc("Callable from CLI: jeka mybuild: myTask")
    public void myTask() { /* ... */ }
}
```

### Configuration

`jeka.properties` controls KBean enablement and field values:

```properties
@project=on                          # enable ProjectKBean
@project.moduleId=org.example:mylib  # set ProjectKBean.moduleId
jeka.classpath=dev.jeka:springboot-plugin  # add plugin to classpath
```

Properties are also settable from the CLI as `-D` flags or `kbeanName.field=value` positional args.

### API Style Conventions

- All public API classes are prefixed `Jk` (e.g., `JkProject`, `JkPathTree`, `JkDependencySet`).
- Fluent, immutable-style builders throughout the `api/` packages.
- **Zero external dependencies** in the core JAR — only JDK built-ins.
- KBean classes are suffixed `KBean` (e.g., `ProjectKBean`, `BaseKBean`).

### Plugin Pattern

Plugins are KBeans published as separate JARs. They hook into the build via `@JkPostInit` on `ProjectKBean` or `BaseKBean`. Users activate a plugin by adding it to `jeka.classpath` and enabling it with `@pluginName=on`.

### Multi-Module Builds

The root `jeka.properties` lists child bases:
```properties
_jeka.child-bases=core, plugins/*
```
`RunbaseGraph` builds a dependency graph across them. Removing the `_` prefix propagate the property to child modules.

## Testing

- Tests use JUnit 5 (Jupiter).
- E2E/integration tests live under `core/src/test/java/**/integrationtest/`.
- Test output uses PLAIN style for Spring Boot compatibility.
- The `e2eTest` action in `ProjectKBean` runs end-to-end tests as a separate step after `pack`.

## Documentation

Docs live in `/docs/` and are built with MkDocs (Material theme). The `build: enrichMkDocs` action generates reference pages from `@JkDoc` annotations. To preview locally:
```bash
pip install mkdocs-material
mkdocs serve   # http://localhost:8000
```
