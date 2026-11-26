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

## KBeans

- **`SetupKBean.name`** is not used. Kept, as removing a public field may break users. *From source.*
- **`base: info`** fails in a Spring Boot project: "No class annotated with @SpringBootApplication found". Not reproduced on master `50afb5fad` with a project having `@project=on` and `@springboot=on`, run as `base: info`, `project: base: info` or `springboot: base: info`: `SpringbootKBean` replaces the main class finder of `base` only when no `ProjectKBean` is found, and in a base, the error is expected when `jeka-src` has no such class. In a project, `base: info` shows `Main Class : dev.jeka.core.tool.Main`: the fallback of `BaseKBean` when `jeka-src` has no main class, by design. *Reproduced by the sample-maintainer agent, once.*

## Tests and CI

### Core tests fail with `test.fork=false`
- **Symptom:** `IllegalAccessError` on package-private classes, as JeKa tests itself in-process.
- **Cause:** in-process, `JkTestProcessor` runs tests in a `URLClassLoader` whose parent is the JeKa classloader, searched first. The main classes of core then come from the running JeKa, and its test classes from the child loader: Java sees two runtime packages, so package-private access fails. Fixing it would need child-first loading, which would load JeKa twice. It only affects projects whose classes are on the JeKa classpath, as core; `test.fork=true` (the default) avoids it.
- **Where:** `JkTestProcessor.launch`.
- **Evidence:** reproduced; cause from source.

## Not bugs, but to plan
- Material for MkDocs reaches end of life on 2027-05-05.
- Four minor SonarCloud issues in `JkBeanDescription` and `BundleKBean`.
- `gradle-plugin` is not on Maven Central yet; docs promise it for the version after 0.11.68.
