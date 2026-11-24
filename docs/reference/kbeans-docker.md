# Docker KBean

<!-- header-autogen-doc -->


[`DockerKBean`](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/tooling/docker/DockerKBean.java) allows the creation of Docker images for both [`project`](kbeans-project.md) and [`base` KBeans](kbeans-base.md). It supports generating JVM-based images as well as minimalist Docker images containing only the native executable.

**Key Features:**

- Efficiently create layered and secure Docker images for JVM applications.
- Generate secure, optimized Docker images for native applications.
- Infer image name/version from the project.
- Optionally switch to a non-root user (configurable).
- Customize the generated image via Java API.

**Prerequisites**

- A running Docker daemon for `build` and `buildNative`.
- An application with a main class. It is detected, or set with `@project.pack.mainClass`.
- No need to run `pack` before: classes are compiled if needed.

**Example Invocation:**

```shell
jeka docker: build         # builds a JVM image
jeka docker: buildNative   # builds an image containing the native executable only
jeka docker: info          # displays the image name and the Dockerfile, without building the image
```
When the image is built, the console displays the command to run it, as `docker run --rm com.acme-shop:1.0.0`.

**Image Name**

By default, the name is `<group>-<artifact>:<version>` (e.g. `com.acme-shop:1.0.0`), or the directory name if the project has no module ID. 
Native image names are prefixed with `native-`. 
The tag is `latest` when the version is a SNAPSHOT or not specified.
To use another name:
```properties title="jeka.properties"
@docker.jvmImageName=my-app:$version
@docker.nativeImageName=my-app-native:$version
```

**Example Configuration:**

Specify base image:
```properties
@docker.nativeBaseImage=gcr.io/distroless/static-debian12:nonroot
```

Add JVM agents:
```properties
@docker.jvmAgents.0.coordinate=io.opentelemetry.javaagent:opentelemetry-javaagent:2.16.0
@docker.jvmAgents.0.optionLine=-Dotel.traces.exporter=otlp,-Dotel.metrics.exporter=otlp
```

**Example for programmatic customization:**

```java title="jeka-src/Custom.java"
import dev.jeka.core.tool.JkPostInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.builtins.tooling.docker.DockerKBean;

import java.nio.file.Paths;

class Custom extends KBean {

    @JkPostInit
    private void postInit(DockerKBean dockerKBean) {
        dockerKBean.customizeJvmImage(dockerBuild -> {
            dockerBuild
                    .addAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:2.16.0", "")
                    .setBaseImage("eclipse-temurin:21.0.1_12-jre-jammy");
            dockerBuild.dockerfileTemplate
                    .moveCursorBeforeUserNonRoot()   // following steps are inserted before USER nonroot
                    .addCopy(Paths.get("jeka-output/release-note.md"), "/release.md")
                    .add("RUN chmod a+rw /release.md");
        });
    }
}
```
This KBean allows customizing the Docker image programmatically using the [JeKa libs for Docker](api-docker.md).

It’s easy to see the customization result by executing `jeka docker: info`. 
This will display details about the built image, including the generated Dockerfile. 
You can also visit the generated Docker build directory, 
which contains all the Docker context needed to build the image with a Docker client.

## Summary

<!-- body-autogen-doc -->