# Known Bugs

Bugs found but not fixed yet. Listed on 2026-11-24, after PR #261.

- Read this before diagnosing a failure or documenting a feature: the problem may already be known.
- Don't fix an entry as a side effect of another task. Report it instead.
- Remove an entry in the same commit that fixes it. Add new ones with their evidence.
- **Evidence**: *reproduced* = observed by running it; *from source* = read in code, not run.

## Build and packaging

### Shading fails on Java 25 classes
- **Symptom:** `jarType=SHADE` (or `shadeJarClassifier`) fails with `Unsupported class file major version 69`. Scaffolds currently set `jeka.java.version=25`.
- **Where:** maven-shade-plugin 3.6.0, in `JkInternalJarShader`.
- **Evidence:** reproduced. Works with Java 21.
- **When fixed:** remove the note in `docs/reference/kbeans-maven.md`.

### Stale manifest in `base` builds
- **Symptom:** after a REGULAR or SHADE build, later FAT jars reuse the old manifest (missing version and custom attributes), even with `--clean`.
- **Where:** `JkJarPacker.makeJar` writes `MANIFEST.MF` in `.jeka-work/jeka-src-classes`.
- **Workaround:** `--clean-work`.
- **Evidence:** reproduced by the doc-writer agent.

### Unclear error when the main jar is missing at publication
- **Symptom:** `No artifact file and artifact maker found for [main].jar...` instead of suggesting to run `pack`.
- **Evidence:** reproduced.

### `pack` without `--clean` can fail on an existing sdkman directory
- **Symptom:** packing `core` a second time fails with `FileAlreadyExistsException: .../jeka-core-master-SNAPSHOT-sdkman/LICENSE`.
- **Workaround:** `--clean`.
- **Evidence:** reproduced once.

## Versioning (Git)

### Tag prefix ignored on detached HEAD
- **Symptom:** a detached commit tagged `v1.0` gives version `v1.0` even with `tagPrefix=v`. Docs say the prefix is removed. Any tag matches, whatever the prefix.
- **Where:** `JkGit.getVersionFromTag`, which uses `git describe --exact-match`.
- **Caution:** fixing it changes released versions for anyone building this way.
- **Evidence:** from source.

### `Git-branch: null` in manifest on detached HEAD
- **Where:** `JkVersionFromGit.handleVersioning`. Cosmetic.
- **Evidence:** reproduced.

### `git: lastTag` without any tag shows a raw Git error
- **Evidence:** reproduced by the doc-writer agent.

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

### Self-referencing property recurses endlessly
- **Symptom:** `foo=${foo}` in a properties file ends with a `StackOverflowError`, when `foo` is not defined by a system property or an environment variable.
- **Evidence:** from source.

### Process output: non-ASCII characters are corrupted
- **Where:** `JkUtilsIO.JkStreamGobbler` writes each decoded char as a single byte.
- **Evidence:** from source.

### `--program-args` is advertised but not handled
- **Symptom:** `--help` shows `--program-args`, the shell script only handles `-p` and `--program`.
- **Where:** `PicocliMainCommand`, `core/src/main/shell/jeka`.
- **Evidence:** from source.

### `${...}` in `@JkDoc` is displayed as `null` by `--doc`
- **Workaround:** don't write `${...}` in `@JkDoc` texts.
- **Evidence:** reproduced by the doc-writer agent.

### Two `@JkDep` on the same line fail
- **Symptom:** `module does not specify version`. Documented in `docs/reference/kbeans-base.md`.
- **Where:** `SourceParser` reads line by line.
- **Evidence:** reproduced by the doc-writer agent.

## KBeans

- **`eclipse: all` and `intellij: syncAll`** process each module twice when the parent has `child-bases`. Harmless. *Reproduced.*
- **`scaffold.jekaDistribRepo`** writes `jeka.distrib.repo`, which nothing reads. *From source.*
- **`AppKBean.install`** tells to use `remote=`; the field is `repo`. *From source.*
- **`SetupKBean.name`** is not used. Kept, as removing a public field may break users. *From source.*
- **`SecurityChecker.addTrustedUrl`** writes a leading space in `jeka.app.url.trusted`. Harmless for Java, not checked in the shell script. *Reproduced by the doc-writer agent.*
- **`NodeJsKBean.init` `@JkDoc`** says it configures the project, but it only registers a clean action. *From source.*
- **`bundle`**: `jdeps` runs with `--multi-release base`, so modules used only by version-specific classes are not detected. Add them with `--add-modules`. *From source.*

## Tests and CI

### Node.js plugin runs no tests
- **Symptom:** 0 test found in CI, reported as success. Without CI's classpath: `Cannot create Launcher without at least one TestEngine`.
- **Evidence:** reproduced.

### Docker native test never runs in CI
- **Where:** `ScaffoldTest.docker_native_image_ok` needs `JEKA_RUN_SLOW_TEST=true`, which `push-master.yml` sets only for the packaging step.
- **Evidence:** reproduced (1 skipped in CI). It passes on macOS with Docker.

### Sample tests need a checkout folder named `jeka`
- **Symptom:** `jacoco_ok`, `sonarqube_ok` and `protobuf_ok` fail with `...plugin.jar not found`.
- **Where:** `SamplesTest.pluginsDir()` in `jeka-src/_dev/test`.
- **Evidence:** reproduced.

### E2E and sample tests need jansi on the classpath
- **Symptom:** `NoClassDefFoundError: org/jline/jansi/Ansi`.
- **Workaround:** `-cp=core/libs/compile-only/jansi-3.30.4.jar`, as in CI.
- **Evidence:** reproduced.

### Core tests fail with `test.fork=false`
- **Symptom:** `IllegalAccessError` on package-private classes, as JeKa tests itself in-process.
- **Evidence:** reproduced.

### Tests relying on Maven Central are flaky
- **Symptom:** `JkRepoTest.findVersionsOf`, `DependencySetResolutionIT.resolve_usingDynamicVersion_ok` and `JkScaffoldTest.testInterpolateLastVersionOf` failed once in CI and passed when rerun on the same code.
- **Evidence:** reproduced once.

## Not bugs, but to plan
- Material for MkDocs reaches end of life on 2027-05-05.
- Four minor SonarCloud issues in `JkBeanDescription` and `BundleKBean`.
- `gradle-plugin` is not on Maven Central yet; docs promise it for the version after 0.11.68.
