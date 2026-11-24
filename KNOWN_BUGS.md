# Known Bugs

Bugs found but not fixed yet. Listed on 2026-11-24, after PR #261.

- Read this before diagnosing a failure or documenting a feature: the problem may already be known.
- Don't fix an entry as a side effect of another task. Report it instead.
- Remove an entry in the same commit that fixes it. Add new ones with their evidence.
- **Evidence**: *reproduced* = observed by running it; *from source* = read in code, not run; *naive agent* / *doc-writer agent* = observed by that agent only.

## Build and packaging

### Shading fails on Java 25 classes
- **Symptom:** `jarType=SHADE` (or `shadeJarClassifier`) fails with `Unsupported class file major version 69`. Scaffolds currently set `jeka.java.version=25`.
- **Where:** maven-shade-plugin 3.6.0, in `JkInternalJarShader`.
- **Evidence:** reproduced. Works with Java 21.
- **When fixed:** remove the note in `docs/reference/kbeans-maven.md`.

### A `META-INF/MANIFEST.MF` resource replaces the generated manifest in FAT jars
- **Symptom:** with a `META-INF/MANIFEST.MF` in the project resources, a FAT jar contains this one only, ignoring version and manifest customizers. A REGULAR jar contains the generated one only. Nothing tells the user. `.jeka-work/jeka-src-classes` may also hold such a file, left by JeKa 0.11.68 or older: use `--clean-work` once.
- **Where:** `JkJarPacker.makeFatJar`.
- **To decide:** which one wins, or merge both.
- **Evidence:** reproduced.

### Deleted classes stay in the jar without `--clean`
- **Symptom:** after deleting a source file, `project: pack` still packages its class.
- **Where:** `jeka-output/classes` is not cleaned, and `JkAbstractPathTree.zipTo` adds to the existing jar.
- **Workaround:** `--clean`.
- **Evidence:** reproduced.

### Unclear error when the main jar is missing at publication
- **Symptom:** `No artifact file and artifact maker found for [main].jar...` instead of suggesting to run `pack`.
- **Evidence:** reproduced.

### `pack` without `--clean` can fail on an existing sdkman directory
- **Symptom:** packing `core` a second time fails with `FileAlreadyExistsException: .../jeka-core-master-SNAPSHOT-sdkman/LICENSE`.
- **Workaround:** `--clean`.
- **Evidence:** reproduced once.

### Forked tests fail on Java targets below 21
- **Symptom:** `@project.javaVersion=17` on a fresh scaffold makes `project: test` fail with `UnsupportedClassVersionError: JkTestProcessor ... class file version 65.0`. Docs use 17 as an example.
- **Workaround:** `@project.test.fork=false`, or target 21+.
- **To decide:** support it, or fail with a clear message, as JeKa needs Java 21.
- **Evidence:** reproduced.

### JaCoCo plugin fails on Java 25 classes
- **Symptom:** default JaCoCo 0.8.12 gives `Unsupported class file major version 69` on a fresh scaffold (Java 25).
- **Workaround:** `@jacoco.jacocoVersion=0.8.14`.
- **Evidence:** reproduced by the naive agent.

### `createWarFile=true` runs tests and warns on `project: pack`
- **Symptom:** with `@springboot.createWarFile=true`, `project: pack` runs the tests and warns that `jeka-war...` is empty or does not exist.
- **Evidence:** reproduced by the naive agent.

## Publication and Maven

### Sibling module missing from the published POM
- **Symptom:** a module depending on `../plugin-common` in `jeka.project.deps` is published without that dependency.
- **Evidence:** reproduced by the naive agent.

### Compile-only dependencies of a sibling module leak into the runtime classpath
- **Symptom:** a module declaring `../a` in `[compile]` gets the `[compile-only]` dependencies of `a` (e.g. Lombok) in its runtime and test classpaths.
- **Where:** `JkProject.toDependency` exports the compile and runtime dependencies merged.
- **Evidence:** reproduced with `project: depTree`.

### `maven: migrateDeps` generates an invalid `jeka.project.deps`
- **Symptom:** a `provided` dependency becomes `- org.projectlombok:lombok` in `[compile-only]`, without version. Using it fails with `Coordinate specification '- org...' is not correct`.
- **Evidence:** reproduced (generated output).

### NPE message when a project has no module ID
- **Symptom:** in a multi-module build with `parentBom=true`, a child declaring `@maven.pub.moduleId` but no `@project.moduleId` fails with `Cannot invoke "JkModuleId.toColonNotation()"...`, possibly after other modules were published. `docs/reference/multi-modules.md` shows this configuration.
- **Evidence:** reproduced with `maven: info`.

### Javadoc warning when publishing a scaffolded project
- **Symptom:** warning with status 1.
- **Evidence:** reproduced by the naive agent.

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

### Untrusted remote run without a terminal fails silently
- **Symptom:** `jeka -r <untrusted url>` without a terminal clones the repo first, then exits 1 without message.
- **Evidence:** reproduced by the naive agent.

### Git versioning without any commit gives a bare Git error
- **Symptom:** `error code 128` or `129` on `git diff HEAD --stat`.
- **Evidence:** reproduced by the naive agent.

## KBeans

- **`eclipse: all` and `intellij: syncAll`** process each module twice when the parent has `child-bases`. Harmless. *Reproduced.*
- **`scaffold.jekaDistribRepo`** writes `jeka.distrib.repo`, which nothing reads. *From source.*
- **`AppKBean.install`** tells to use `remote=`; the field is `repo`. *From source.*
- **`SetupKBean.name`** is not used. Kept, as removing a public field may break users. *From source.*
- **`SecurityChecker.addTrustedUrl`** writes a leading space in `jeka.app.url.trusted`. Harmless for Java, not checked in the shell script. *Reproduced by the doc-writer agent.*
- **`NodeJsKBean.init` `@JkDoc`** says it configures the project, but it only registers a clean action. *From source.*
- **`docker: build`** prints "Native compilation started at" for JVM images. *Reproduced by the naive agent.*
- **`nodeJs: exec`** shows its output only with `-v`. *Reproduced by the naive agent.*
- **Empty `jeka.project.deps`** gives a "has still legacy format" warning. *Reproduced by the naive agent.*
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
