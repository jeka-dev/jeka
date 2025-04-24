#

## JeKa commands

Create jar:
```shell
jeka project: pack
```

Clean and create jar:
```shell
jeka project: pack --clean
```

Run test and create jar:
```shell
jeka project: test pack 
```

Run jar
```shell
jeka project: runJar run.programArgs="" run.jvmOptions=""
```

Synchronize IntelliJ
```shell
jeka intellij: sync
```

### Docker

Create image:
```shell
jeka docker: build
```

Show info about image:
```shell
jeka docker: info
```

Create native image:
```shell
jeka docker: buildNative
```

Show info about native image:
```shell
jeka docker: infoNative
```

Create minimalist Docker image:
```shell
jeka docker: buildNative nativeBaseImage=gcr.io/distroless/static-debian12:nonroot native: staticLink=MUSL
```

