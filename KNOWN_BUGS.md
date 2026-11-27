# Known Bugs

Bugs found but not fixed yet. Listed on 2026-11-24, after PR #261; updated on 2026-11-27, after running the samples on 0.11.70 and fixing #293 to #297.

- Read this before diagnosing a failure or documenting a feature: the problem may already be known.
- Don't fix an entry as a side effect of another task. Report it instead.
- Remove an entry in the same commit that fixes it. Add new ones with their evidence.
- **Evidence**: *reproduced* = observed by running it; *from source* = read in code, not run; *xxx agent* = observed by that agent only.

## Build and packaging

### nodejs plugin: JS tests run before the dependency install
- **Symptom:** with the configuration of the plugin README (`@nodeJs.buildCmd=npm install, npm run build`, `@nodeJs.testCmd=npm run test-headless`, `@nodeJs.configureProject=true`), `project: test pack` on a fresh clone runs the JS tests before `npm install`, and they fail.
- **Cause:** `JkNodeJsProject.registerIn` appends the JS tests to `project.test.postActions`, while the build commands, which hold `npm install`, run in `project.pack.actions`, after the tests.
- **Where:** `plugins/plugins.nodejs/src/dev/jeka/plugins/nodejs/JkNodeJsProject.java`, `registerIn`.
- **Workaround:** put the install in the test commands too: `@nodeJs.testCmd=npm install, npm run test-headless`.
- **Decision needed:** document the workaround in the README, run the build commands before the tests, or install the dependencies before the tests when `node_modules` is missing (`npm` only, while `buildCmd` may use another tool).
- **Evidence:** sample-maintainer agent; order of the actions from source.

### Docker image name doubled when the module ID has no group
- **Symptom:** `@base.moduleId=demo-consumer` gives the image `demo-consumer-demo-consumer:latest` (`docker: info`).
- **Cause:** `JkModuleId.of("demo-consumer")` uses the name as group too, as documented, and `DockerKBean.computeImageName` joins group and name with `-`.
- **Where:** `DockerKBean.computeImageName(JkModuleId, JkVersion, Path)`.
- **Workaround:** set `@docker.jvmImageName` (and `@docker.nativeImageName`).
- **Decision needed:** using only the name when the group equals it changes the name of the images that existing projects build and push.
- **Evidence:** reproduced on master (2026-11-27).

## Publication and Maven

### Ivy messages shown as errors
- **Symptom:** `Error: [Ivy] unknown resolver https://...` lines show at the normal log level.
- **Cause:** `IvyMessageLogger` only hides messages starting with `unknown resolver url`, and the resolver names changed.
- **Evidence:** sample-maintainer agent; filter from source. Not reproduced on master (2026-11-27): resolver names are `url:https://...`, so the cached `ivydata-*.properties` hold `resolver=url:https://...`, which the filter hides. Resolving `commons-text:1.12.0` from `repo1.maven.org`, then `repo.maven.apache.org`, then `repo1` again, logged nothing. The exact message and the command that shows it are needed; otherwise, candidate for removal.


## Versioning (Git)

### `app: install repo=<url>#branch` fails with a bare git error
- **Symptom:** `jeka app: install repo=https://github.com/jeka-dev/demo-cowsay#master` fails with `Process has returned with error code 128`, after the git command line, with no explanation.
- **Cause:** `repo` is passed as is to `git ls-remote --symref`, and git rejects the `#` fragment. `app: install` doesn't support `#`: the version goes in `tag=`, and branches other than the default one are not supported.
- **Where:** `AppKBean.install`, `AppManager.getRemoteDefaultBranch`.
- **Decision needed:** fail early with a message pointing to `tag=`, or accept `#<tag>` as `jeka -r <url>#<tag>` does.
- **Evidence:** reproduced on master (2026-11-27).


## Engine and command line

### `jeka --doc`: the last line breaks inside a colour code
- **Symptom:** the footer `Execute jeka <kbean>: --doc (as jeka docker: --doc) to get details on specific KBean.` is split over two lines, after the italic `jeka docker: --doc` and before its reset code, so the second line starts with `[m)`.
- **Cause:** probably picocli wrapping the footer at the usage width, counting the ANSI codes as characters. Not checked.
- **Where:** `PicocliHelp`, footer built at line 188.
- **Evidence:** naive agent, on 0.11.71 and master-SNAPSHOT (2026-11-27), output not on a terminal: `jeka --doc 2>&1 | tail -3 | od -c` shows the line break between `ESC` and `[m`; footer code read from source on master.

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
