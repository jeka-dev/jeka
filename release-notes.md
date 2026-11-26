## Upgrade notes

Some changes may need action when upgrading from 0.11.68:

**Security and trust**
- **Remote Git runs check trust before cloning.** `jeka -r <url> <command>` now asks for confirmation for untrusted URLs, even without `-p`. Without a terminal, it refuses and exits with code 1. In CI, set `JEKA_APP_URL_TRUSTED` or `jeka.app.url.trusted`.
- **Trusted URLs match on whole path segments:** `github.com/foo` no longer trusts `github.com/foo-bar`. URLs trusted from SSH URLs must be trusted again.

**Versions from Git**
- **Released versions:** builds of a tagged, detached commit (as on CI) now remove `tagPrefix` from the version. Tags without the prefix are no longer used.
- **Branch versions** no longer contain `/`: `feat/x` gives `feat-x-SNAPSHOT`.

**Dependencies and publication**
- **Published POM and Ivy files contain the runtime classpath.** `[compile-only]` dependencies are no longer published, and `[runtime]` ones are, with scope `runtime`.
- **BOM versions apply.** Versions managed by a BOM were ignored; they now apply, while versions declared directly still win, as in Maven.
- **Properties of a child POM** override those of its parents in inherited dependency management, as in Maven.
- **A sibling module without a version** now fails with "do not specify version", instead of being dropped from the POM.
- **The parent BOM** lists each child under the module ID it is published with: `@maven.pub.moduleId`, or else `@project.moduleId`.

**Build and packaging**
- **Files put in `jeka-output/classes` before compilation are wiped**, as the class dir is now cleaned before compiling.
- **A `META-INF/MANIFEST.MF` resource is merged** under the generated manifest, in regular and FAT jars: the generated attributes prevail when both set the same one.
- **Shaded jars** relocate only the packages of the dependencies: application and JDK packages keep their names. `META-INF/services` files are now relocated and merged. Class names in other text files (e.g. `logback.xml`) are not relocated.
- **`pack` no longer runs tests for wars.**
- **`jeka.jdk.N` wins over the JDK cache** for jpackage, jlink, jdeps and running projects.
- **Forked tests on a Java target below 21** fail before starting, with a message suggesting `@project.test.fork=false`, instead of an `UnsupportedClassVersionError`.
- Bases built with an older version may hold a stale manifest: run `--clean-work` once.

**Command line, scripts and `app:`**
- **`app: update` and `app: uninstall` fail on an unknown name**, instead of exiting with 0. With `force=true`, `uninstall` only warns.
- **App scripts already installed** keep their old launch line until `app: update`.
- **`-p` on a jar without `Main-Class`** fails with a message suggesting `jeka -c -p`.
- **Scaffolding keeps existing files**, including `jeka` and `jeka.ps1`. Use `setup: updateLocalScripts` to refresh the scripts.
- **KBean property validation is active again:** unknown properties warn, or fail with `--strict`.

## New features

- `app: install` has a `tag` option (`tag=@` for the last commit). After `app: trustUrl`, `app: install repo=... tag=... name=...` asks no question, so it works in scripts and CI.
- The `jeka` and `jeka.ps1` scripts download the JeKa distribution from `jeka.distrib.repo` when it is set.

## Fixes

**Packaging**
- Packing no longer keeps the classes and jar entries of deleted files.
- Shading works with classes compiled for Java 24 and higher (maven-shade-plugin 3.6.2).
- Shaded jars no longer break `ServiceLoader`, nor relocate JDK packages such as `org.xml.sax`.
- A failed shade no longer leaves a partial jar that could be published.
- FAT jars no longer reuse the manifest of a previous build, and their manifest is written first.
- `bundle: customJre=true` works for non-modular projects, and `jdeps` accepts multi-release jars.
- `bundle` (jpackage, jlink, jdeps) uses `jeka.jdk.N`, and `jdeps` analyzes the classes of the bundled Java version.

