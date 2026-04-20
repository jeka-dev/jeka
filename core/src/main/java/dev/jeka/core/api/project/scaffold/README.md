#

## JeKa commands

Create jar:
```shell
jeka project: pack
```

Clean and create a jar:
```shell
jeka project: pack --clean
```

Run the JAR (build it first if it doesn’t exist). Pass JVM properties as program arguments using `-Dxxx=xx`.
```shell
jeka -p <PROGRAM ARGS...>
```

Clean the output dir, create a jar and run it.
```shell
jeka -c -p <PROGRAM ARGS...>
```

Full build, including static analysis and end-to-end tests if present.
```shell
jeka project: build
```

Synchronize IntelliJ:
```shell
jeka intellij: sync
```

### Docker

Create image:
```shell
jeka docker: build
```

Show info about the Docker image:
```shell
jeka docker: info
```

Create a native image:
```shell
jeka docker: buildNative
```

Show info about native image:
```shell
jeka docker: infoNative
```

Create a minimalist Docker image:
```shell
jeka docker: buildNative nativeBaseImage=gcr.io/distroless/static-debian12:nonroot native: staticLink=MUSL
```

