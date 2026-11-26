# Known Bugs

Bugs found but not fixed yet. Listed on 2026-11-24, after PR #261.

- Read this before diagnosing a failure or documenting a feature: the problem may already be known.
- Don't fix an entry as a side effect of another task. Report it instead.
- Remove an entry in the same commit that fixes it. Add new ones with their evidence.
- **Evidence**: *reproduced* = observed by running it; *from source* = read in code, not run; *xxx agent* = observed by that agent only.

## Build and packaging

### Compiling for Java 8 not verified with a real JDK 8
- **Symptom:** `@project.javaVersion=8` failed with `Unrecognized option: --add-exports=jdk.compiler/...`. The option is no longer passed to JDK 8 compilers, but no JDK 8 build was run since: other options may still fail.
- **Where:** `JkJavaCompilerToolChain.ToolOrProcess.run`.
- **Evidence:** fixed from a fake `javac` in `JkJavaCompilerToolChainTest`.

## Publication and Maven

### A BOM child without project module ID fails late
- **Symptom:** in a multi-module build with `parentBom=true`, a child declaring `@maven.pub.moduleId` but no `@project.moduleId` makes the parent fail with "has no project module ID", possibly after other modules were published. The BOM ignores `@maven.pub.moduleId`.
- **To decide:** list the publication module ID of children in the BOM, or skip children without module ID.
- **Evidence:** reproduced with `maven: info`.

## Versioning (Git)

## Engine and command line

### `-cb=<child> <kbean>:` fails for a KBean existing only in that child
- **Symptom:** `jeka -cb=b build: hi` gives `No KBean found for name 'build'`. KBean names are resolved against the parent.
- **Evidence:** reproduced.

### A child cannot inject its parent when run from the parent
- **Symptom:** `@JkInject("..")` fails with `targets the parent runbase, which is not initialized yet`. Modules injecting each other work, with warnings.
- **Where:** `JkRunbase.getInjectedRunbase`.
- **Evidence:** reproduced by unit test.

### Command-line value equal to a field name is rejected
- **Symptom:** `project: version=moduleId` fails. Works from `jeka.properties`.
- **Where:** `CmdLineParser`. Left as is, so that a field without value does not swallow the next argument.
- **Evidence:** from source.

### Two `@JkDep` on the same line fail
- **Symptom:** `module does not specify version`. Documented in `docs/reference/kbeans-base.md`.
- **Where:** `SourceParser` reads line by line.
- **Evidence:** reproduced by the doc-writer agent.

### Shorthand values ignore quotes
- **Symptom:** `jeka.cmd.hi=-f daemon "Hello"` passes the quotes to the program. May be a known trade-off.
- **Evidence:** reproduced by the sample-maintainer agent with 0.11.63; same code on master.

### `jeka --doc` can fail when an enum initializer runs under two classloaders
- **Symptom:** seen with working-examples/kubernetes. The error now names the exception and its cause, instead of a bare `Failed!`.
- **Where:** `PicocliHelp.cmdHelp` builds its own classloader.
- **Evidence:** reproduced by the sample-maintainer agent.

### `-c` after `-p` cleans `jeka-output`
- **Symptom:** `jeka -p -c` passes `-c` to the program, but the shell script also deletes `jeka-output`, so the program is rebuilt. Same with `-v` and `-q`, which change the script logs.
- **Where:** `compute_VERBOSE_QUIET_CLEAN` in `jeka` reads all arguments; `IsCleanFlagPresent` in `jeka.ps1` too (from source).
- **Evidence:** reproduced with `jeka`.

### `-cb=<unknown>` only warns and exits 0
- **Evidence:** reproduced by the sample-maintainer agent.

## KBeans

- **`eclipse: all` and `intellij: syncAll`** process each module twice when the parent has `child-bases`. Harmless. *Reproduced.*
- **`scaffold.jekaDistribRepo`** writes `jeka.distrib.repo`, which nothing reads. *From source.*
- **`SetupKBean.name`** is not used. Kept, as removing a public field may break users. *From source.*
- **`base: info`** fails in a Spring Boot project: "No class annotated with @SpringBootApplication found". Not reproduced on a project or base scaffolded by the Spring Boot plugin, nor on `samples.springboot`: in a project, it shows `Main Class : dev.jeka.core.tool.Main` instead. *Reproduced by the sample-maintainer agent.*
- **`AppKBean.runtime`**: incomplete or wrong `@JkDoc`, according to an earlier review; the issue was not identified again. *From source.*

## Tests and CI

### Core tests fail with `test.fork=false`
- **Symptom:** `IllegalAccessError` on package-private classes, as JeKa tests itself in-process.
- **Evidence:** reproduced.

### Tests relying on Maven Central are flaky
- **Symptom:** `JkRepoTest.findVersionsOf`, `DependencySetResolutionIT.resolve_usingDynamicVersion_ok` and `JkScaffoldTest.testInterpolateLastVersionOf` failed once in CI and passed when rerun on the same code.
- **Evidence:** reproduced once.

### SonarQube plugin runs no tests
- **Symptom:** `project: test` reports `Test Found: 0` as success: `test/` only holds `SonarqubeRestClientRunner`, a `main` class.
- **Evidence:** reproduced from the root with `project: test`.

## Not bugs, but to plan
- Material for MkDocs reaches end of life on 2027-05-05.
- Four minor SonarCloud issues in `JkBeanDescription` and `BundleKBean`.
- `gradle-plugin` is not on Maven Central yet; docs promise it for the version after 0.11.68.
