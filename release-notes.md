# Next release

## Fixes

**Build**
- `@project.javaVersion=8` downloads a JDK 8 on Linux and macOS: foojay rejected the download URL for Java 8. When a JDK download fails, the running JDK compiles with `--release <version>`, instead of producing classes of its own version with only a warning.

**Dependencies and Maven**
- When a dependency version is evicted by a newer one met later, the jar of the newer one is no longer missing from the classpath. `kotlin-compiler` 2.1.21 and later compile, instead of failing with `NoClassDefFoundError: org/jetbrains/annotations/NotNull`. If you tried such a version, delete `~/.jeka/cache/kotlinc/<version>`, and the `.jeka-work` dir of affected projects.
- Changing only a BOM version resolves the dependencies again, instead of reusing the classpath cached in `.jeka-work`.
- Versions follow the BOM again (regression of 0.11.70): with `spring-boot-dependencies:3.5.16`, `httpclient5` is 5.5.2 again, not 5.3.1. As in Maven, only `pom` dependencies with scope `import` are imported BOMs, the versions a BOM declares win over the ones it imports, and the first import wins over the next ones.

**`app:` and Git**
- `app: install` proposes `0.11.69`, not `0.11.69^{}`, for an annotated tag. `app: list` and `app: update` no longer take an app installed on an annotated tag for outdated.

# 0.11.70

## Upgrade notes

Some changes may need action when upgrading from 0.11.69:

**Versions from Git**
- **Released versions:** builds of a tagged, detached commit (as on CI) now remove `tagPrefix` from the version. Tags without the prefix are no longer used.
- **Branch versions** no longer contain `/`: `feat/x` gives `feat-x-SNAPSHOT`.

**Dependencies and publication**
- **Properties of a child POM** override those of its parents in inherited dependency management, as in Maven.
- **A sibling module without a version** now fails with "do not specify version", instead of being dropped from the POM.
- **The parent BOM** lists each child under the module ID it is published with: `@maven.pub.moduleId`, or else `@project.moduleId`.

**Build and packaging**
- **Files put in `jeka-output/classes` before compilation are wiped**, as the class dir is now cleaned before compiling.
- **A `META-INF/MANIFEST.MF` resource is merged** under the generated manifest, in regular and FAT jars: the generated attributes prevail when both set the same one.
- **`pack` no longer runs tests for wars.**
- **`jeka.jdk.N` wins over the JDK cache** for jpackage, jlink, jdeps and running projects.
- **Forked tests on a Java target below 21** fail before starting, with a message suggesting `@project.test.fork=false`, instead of an `UnsupportedClassVersionError`.

**Command line, scripts and `app:`**
- **`-cb=<location>` fails when no declared child base matches**, instead of only warning. Use `--force` to keep the warning. The location may use `/` or `\` on any OS, and a trailing `/` is ignored.
- **`app: update` and `app: uninstall` fail on an unknown name**, instead of exiting with 0. With `force=true`, `uninstall` only warns.
- **App scripts already installed** keep their old launch line until `app: update`.
- **`-p` on a jar without `Main-Class`** fails with a message suggesting `jeka -c -p`.
- **Options after `-p` go to the program:** with the `jeka` and `jeka.ps1` scripts, `-c`, `-v` and `-q` written after `-p` are passed to the program, and no longer clean `jeka-output` or change JeKa's log level. Put them before `-p` to act on JeKa.
- **`setup: name` is removed**: it was never used. `jeka setup: name=...` now fails with `Unmatched argument`.

## New features

- `app: install` has a `tag` option (`tag=@` for the last commit). After `app: trustUrl`, `app: install repo=... tag=... name=...` asks no question, so it works in scripts and CI.
- The `jeka` and `jeka.ps1` scripts download the JeKa distribution from `jeka.distrib.repo` when it is set.

## Fixes

**Packaging**
- Packing no longer keeps the classes and jar entries of deleted files.
- FAT jars write their manifest first.
- `bundle` (jpackage, jlink, jdeps) uses `jeka.jdk.N`, and `jdeps` analyzes the classes of the bundled Java version.

**Dependencies and Maven**
- Sibling modules appear in published POM and Ivy files, with their module ID and version.
- Compile-only dependencies of a sibling module no longer leak into the runtime classpath.
- BOM versions are resolved from all parent POMs.
- Exclusions apply to transitive dependencies only, so `!com.google.guava:*` no longer excludes guava itself.
- A deps file with only `[processor]`, or an empty one, is not taken for the legacy format.

**Properties and command line**
- `-p` skips `*-sources.jar` and `*-javadoc.jar`.
- `jeka -r <dir> -c` cleans `<dir>/jeka-output`, not the working dir's.
- Installed app scripts pass `*` and spaces as is.
- `-cb=<child>` finds the KBeans that only the child declares.
- A field name is accepted as a command-line value, as `version=moduleId`.
- A boolean field written without a value, as `project: test.skip`, is set to `true`.
- Quoted words stay together in `jeka.cmd.*` shorthands run by the scripts with `-p`.

**KBeans and engine**
- A child can inject its parent with `@JkInjectRunbase` when it is built from that parent.
- All annotations on a line of `jeka-src` are read, as several `@JkDep` on one line.
- `docker.jvmBaseImage` is used again.
- `intellij: syncAll` and `eclipse: all` sync each module once from a parent.
- `scaffold.kind=EMPTY` no longer writes `@custom=on`.
- Scaffolded projects have a `public class Main`, so javadoc is no longer empty.
- `Git-branch` is omitted from the manifest on a detached HEAD.
- Forked compilation for Java 8 no longer passes `-J--add-exports`.
- JaCoCo defaults to 0.8.14, which supports Java 25.
- The SonarQube plugin passes the project base dir and source paths without `..` segments, and has tests.
- Trusted URLs are appended without a leading space.

**Messages**
- Clear errors when Maven is missing, Git has no commit or tag, the main jar is missing at publication, a property references itself, an `app:` command runs without a terminal, or a BOM child has no module ID.
- `Failed!` names the exception when it has no message.
- No spinner in redirected output on JDK 22+.
- `--doc` shows `${...}` as is, and "Undocumented." for undocumented init methods.
- `--doc` no longer fails when a KBean's enum initializer would run under two classloaders: KBean classes are loaded once.
- `--help` documents `--program`, the name the scripts handle.

## API

All additions:
- `JkLocalProjectDependency.getCoordinate`, `JkDependencySet.replaceLocalProjectDependenciesByCoordinates`.
- `JkJavaCompilerToolChain.getJdks`, `JkJdeps.setMultiRelease`, `JkNodeJs.setShowOutput`.
- The `app:` fields `tag` and `force`.
- `JkInternalEmbeddedClassloader.embeddedLibs` is now public.

One change: `JkUtilsSystem.CONSOLE` is null when output is redirected on JDK 22+.

## Documentation

- Micronaut and similar frameworks need `FAT` jars, not `SHADE`: the docs now say so.
- The BUNDLE runtime of `app: install` is described as it works.

## Contributing

- CI sets `JEKA_RUN_SLOW_TEST`, so slow tests such as the Docker native one run. CI takes longer.
- Tests relying on Maven Central are skipped, not failed, when it does not answer.
- Sample tests no longer need the repo folder to be named `jeka`, and the Node.js plugin has tests.
- Repacking core no longer fails, and the sdkman zip no longer keeps old entries.
