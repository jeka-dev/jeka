# Known Bugs

Bugs found but not fixed yet. Listed on 2026-11-24, after PR #261; updated on 2026-11-28, after fixing #308 to #315.

- Read this before diagnosing a failure or documenting a feature: the problem may already be known.
- Don't fix an entry as a side effect of another task. Report it instead.
- Remove an entry in the same commit that fixes it. Add new ones with their evidence.
- **Evidence**: *reproduced* = observed by running it; *from source* = read in code, not run; *xxx agent* = observed by that agent only.

## Build and packaging

## Publication and Maven

## Versioning (Git)

## Engine and command line

## KBeans

## Tests and CI

- **Outdated samples:** `samples/samples.baseapp` and `samples/samples.project-api` set `jeka.java.version=17`, below the Java 21 JeKa needs to run. `samples/samples.project-flat-facade-api` has no command running its test, and outdated dependencies. *sample-maintainer agent.*


## Not bugs, but to plan
- Material for MkDocs reaches end of life on 2027-05-05.
- Known limitation: core tests fail with `test.fork=false` (`IllegalAccessError` on package-private classes). In-process, `JkTestProcessor.launch` loads tests in a child of the JeKa classloader, so core main classes come from the running JeKa: two runtime packages. Only projects on the JeKa classpath, as core, are affected; `test.fork=true` (the default) avoids it. A fix would need child-first loading, which loads JeKa twice.
