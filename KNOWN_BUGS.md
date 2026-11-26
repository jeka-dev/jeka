# Known Bugs

Bugs found but not fixed yet. Listed on 2026-11-24, after PR #261.

- Read this before diagnosing a failure or documenting a feature: the problem may already be known.
- Don't fix an entry as a side effect of another task. Report it instead.
- Remove an entry in the same commit that fixes it. Add new ones with their evidence.
- **Evidence**: *reproduced* = observed by running it; *from source* = read in code, not run; *xxx agent* = observed by that agent only.

## Build and packaging

## Publication and Maven

## Versioning (Git)

## Engine and command line

### `jeka --doc` can fail when an enum initializer runs under two classloaders
- **Symptom:** seen with working-examples/kubernetes. The error now names the exception and its cause, instead of a bare `Failed!`.
- **Where:** `PicocliHelp.cmdHelp` builds its own classloader.
- **Evidence:** reproduced by the sample-maintainer agent.

### `-c` after `-p` cleans `jeka-output`
- **Symptom:** `jeka -p -c` passes `-c` to the program, but the shell script also deletes `jeka-output`, so the program is rebuilt. Same with `-v` and `-q`, which change the script logs.
- **Where:** `compute_VERBOSE_QUIET_CLEAN` in `jeka` reads all arguments; `IsCleanFlagPresent` in `jeka.ps1` too (from source).
- **Evidence:** reproduced with `jeka`.

## KBeans

- **`eclipse: all` and `intellij: syncAll`** process each module twice when the parent has `child-bases`. Harmless. *Reproduced.*
- **`SetupKBean.name`** is not used. Kept, as removing a public field may break users. *From source.*
- **`base: info`** fails in a Spring Boot project: "No class annotated with @SpringBootApplication found". Not reproduced on a project or base scaffolded by the Spring Boot plugin, nor on `samples.springboot`: in a project, it shows `Main Class : dev.jeka.core.tool.Main` instead. *Reproduced by the sample-maintainer agent.*
- **`AppKBean.runtime`**: incomplete or wrong `@JkDoc`, according to an earlier review; the issue was not identified again. *From source.*

## Tests and CI

### Core tests fail with `test.fork=false`
- **Symptom:** `IllegalAccessError` on package-private classes, as JeKa tests itself in-process.
- **Evidence:** reproduced.

### SonarQube plugin runs no tests
- **Symptom:** `project: test` reports `Test Found: 0` as success: `test/` only holds `SonarqubeRestClientRunner`, a `main` class.
- **Evidence:** reproduced from the root with `project: test`.

## Not bugs, but to plan
- Material for MkDocs reaches end of life on 2027-05-05.
- Four minor SonarCloud issues in `JkBeanDescription` and `BundleKBean`.
- `gradle-plugin` is not on Maven Central yet; docs promise it for the version after 0.11.68.
