# Docker API

The **Docker API** provides classes for defining and building Docker images programmatically.  
It relies on the Docker CLI, so a Docker installation (e.g., Docker Desktop) is required to invoke Docker commands from Java code.

## Features

- Define Dockerfiles and build-context directories programmatically.
- Generate optimized Docker images (JVM-based and native) directly from a project definition (e.g., a `JKProject` instance).

## Classes

The classes are located in package `dev.jeka.core.api.tooling.docker`.

### `JkDocker` class

Provides methods to:

- Execute Docker commands conveniently.
- Get a list of image names presnts in Docker registry

```java
JkDocker.execCmdLine("run", "--rm -p8080:8080 io.my-registry/my-image:latest");
```

### `JkDockerBuild` class

Provides methods to:

- Define Dockerfiles and build-context directories.
- Build images from defined build-context.

//TO BE CONTINUED...

