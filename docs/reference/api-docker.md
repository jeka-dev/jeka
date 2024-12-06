# Docker API

The **Docker API** provides classes for defining and building Docker images programmatically.  
It relies on the Docker CLI, so a Docker installation (e.g., Docker Desktop) is required to invoke Docker commands from Java code.

## Features

- Define Dockerfiles and build-context directories programmatically.
- Generate optimized Docker images (JVM-based and native) directly from a project definition (e.g., a `JKProject` instance).

## Classes

The classes are located in package `dev.jeka.core.api.tooling.docker`.

### `JkDocker`

Provides helper methods to:

- Execute Docker commands conveniently.
- Retrieve a list of image names present in the Docker registry.

```java
JkDocker.execCmdLine("run", "--rm -p8080:8080 io.my-registry/my-image:latest");
```

### `JkDockerBuild`

Represents a Docker build context.  
It enables defining the build context directory and Dockerfile, as well as invoking Docker to build an image.

Key features include:

- Define Dockerfiles and build-context directory contents programmatically.
- Build images based on the defined build context.
- Simplify Dockerfile editing:
  - Add a non-root user.
  - Copy files into the build context.
  - Adjust cursor position to insert build steps at specific locations.
  - And more...


```java title="Example"
JkDockerBuild dockerBuild = JkDockerBuild.of()
        .setBaseImage("eclipse-temurin:21-jdk-alpine")
        .setExposedPorts(8080);

dockerBuild.dockerfileTemplate
        .addCopy(Paths.get("/users/me/jars/my-app.jar"), "/app/my-app.jar")
        .add("WORKDIR /app")
        .addEntrypoint("java", "-jar", "/app/my-app.jar");

dockerBuild.buildImageInTemp("my-image:latest");  // Create the Docker image in a random temp dir
```

```dockerfile title="Dockerfile result"
FROM eclipse-temurin:21-jdk-alpine
RUN addgroup --gid 1002 nonrootgroup && \
    adduser --uid 1001 -g 1002 --disabled-password nonroot
USER nonroot
COPY imported-files/hello-jeka.jar /app/my-app.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "/app/my-app.jar"]
EXPOSE 8080
```

**Key Notes:**

- The non-root user has been automatically added as part of the Dockerfile.
- Jar files from the filesystem have been imported into the *[build context]/imported-files* directory.

For more details, refer to the [Javadoc](https://github.com/jeka-dev/jeka/blob/master/dev.jeka.core/src/main/java/dev/jeka/core/api/tooling/docker/JkDockerBuild.java).

### `JkDockerJvmBuild`

An extension of `JkDockerBuild` that provides additional methods to:

- Generate optimized JVM-based Docker images directly from a project definition.
- Add agents to the program for runtime enhancements.


```java title="Example"
JkProject project = project();
JkDockerJvmBuild dockerJvmBuild = JkDockerJvmBuild.of(project.asBuildable())
    .addAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:2.10.0", "myAgentOption");

dockerBuild.buildImageInTemp("my-jvm-image:latest");  // Create the Docker image in a random temp dir
```

```dockerfile title="Dockerfile result"
FROM eclipse-temurin:23-jdk-alpine
RUN addgroup --gid 1002 nonrootgroup && \
    adduser --uid 1001 -g 1002 --disabled-password nonroot
RUN mkdir -p /app && chown -R nonroot:nonrootgroup /app \
    && mkdir -p /workdir && chown -R nonroot:nonrootgroup /workdir
USER nonroot
COPY agents /app/agents
COPY libs /app/libs
COPY snapshot-libs /app/snapshot-libs
COPY classpath.txt /app/classpath.txt
COPY resources /app/classes
COPY classes /app/classes
WORKDIR /workdir
ENTRYPOINT [ "java", "-javaagent:/app/agents/opentelemetry-javaagent-2.10.0.jar=myAgentOption", "-cp", "@/app/classpath.txt", "dev.jeka.core.tool.Main" ]
CMD []
```
 **Key Notes:**

- The Docker image is layered for optimal caching. If a class changes without modifying dependencies, only the final step is re-executed, significantly speeding up the image creation process.
- The OpenTelemetry agent has been included and referenced in the command-line arguments.


### `JkDockerNativeBuild`

An extension of `JkDockerBuild` offering additional methods for creating compact and efficient native executable images.

**Example:* Creating a Minimalist Distroless Image

This approach produces images with a minimal package set to reduce the attack surface.
It requires building a native executable with static linking on `libc` (e.g., using MUSL).

```java title="Example"
JkNativeCompilation nativeCompilation = getNativeCompilation(); 
nativeCompilation.setStaticLinkage(MUSL);
JkDockerNativeBuild dockerBuild = JkDockerNativeBuild.of(nativeCompilation);
dockerBuild.setBaseImage("gcr.io/distroless/static-debian12:nonroot");

dockerBuild.buildImageInTemp("my-jvm-image:latest");  // Create the Docker image in a random temp dir
```

```dockerfile title="Dockerfile result"
FROM ghcr.io/graalvm/native-image-community:23-muslib AS build
COPY imported-files/j2objc-annotations-3.0.0.jar /root/cp/j2objc-annotations-3.0.0.jar
COPY imported-files/error_prone_annotations-2.28.0.jar /root/cp/error_prone_annotations-2.28.0.jar
COPY imported-files/checker-qual-3.43.0.jar /root/cp/checker-qual-3.43.0.jar
COPY imported-files/jsr305-3.0.2.jar /root/cp/jsr305-3.0.2.jar
COPY imported-files/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar /root/cp/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar
COPY imported-files/failureaccess-1.0.2.jar /root/cp/failureaccess-1.0.2.jar
COPY imported-files/guava-33.3.1-jre.jar /root/cp/guava-33.3.1-jre.jar
COPY imported-files/hello-jeka.jar /root/cp/hello-jeka.jar
COPY imported-files/jeka-native-image-arg-file-5771078964740361323.txt /argfile
RUN native-image @/argfile

FROM gcr.io/distroless/static-debian12:nonroot

COPY  --from=build /my-app /app/myapp

WORKDIR /app
ENTRYPOINT ["/app/myapp"]
```

**Key Notes:**

- The native executable is generated within a container using a multi-stage build.
- No non-root user has been created because the base image is inferred to already include one (based on its name).
- Native-based Docker images can be created directly from Windows or macOS workstations without any prerequisites other than Docker.
