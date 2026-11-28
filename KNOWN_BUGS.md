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

### `intellij: sync` drops the SDK and the iml customizers in the default, single-module mode
- **Symptom:** the `.iml` written misses what `IntellijKBean` customizers add. E.g. in `plugins/plugins.protobuf`, `ProtobufCustom` calls `replaceLibByModule("dev.jeka.jeka-core.jar", "core")`, but `jeka intellij: iml` writes a `module-library` on `dev.jeka.jeka-core.jar` instead of the `core` module dependency of the committed `.idea/plugins.protobuf.iml`.
- **Cause:** `IntellijKBean.generateSingleIml` writes the file, then sets the SDK (`computeImlJdk`, which reads the existing `.iml`) and runs `imlCustomizers` on the iml object, after it was written. Only the split mode (`splitModule=true`, or a project Java version other than the running one) writes after them.
- **Where:** `IntellijKBean.generateSingleIml`.
- **Workaround:** `@intellij.splitModule=true`: the project `.iml` then holds the customizations.
- **Evidence:** reproduced on master (2026-11-27) in `plugins/plugins.protobuf`; order of the calls from source; workaround run there.

## Tests and CI

- **Outdated samples:** `samples/samples.baseapp` and `samples/samples.project-api` set `jeka.java.version=17`, below the Java 21 JeKa needs to run. `samples/samples.project-flat-facade-api` has no command running its test, and outdated dependencies. *sample-maintainer agent.*


## Not bugs, but to plan
- Material for MkDocs reaches end of life on 2027-05-05.
- Known limitation: core tests fail with `test.fork=false` (`IllegalAccessError` on package-private classes). In-process, `JkTestProcessor.launch` loads tests in a child of the JeKa classloader, so core main classes come from the running JeKa: two runtime packages. Only projects on the JeKa classpath, as core, are affected; `test.fork=true` (the default) avoids it. A fix would need child-first loading, which loads JeKa twice.
