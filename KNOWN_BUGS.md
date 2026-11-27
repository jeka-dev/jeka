# Known Bugs

Bugs found but not fixed yet. Listed on 2026-11-24, after PR #261; updated on 2026-11-27, after running the samples on 0.11.70.

- Read this before diagnosing a failure or documenting a feature: the problem may already be known.
- Don't fix an entry as a side effect of another task. Report it instead.
- Remove an entry in the same commit that fixes it. Add new ones with their evidence.
- **Evidence**: *reproduced* = observed by running it; *from source* = read in code, not run; *xxx agent* = observed by that agent only.

## Build and packaging

- **nodejs plugin:** the default `testCmd` fails on a fresh clone: JS tests run before the dependency install. *sample-maintainer agent.*
- **Docker image name doubled** when the module ID has no group: `demo-build-conventions-consumer-demo-build-conventions-consumer`. *sample-maintainer agent.*


## Publication and Maven

### Ivy messages shown as errors
- **Symptom:** `Error: [Ivy] unknown resolver https://...` lines show at the normal log level.
- **Cause:** `IvyMessageLogger` only hides messages starting with `unknown resolver url`, and the resolver names changed.
- **Evidence:** sample-maintainer agent; filter from source.


## Versioning (Git)

- **`app: install repo=<url>#branch`** fails with git exit code 128 and no explanation. *sample-maintainer agent.*


## Engine and command line

## KBeans

## Tests and CI

## Not bugs, but to plan
- Material for MkDocs reaches end of life on 2027-05-05.
- Known limitation: core tests fail with `test.fork=false` (`IllegalAccessError` on package-private classes). In-process, `JkTestProcessor.launch` loads tests in a child of the JeKa classloader, so core main classes come from the running JeKa: two runtime packages. Only projects on the JeKa classpath, as core, are affected; `test.fork=true` (the default) avoids it. A fix would need child-first loading, which loads JeKa twice.
