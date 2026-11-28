# Known Bugs

Bugs found but not fixed yet. Listed on 2026-11-24, after PR #261; updated on 2026-11-28, after the sample-maintainer run of that day.

- Read this before diagnosing a failure or documenting a feature: the problem may already be known.
- Don't fix an entry as a side effect of another task. Report it instead.
- Remove an entry in the same commit that fixes it. Add new ones with their evidence.
- **Evidence**: *reproduced* = observed by running it; *from source* = read in code, not run; *xxx agent* = observed by that agent only.

## Build and packaging

### FAT jar keeps only one copy of duplicate `META-INF` files
- **Symptom:** a FAT jar of a Spring Boot 4 app starts without its web server: only one `META-INF/spring/...AutoConfiguration.imports` is kept. `META-INF/services` files of several jars would be lost the same way.
- **Cause:** `JkJarWriter` ignores an entry already written instead of merging service-like files.
- **Where:** `core/src/main/java/dev/jeka/core/api/java/JkJarPacker.java` (`makeFatJar`), `JkJarWriter`.
- **Workaround:** use the bootable jar of the springboot plugin.
- **Evidence:** sample-maintainer agent, reproduced on working-examples `springboot-base` before its fix.

### Native: reachability metadata version picked without `index.json`
- **Symptom:** a native Spring/Hibernate app fails at startup, e.g. `Invalid logger interface org.hibernate.boot.archive.scan.internal.ScannerLogger (implementation not found)`.
- **Cause:** with no exact version match, `findDirForVersion` takes the highest directory, pre-releases included: Hibernate 7.4.15 gets `8.0.0.Beta1` instead of `7.3.0.Final`, the one `index.json` names (`"default-for": "7\\.[34]\\..*"`).
- **Where:** `core/src/main/java/dev/jeka/core/api/tooling/nativ/JkNativeCompilation.java`, `findDirForVersion`.
- **Workaround:** pin an older `@native.metadataRepoVersion` (`1.0.0` or `0.10.6`).
- **Evidence:** sample-maintainer agent, reproduced on demo-project-springboot-angular, demo-build-conventions-consumer and working-examples `springboot-kotlin-reactjs`.

### Native: metadata extraction folder not cleaned when the version changes
- **Symptom:** after changing `@native.metadataRepoVersion`, folders of the previous version are still used.
- **Cause:** the zip is extracted over the existing `jeka-output/aot-discovery-metadata-repo`.
- **Where:** `JkNativeCompilation.java` (`extractReachabilityMetadata`), `NativeKBean.java`.
- **Workaround:** clean the output (`-c`).
- **Evidence:** sample-maintainer agent, reproduced (`hibernate-core/8.0.0.Beta1` used with `0.10.6`, until a clean clone).

### `docker: buildNative`: metadata folders collide in the build context
- **Symptom:** in the native Docker image, kotlin-reflect gets kotlin-stdlib's metadata (`KotlinReflectionNotSupportedError` at startup); about 11 spring-boot modules share one `4.0.6` folder.
- **Cause:** each metadata folder is copied as `imported-files/<version>`. The `importedFiles` set meant to give unique names is never filled.
- **Where:** `core/src/main/java/dev/jeka/core/api/tooling/docker/JkDockerBuild.java` (`importedFiles`), `JkDockerNativeBuild.java`.
- **Workaround:** none; build the native image outside Docker.
- **Evidence:** sample-maintainer agent, reproduced on working-examples `springboot-kotlin-reactjs` (the Dockerfile copies `imported-files/1.7.10` to both folders); from source: no `importedFiles.add` call.

## Publication and Maven

## Versioning (Git)

## Engine and command line

## KBeans

## Tests and CI

- **Outdated samples:** `samples/samples.baseapp` and `samples/samples.project-api` set `jeka.java.version=17`, below the Java 21 JeKa needs to run. `samples/samples.project-flat-facade-api` has no command running its test (`FlatFacadeBuild.cleanPack` runs 0 test), and outdated dependencies. `samples/samples.springboot/README.md` uses `jeka native: make`, which doesn't exist (`native: compile`). `samples/samples.basic/jeka.properties` holds `hh=h` and `to.to=dfd`, which look like leftovers. *sample-maintainer agent.*
- **springboot plugin:** `JkSpringModules` has no constants for the Spring Boot 4 starters (`spring-boot-starter-webmvc`, `spring-boot-starter-webmvc-test`). *sample-maintainer agent.*


## Not bugs, but to plan
- Material for MkDocs reaches end of life on 2027-05-05.
- Known limitation: core tests fail with `test.fork=false` (`IllegalAccessError` on package-private classes). In-process, `JkTestProcessor.launch` loads tests in a child of the JeKa classloader, so core main classes come from the running JeKa: two runtime packages. Only projects on the JeKa classpath, as core, are affected; `test.fork=true` (the default) avoids it. A fix would need child-first loading, which loads JeKa twice.
