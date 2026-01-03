# Docker KBean

<!-- header-autogen-doc -->


[`DockerKBean`](https://github.com/jeka-dev/jeka/blob/master/core/src/main/java/dev/jeka/core/tool/builtins/tooling/docker/DockerKBean.java) allows the creation of Docker images for both `project` and `base` KBeans. It supports generating JVM-based images as well as minimalist Docker images containing only the native executable.

**Key Features:**

- Efficiently create layered and secure Docker images for JVM applications.
- Generate secure, optimized Docker images for native applications.
- Infer image name/version from the project.
- Optionally switch to a non-root user (configurable).
- Customize the generated image via Java API.

**Example Invocation:**

- `jeka docker: buildNative`: Builds a native Docker image of your application.

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

```java
@JkPostInit
private void postInit(DockerKBean dockerKBean) {
    dockerKBean.customizeJvmImage(dockerBuild -> dockerBuild
            .addAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:2.16.0", "")
            .setBaseImage("eclipse-temurin:21.0.1_12-jre-jammy")
            .nonRootSteps()   // inserted after  USER nonroot
            .addCopy(Paths.get("jeka-output/release-note.md"), "/release.md")
            .add("RUN chmod a+rw /release.md ")
    );
}
```
This KBean allows customizing the Docker image programmatically using the [JeKa libs for Docker](api-docker.md).

It’s easy to see the customization result by executing `jeka docker: info`. 
This will display details about the built image, including the generated Dockerfile. 
You can also visit the generated Docker build directory, 
which contains all the Docker context needed to build the image with a Docker client.

## Summary

<!-- body-autogen-doc -->