**Dependencies and Maven**
- Sibling modules appear in published POM and Ivy files, with their module ID and version.
- Compile-only dependencies of a sibling module no longer leak into the runtime classpath.
- BOM versions are resolved from all parent POMs.
- Exclusions apply to transitive dependencies only, so `!com.google.guava:*` no longer excludes guava itself.
- A deps file with only `[processor]`, or an empty one, is not taken for the legacy format.
- `maven: migrateDeps` generates valid dependencies and Java code: provided and system scopes, classifiers, exclusions, BOMs and managed versions.
- `!!` global exclusions in `jeka.project.deps`, and `@pom` in `andBom`.
- `maven: info` resolves BOMs from the download repositories.

**Properties and command line**
- `::shorthand`, `jeka.cmd._append` and download repositories work again from `jeka.properties` and `global.properties`.
- `${x}` can reference environment, system, parent and global values, and an undefined reference gives a clear error.
- Properties of an unknown KBean warn instead of crashing.
- `-p` skips `*-sources.jar` and `*-javadoc.jar`.
- `jeka -r <dir> -c` cleans `<dir>/jeka-output`, not the working dir's.
- Installed app scripts pass `*` and spaces as is.

**KBeans and engine**
- `app:` and `setup:` ignore the `jeka.properties` of the current project.
- Output of short-lived processes, such as Git commands, is no longer lost, and non-ASCII characters are no longer corrupted.
- `@JkInject("../x")` works in a child module built from its parent.
- `$groupId`, `$artifactId` and `$version` work in Docker image names set from properties or the command line.
- `docker.jvmBaseImage` is used again.
- `intellij: syncAll` and `eclipse: all` work again.
- `scaffold.generateLibsFolders=true` works again, and `scaffold.kind=EMPTY` no longer writes `@custom=on`.
- Scaffolded projects have a `public class Main`, so javadoc is no longer empty.
- Building a detached commit without tag uses `<short hash>-SNAPSHOT` as version, instead of failing. `Git-branch` is omitted from the manifest on a detached HEAD.
- Forked compilation for Java 8 no longer passes `-J--add-exports`.
- Forked tests no longer need jansi on the classpath.
- Central Portal publication times out after 15 minutes (it was about 10 days).
- JaCoCo defaults to 0.8.14, which supports Java 25.
- A warning tells when test include patterns match no test.
- Trust of remote Git URLs is saved on Linux too, and trusted URLs are appended without a leading space.

**Messages**
- Clear errors when Maven is missing, Git has no commit or tag, the main jar is missing at publication, a property references itself, an `app:` command runs without a terminal, or a BOM child has no module ID.
- `Failed!` names the exception when it has no message.
- No spinner in redirected output on JDK 22+.
- `--doc` shows `${...}` as is, and "Undocumented." for undocumented init methods.
- `--help` documents `--program`, the name the scripts handle.

## API

All additions:
- `JkLocalProjectDependency.withCoordinate/getCoordinate`, `JkDependencySet.replaceLocalProjectDependenciesByCoordinates`.
- `JkJavaCompilerToolChain.getJdks`, `JkJdeps.setMultiRelease`, `JkNodeJs.setShowOutput`.
- `JkMavenPublication.getManagedDependencies()`.
- The `app:` fields `tag` and `force`.
- `JkInternalEmbeddedClassloader.embeddedLibs` is now public.

One change: `JkUtilsSystem.CONSOLE` is null when output is redirected on JDK 22+.

## Documentation

- Tutorials, KBean reference, properties, API pages and plugin READMEs are corrected, after an audit and a test by a newcomer.
- New tutorial: [migrate a Maven project](https://jeka-dev.github.io/jeka/tutorials/migrate-from-maven/).
- Pages on installation, `app:`, trust, Docker and native images are completed.
- Micronaut and similar frameworks need `FAT` jars, not `SHADE`: the docs now say so.

## Contributing

- JeKa is bootstrapped with `bootstrap.sh` or `bootstrap.ps1` instead of Ant. `CONTRIBUTING.md` is updated.
- Known pending bugs are listed in [KNOWN_BUGS.md](https://github.com/jeka-dev/jeka/blob/master/KNOWN_BUGS.md).
- CI sets `JEKA_RUN_SLOW_TEST`, so slow tests such as the Docker native one run. CI takes longer.
- Tests relying on Maven Central are skipped, not failed, when it does not answer.
- Sample tests no longer need the repo folder to be named `jeka`, and the Node.js plugin has tests.
- Repacking core no longer fails, and the sdkman zip no longer keeps old entries.
