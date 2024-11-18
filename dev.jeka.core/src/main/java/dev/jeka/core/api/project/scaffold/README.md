#

## JeKa commands

Create jar
```shell
jeka project: pack
```

Clean and Create jar
```shell
jeka project: pack --clean
```

Create jar skipping tests
```shell
jeka project: pack "-Djeka.skip.tests=true"
```

Run jar
```shell
jeka project: runJar run.programArgs="" run.jvmOptions=""
```

Synchronize IntelliJ
```shell
jeka intellij: iml
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

Show info about native image
```shell
jeka docker: infoNative
```

