# Known Bugs

Bugs found but not fixed yet. Listed on 2026-11-24, after PR #261.

- Read this before diagnosing a failure or documenting a feature: the problem may already be known.
- Don't fix an entry as a side effect of another task. Report it instead.
- Remove an entry in the same commit that fixes it. Add new ones with their evidence.
- **Evidence**: *reproduced* = observed by running it; *from source* = read in code, not run; *xxx agent* = observed by that agent only.

## Build and packaging

### A `META-INF/MANIFEST.MF` resource replaces the generated manifest in FAT jars
- **Symptom:** with a `META-INF/MANIFEST.MF` in the project resources, a FAT jar contains this one only, ignoring version and manifest customizers. A REGULAR jar contains the generated one only. Nothing tells the user. `.jeka-work/jeka-src-classes` may also hold such a file, left by JeKa 0.11.68 or older: use `--clean-work` once.
- **Where:** `JkJarPacker.makeFatJar`.
- **To decide:** which one wins, or merge both.
- **Evidence:** reproduced.

### Forked tests fail on Java targets below 21
- **Symptom:** `@project.javaVersion=17` on a fresh scaffold makes `project: test` fail with `UnsupportedClassVersionError: JkTestProcessor ... class file version 65.0`. Docs use 17 as an example.
- **Workaround:** `@project.test.fork=false`, or target 21+.
- **To decide:** support it, or fail with a clear message, as JeKa needs Java 21.
- **Evidence:** reproduced.

### Micronaut applications do not start from a shaded jar
- **Symptom:** with `jarType=SHADE`, the Micronaut sample prints its banner, then no server starts. Same output with 0.11.68.
- **Where:** `META-INF/micronaut` entries keep their original names, as `MavenJarShader` only transforms `META-INF/services` (inferred). Class names in text files (e.g. `logback.xml`) are not relocated either: this is documented.
- **Workaround:** `jarType=FAT`.
- **Evidence:** reproduced.

### Compiling for Java 8 not verified with a real JDK 8
- **Symptom:** `@project.javaVersion=8` failed with `Unrecognized option: --add-exports=jdk.compiler/...`. The option is no longer passed to JDK 8 compilers, but no JDK 8 build was run since: other options may still fail.
- **Where:** `JkJavaCompilerToolChain.ToolOrProcess.run`.
- **Evidence:** fixed from a fake `javac` in `JkJavaCompilerToolChainTest`.

### `-p` runs a non-runnable JAR left by `project: pack`
- **Symptom:** `no main manifest attribute` after packing a REGULAR jar. `jeka -c -p` works.
- **Evidence:** reproduced by the sample-maintainer agent.

## Publication and Maven

### A BOM child without project module ID fails late
- **Symptom:** in a multi-module build with `parentBom=true`, a child declaring `@maven.pub.moduleId` but no `@project.moduleId` makes the parent fail with "has no project module ID", possibly after other modules were published. The BOM ignores `@maven.pub.moduleId`.
- **To decide:** list the publication module ID of children in the BOM, or skip children without module ID.
- **Evidence:** reproduced with `maven: info`.

## Versioning (Git)

### Tag prefix ignored on detached HEAD
- **Symptom:** a detached commit tagged `v1.0` gives version `v1.0` even with `tagPrefix=v`. Docs say the prefix is removed. Any tag matches, whatever the prefix.
- **Where:** `JkGit.getVersionFromTag`, which uses `git describe --exact-match`.
- **Caution:** fixing it changes released versions for anyone building this way.
- **Evidence:** from source.

### Git versioning keeps the `/` of branch names
- **Symptom:** on branch `feat/slash`, the version is `feat/slash-SNAPSHOT`. `maven: publishLocal` writes `org/ex/gv/feat/slash-SNAPSHOT/gv-feat/slash-SNAPSHOT.jar`, without any warning. The `core` build creates `jeka-output/jeka-core-feat/...` directories.
- **Where:** `JkGit.getVersionFromTag` and siblings return `branch + "-SNAPSHOT"`.
- **To decide:** which character replaces `/`. Changes the versions of branch builds.
- **Evidence:** reproduced with a local publication.

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

### `app: install` needs a terminal
- **Symptom:** without a terminal, `app:` commands fail with "the 'app' commands need an interactive terminal". The trust, version and name prompts can't be answered otherwise: `repo=...#tag` is not supported, and there is no option for the tag.
- **To decide:** which options to add for a non-interactive install.
- **Evidence:** reproduced with `< /dev/null`.

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
- **`AppKBean.install`** tells to use `remote=`; the field is `repo`. *From source.*
- **`SetupKBean.name`** is not used. Kept, as removing a public field may break users. *From source.*
- **`SecurityChecker.addTrustedUrl`** writes a leading space in `jeka.app.url.trusted`. Harmless for Java, not checked in the shell script. *Reproduced by the doc-writer agent.*
- **`NodeJsKBean.init` `@JkDoc`** says it configures the project, but it only registers a clean action. *From source.*
- **`base: info`** fails in a Spring Boot project: "No class annotated with @SpringBootApplication found". *Reproduced by the sample-maintainer agent.*
- **`docker: build`** prints "Native compilation started at" for JVM images. *Reproduced by the naive agent.*
- **`nodeJs: exec`** shows its output only with `-v`. *Reproduced by the naive agent.*
- **`jeka.project.deps` with only `[processor]`** fails with a "legacy format" error. *Reproduced by the doc-writer agent.*
- **`scaffold.kind=EMPTY`** leaves `@custom=on` without `Custom` class, which gives a warning. *Reproduced by the doc-writer agent.*
- **`DockerKBean.jvmBaseImage`** is never used. *From source.*
- **`app: update` and `app: uninstall`** with an unknown name exit with code 0. *Reproduced by the naive agent.*
- **`AppKBean.name`, `AppKBean.runtime`, `ProjectKBean.test.fork`, `MavenKBean` `parentBom`**: incomplete or wrong `@JkDoc`. *From source.*
- **Empty `jeka.project.deps`** gives a "has still legacy format" warning. *Reproduced by the naive agent.*
- **`bundle`**: `jdeps` runs with `--multi-release base`, so modules used only by version-specific classes are not detected. Add them with `--add-modules`. *From source.*

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
