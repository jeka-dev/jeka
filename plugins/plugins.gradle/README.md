# Gradle Plugin for JeKa

A plugin to execute Gradle projects from JeKa, in the same way the built-in *MavenKBean* wraps Maven builds.

It provides a [KBean](src/dev/jeka/plugins/gradle/GradleKBean.java) to run Gradle commands from JeKa,
with the current JVM and environment forwarded automatically. The Gradle wrapper (`gradlew`/`gradlew.bat`)
is used when present in the project directory, otherwise falls back to the system `gradle`.

It also exposes [`JkGradle`](src/dev/jeka/plugins/gradle/JkGradle.java) and
[`JkGradleProject`](src/dev/jeka/plugins/gradle/JkGradleProject.java) for programmatic usage.

## Run a Gradle Application from Source

Add a `jeka.properties` file at the root of your Gradle project:

```properties
jeka.java.version=21

jeka.classpath=dev.jeka:gradle-plugin
jeka.program.build=gradle: wrapBuild
```

For multi-module projects, specify which sub-module contains the application:

```properties
jeka.java.version=21

jeka.classpath=dev.jeka:gradle-plugin
jeka.program.build=gradle: wrapBuild appModule=app
```

Then run:

```bash
jeka -p
```

JeKa invokes `wrapBuild`, which runs `gradle clean build -x test`, copies the produced JAR from
`build/libs/` (or `<appModule>/build/libs/`) to `jeka-output/`, and runs it.

> **Requirement:** The Gradle build must produce an executable fat JAR (with `Main-Class` set in the manifest).
> See [sample/app/build.gradle](sample/app/build.gradle) for an example.

For native builds (requires GraalVM and the `org.graalvm.buildtools.native` plugin):

```properties
jeka.program.build.native=gradle: wrapNative appModule=app -Djeka.java.distrib=graalvm -Djeka.java.version=21
```

See [sample/](sample/) for a complete working example.

## KBean Fields

| Field       | Type   | Default | Description                                                                              |
|-------------|--------|---------|------------------------------------------------------------------------------------------|
| `args`      | String | `""`    | Extra arguments forwarded to the `gradle` command. Example: `--info -Pprofile=prod`      |
| `appModule` | String | (root)  | Sub-module containing the application in multi-module projects.                           |

## Tasks

| Task          | Description                                                                                                                                   |
|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `wrap`        | Runs Gradle using the arguments defined in `args`.                                                                                            |
| `wrapBuild`   | Runs `clean build -x test` and copies produced artifacts from `build/libs/` to `jeka-output/`.                                               |
| `wrapNative`  | Runs `nativeCompile -x test` and copies the native executable from `build/native/nativeCompile/` to `jeka-output/`. Requires GraalVM and `org.graalvm.buildtools.native`. |

## Programmatic Usage

Use `JkGradle` or `JkGradleProject` directly for lower-level control:

```java
// Run an arbitrary Gradle command
JkGradle.of(myProjectDir)
        .addParams("clean", "build", "--info")
        .exec();

// Use the project wrapper
JkGradleProject project = JkGradleProject.of(myProjectDir);
project.cleanBuild().exec();
project.publishToMavenLocal().exec();
```

### From a KBean

```java
@JkDep("dev.jeka:gradle-plugin")
class Build extends KBean {

    @JkInject
    GradleKBean gradle;

    public void buildAndPackage() {
        gradle.wrapBuild();
    }
}
```

Source code: [GradleKBean](src/dev/jeka/plugins/gradle/GradleKBean.java)
